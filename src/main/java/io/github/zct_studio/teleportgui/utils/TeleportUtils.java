package io.github.zct_studio.teleportgui.utils;

import io.github.zct_studio.teleportgui.ServerMain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;
import java.util.stream.StreamSupport;

import static net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT;

@SuppressWarnings("unused")
public final class TeleportUtils {

    private static final Set<String> UNSAFE_NON_COLLIDABLE_BLOCKS = Set.of(
            "block.minecraft.lava", "block.minecraft.flowing_lava", "block.minecraft.end_portal",
            "block.minecraft.end_gateway", "block.minecraft.fire", "block.minecraft.soul_fire",
            "block.minecraft.powder_snow", "block.minecraft.nether_portal"
    );

    private static final Map<String, PreviousPos> PREV_POS = new HashMap<>();

    private TeleportUtils() {
    }

    public static void recordPreviousPos(ServerPlayer player) {
        PREV_POS.put(player.getStringUUID(),
                new PreviousPos(player.position(), player.level().dimension()));
    }

    public static Optional<PreviousPos> getPreviousPos(String uuid) {
        return Optional.ofNullable(PREV_POS.get(uuid));
    }

    public static void clearPreviousPos(String uuid) {
        PREV_POS.remove(uuid);
    }

    public static net.minecraft.server.MinecraftServer getServer() {
        return ServerMain.SERVER;
    }

    public static BlockPos getTeleportPos(Entity entity) {
        return new BlockPos(
                (int) Math.floor(entity.getX()),
                (int) Math.floor(entity.getY() + 0.5D),
                (int) Math.floor(entity.getZ()));
    }

    public static BlockPos getStandablePos(ServerLevel world, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < 8 && !hasRoomForFeet(world, cursor); i++) {
            cursor.move(Direction.UP);
        }
        return cursor.immutable();
    }

    private static boolean hasRoomForFeet(ServerLevel world, BlockPos feet) {
        if (!world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()) {
            return false;
        }
        BlockPos below = feet.below();
        VoxelShape belowShape = world.getBlockState(below).getCollisionShape(world, below);
        return belowShape.isEmpty() || belowShape.max(Direction.Axis.Y) <= 1.0D;
    }

    public static boolean isBedrockPlayer(ServerPlayer player) {
        try {
            Class.forName("org.geysermc.geyser.api.GeyserApi");
            Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Object api = apiClass.getMethod("api").invoke(null);
            if (api == null) return false;
            Object connection = apiClass.getMethod("connectionByUuid", java.util.UUID.class)
                    .invoke(api, player.getUUID());
            return connection != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasFloodgate() {
        try {
            Class.forName("org.geysermc.cumulus.form.Form");
        } catch (Throwable e) {
            return false;
        }
        return floodgateApi() != null;
    }

    public static Object floodgateApi() {
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            for (String name : new String[]{"getInstance", "api"}) {
                java.lang.reflect.Method m;
                try {
                    m = apiClass.getMethod(name);
                } catch (NoSuchMethodException ignored) {
                    continue;
                }
                if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
                m.setAccessible(true);
                Object api = m.invoke(null);
                if (api != null) return api;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static List<String> getWorldIds() {
        var server = getServer();
        if (server == null) return List.of();
        return StreamSupport.stream(server.getAllLevels().spliterator(), false)
                .map(level -> level.dimension().identifier().toString())
                .toList();
    }

    public static void teleport(ServerPlayer player, ServerLevel world, Vec3 coords) {
        recordPreviousPos(player);
        world.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1, player.getZ(), 20, 0.0D, 0.0D, 0.0D, 0.01);
        world.sendParticles(ParticleTypes.WHITE_SMOKE, player.getX(), player.getY(), player.getZ(), 15, 0.0D, 1.0D, 0.0D, 0.03);
        world.playSound(null, player.blockPosition(), SoundEvent.createVariableRangeEvent(ENDERMAN_TELEPORT.location()), SoundSource.PLAYERS, 0.4f, 1.0f);

        boolean flying = player.getAbilities().flying;
        player.teleportTo(world, coords.x, coords.y, coords.z, Set.of(), player.getYRot(), player.getXRot(), false);

        if (flying) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }

        world.playSound(null, player.blockPosition(), SoundEvent.createVariableRangeEvent(ENDERMAN_TELEPORT.location()), SoundSource.PLAYERS, 0.4f, 1.0f);
        world.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY(), player.getZ(), 20, 0.0D, 1.0D, 0.0D, 0.01);
        world.sendParticles(ParticleTypes.WHITE_SMOKE, player.getX(), player.getY(), player.getZ(), 15, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    public static Optional<BlockPos> findSafeBlock(BlockPos pos, ServerLevel world) {
        if (isSafe(pos, world)) return Optional.of(pos);
        int[] rowArr = {1, 2, 3};
        for (int row : rowArr) {
            for (int z = -row; z <= row; z++) {
                for (int x = -row; x <= row; x++) {
                    for (int y = -row; y <= row; y++) {
                        if ((x == -row || x == row) || (z == -row || z == row) || (y == -row || y == row)) {
                            BlockPos newPos = new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                            if (isSafe(newPos, world)) return Optional.of(newPos);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isSafe(BlockPos bottomPlayer, ServerLevel world) {
        BlockPos below = bottomPlayer.below();
        String belowId = world.getBlockState(below).getBlock().getDescriptionId();
        String bottomId = world.getBlockState(bottomPlayer).getBlock().getDescriptionId();
        BlockPos top = bottomPlayer.above();
        String topId = world.getBlockState(top).getBlock().getDescriptionId();
        boolean hasStandableGround = !"block.minecraft.water".equals(belowId)
                && !world.getBlockState(below).getCollisionShape(world, below).isEmpty();
        boolean bottomIsPassable = world.getBlockState(bottomPlayer).getCollisionShape(world, bottomPlayer).isEmpty()
                && !UNSAFE_NON_COLLIDABLE_BLOCKS.contains(bottomId);
        boolean topIsNotDanger = !UNSAFE_NON_COLLIDABLE_BLOCKS.contains(topId);
        return hasStandableGround && bottomIsPassable && topIsNotDanger;
    }

    public static boolean isUnsafeNonCollidable(BlockState state) {
        return UNSAFE_NON_COLLIDABLE_BLOCKS.contains(state.getBlock().getDescriptionId());
    }

    public record PreviousPos(Vec3 pos, ResourceKey<Level> dim) {
    }
}
