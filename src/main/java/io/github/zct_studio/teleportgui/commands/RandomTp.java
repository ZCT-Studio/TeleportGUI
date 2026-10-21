package io.github.zct_studio.teleportgui.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.ServerMain;
import io.github.zct_studio.teleportgui.Styles;
import io.github.zct_studio.teleportgui.Tr_i18n;
import io.github.zct_studio.teleportgui.utils.CommandGuard;
import io.github.zct_studio.teleportgui.utils.TeleportUtils;
import io.github.zct_studio.teleportgui.utils.config.CommandLoader;
import io.github.zct_studio.teleportgui.utils.config.ConfigKeys;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class RandomTp {

    private static final int LOADED_ATTEMPTS = 12;
    private static final int CHUNK_LOAD_ATTEMPTS = 4;
    private static final int NETHER_SCAN_DEPTH = 48;
    private static final int DEFAULT_MAX_DISTANCE = 1000;
    private static final long CHUNK_TIMEOUT_SECONDS = 10L;
    private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();

    private RandomTp() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> primary = dispatcher.register(
                Commands.literal("randomtp")
                        .executes(context -> execute(context.getSource())));
        dispatcher.register(Commands.literal("rtp").redirect(primary));
    }

    private static int execute(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_RANDOM)) {
            return 1;
        }
        if (!CommandGuard.checkAndWarnCooldown(player, ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_RANDOM)) {
            return 1;
        }
        if (!PENDING.add(player.getStringUUID())) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.randomtp.searching", player)
                            .withStyle(Styles.INFO),
                    true);
            return 0;
        }

        int minDistance = Math.max(0, CommandLoader.getMinDistance(
                ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_RANDOM));
        int maxDistance = CommandLoader.getMaxDistance(
                ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_RANDOM);
        if (maxDistance <= 0) {
            maxDistance = DEFAULT_MAX_DISTANCE;
        }
        if (maxDistance <= minDistance) {
            maxDistance = minDistance + 1;
        }

        ServerLevel world = player.level();
        player.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.randomtp.searching", player)
                        .withStyle(Styles.INFO),
                true);

        try {
            if (!tryLoadedChunks(player, world, minDistance, maxDistance)) {
                loadChunk(player, world, minDistance, maxDistance, 0);
            }
        } catch (RuntimeException e) {
            Constants.LOGGER.error("Random teleport search failed", e);
            fail(player);
        }
        return 0;
    }

    private static boolean tryLoadedChunks(ServerPlayer player, ServerLevel world,
                                           int minDistance, int maxDistance) {
        RandomSource random = player.getRandom();
        boolean ceiling = world.dimensionType().hasCeiling();

        for (int attempt = 0; attempt < LOADED_ATTEMPTS; attempt++) {
            int[] sample = sample(player, world, minDistance, maxDistance, random);
            if (sample == null) continue;

            int chunkX = sample[0] >> 4;
            int chunkZ = sample[1] >> 4;
            if (world.getChunkSource().getChunkNow(chunkX, chunkZ) == null) continue;

            Optional<BlockPos> target = findTarget(world, sample[0], sample[1], ceiling);
            if (target.isPresent()) {
                complete(player, world, target.get());
                return true;
            }
        }
        return false;
    }

    private static void loadChunk(ServerPlayer player, ServerLevel world, int minDistance,
                                  int maxDistance, int attempt) {
        if (attempt >= CHUNK_LOAD_ATTEMPTS) {
            fail(player);
            return;
        }

        int[] sample = sample(player, world, minDistance, maxDistance, player.getRandom());
        if (sample == null) {
            loadChunk(player, world, minDistance, maxDistance, attempt + 1);
            return;
        }

        int chunkX = sample[0] >> 4;
        int chunkZ = sample[1] >> 4;
        boolean ceiling = world.dimensionType().hasCeiling();

        if (world.getChunkSource().getChunkNow(chunkX, chunkZ) != null) {
            Optional<BlockPos> target = findTarget(world, sample[0], sample[1], ceiling);
            if (target.isPresent()) {
                complete(player, world, target.get());
                return;
            }
            loadChunk(player, world, minDistance, maxDistance, attempt + 1);
            return;
        }

        ServerChunkCache chunkSource = world.getChunkSource();
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);

        try {
            chunkSource.addTicketWithRadius(TicketType.PORTAL, chunkPos, 0);
            CompletableFuture<ChunkResult<ChunkAccess>> future =
                    chunkSource.getChunkFuture(chunkX, chunkZ, ChunkStatus.FULL, true);

            future.orTimeout(CHUNK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .whenComplete((result, throwable) -> {
                        MinecraftServer server = ServerMain.SERVER;
                        if (server == null) return;
                        server.execute(() -> {
                            chunkSource.removeTicketWithRadius(TicketType.PORTAL, chunkPos, 0);
                            if (player.isRemoved()) {
                                PENDING.remove(player.getStringUUID());
                                return;
                            }
                            if (throwable != null || result == null || !result.isSuccess()) {
                                loadChunk(player, world, minDistance, maxDistance, attempt + 1);
                                return;
                            }
                            Optional<BlockPos> target = findTarget(world, sample[0], sample[1], ceiling);
                            if (target.isPresent()) {
                                complete(player, world, target.get());
                            } else {
                                loadChunk(player, world, minDistance, maxDistance, attempt + 1);
                            }
                        });
                    });
        } catch (RuntimeException e) {
            chunkSource.removeTicketWithRadius(TicketType.PORTAL, chunkPos, 0);
            Constants.LOGGER.error("Failed to load random teleport chunk", e);
            loadChunk(player, world, minDistance, maxDistance, attempt + 1);
        }
    }

    private static int[] sample(ServerPlayer player, ServerLevel world, int minDistance,
                                int maxDistance, RandomSource random) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double min = minDistance;
            double max = maxDistance;
            double distance = Math.sqrt(min * min
                    + random.nextDouble() * (max * max - min * min));
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            if (world.getWorldBorder().isWithinBounds(x, z)) {
                return new int[]{x, z};
            }
        }
        return null;
    }

    private static Optional<BlockPos> findTarget(ServerLevel world, int x, int z, boolean ceiling) {
        if (ceiling) {
            int top = Math.min(world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z),
                    world.getMaxY());
            int bottom = Math.max(world.getMinY() + 2, top - NETHER_SCAN_DEPTH);
            for (int y = top - 1; y >= bottom; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (isSafeDestination(world, pos)) return Optional.of(pos);
            }
            return Optional.empty();
        }

        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = TeleportUtils.getStandablePos(world, new BlockPos(x, y, z));
        return isSafeDestination(world, pos) ? Optional.of(pos) : Optional.empty();
    }

    private static boolean isSafeDestination(ServerLevel world, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState ground = world.getBlockState(below);
        if (!ground.getFluidState().isEmpty()) return false;
        if (ground.getCollisionShape(world, below).isEmpty()) return false;

        BlockState feet = world.getBlockState(pos);
        if (!feet.getFluidState().isEmpty() || TeleportUtils.isUnsafeNonCollidable(feet)) return false;
        if (!feet.getCollisionShape(world, pos).isEmpty()) return false;

        BlockPos headPos = pos.above();
        BlockState head = world.getBlockState(headPos);
        if (!head.getFluidState().isEmpty() || TeleportUtils.isUnsafeNonCollidable(head)) return false;
        return head.getCollisionShape(world, headPos).isEmpty();
    }

    private static void complete(ServerPlayer player, ServerLevel world, BlockPos pos) {
        try {
            if (player.isRemoved()) return;
            TeleportUtils.teleport(player, world,
                    new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
            CommandGuard.recordSuccess(player, ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_RANDOM);
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.randomtp.go", player,
                                    Component.literal(String.valueOf(pos.getX())),
                                    Component.literal(String.valueOf(pos.getY())),
                                    Component.literal(String.valueOf(pos.getZ())))
                            .withStyle(Styles.SUCCESS),
                    true);
        } finally {
            PENDING.remove(player.getStringUUID());
        }
    }

    private static void fail(ServerPlayer player) {
        PENDING.remove(player.getStringUUID());
        if (player.isRemoved()) return;
        player.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.randomtp.failed", player)
                        .withStyle(Styles.ERROR),
                true);
    }
}
