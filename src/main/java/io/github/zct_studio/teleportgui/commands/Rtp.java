package io.github.zct_studio.teleportgui.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class Rtp {

    private static final int MAX_ATTEMPTS = 32;
    private static final int DEFAULT_MAX_DISTANCE = 1000;

    private Rtp() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rtp")
                .executes(context -> cmdRandomTeleport(context.getSource())));
    }

    private static int cmdRandomTeleport(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_RANDOM)) {
            return 1;
        }
        if (!CommandGuard.checkAndWarnCooldown(player, ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_RANDOM)) {
            return 1;
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
                Tr_i18n.tr("commands.teleportgui.rtp.searching", player)
                        .withStyle(Styles.INFO),
                true);

        Optional<BlockPos> target = findRandomTarget(player, world, minDistance, maxDistance);
        if (target.isEmpty()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.rtp.failed", player)
                            .withStyle(Styles.ERROR),
                    true);
            return 1;
        }

        BlockPos pos = target.get();
        TeleportUtils.teleport(player, world,
                new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
        CommandGuard.recordSuccess(player, ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_RANDOM);

        player.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.rtp.go", player,
                                Component.literal(String.valueOf(pos.getX())),
                                Component.literal(String.valueOf(pos.getY())),
                                Component.literal(String.valueOf(pos.getZ())))
                        .withStyle(Styles.SUCCESS),
                true);
        return 0;
    }

    private static Optional<BlockPos> findRandomTarget(ServerPlayer player, ServerLevel world,
                                                       int minDistance, int maxDistance) {
        RandomSource random = player.getRandom();
        boolean ceiling = world.dimensionType().hasCeiling();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double min = minDistance;
            double max = maxDistance;
            double distance = Math.sqrt(min * min
                    + random.nextDouble() * (max * max - min * min));
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            if (!world.getWorldBorder().isWithinBounds(x, z)) continue;

            Optional<BlockPos> target = ceiling
                    ? findBelowCeiling(world, x, z)
                    : findOnSurface(world, x, z);
            if (target.isPresent()) return target;
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> findOnSurface(ServerLevel world, int x, int z) {
        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = TeleportUtils.getStandablePos(world, new BlockPos(x, y, z));
        return isSafeDestination(world, pos) ? Optional.of(pos) : Optional.empty();
    }

    private static Optional<BlockPos> findBelowCeiling(ServerLevel world, int x, int z) {
        int top = Math.min(world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z),
                world.getMaxY());
        int bottom = world.getMinY() + 2;
        for (int y = top - 1; y >= bottom; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (isSafeDestination(world, pos)) return Optional.of(pos);
        }
        return Optional.empty();
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
}
