package io.github.zct_studio.teleportgui.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.zct_studio.teleportgui.storage.DeathPoint;
import io.github.zct_studio.teleportgui.ServerMain;
import io.github.zct_studio.teleportgui.utils.CommandGuard;
import io.github.zct_studio.teleportgui.utils.SafetyGuard;
import io.github.zct_studio.teleportgui.utils.TeleportUtils;
import io.github.zct_studio.teleportgui.utils.config.ConfigKeys;
import io.github.zct_studio.teleportgui.Tr_i18n;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelData;

import java.util.Objects;
import java.util.Optional;

import static net.minecraft.world.level.Level.OVERWORLD;

public final class TpPoints {

    private TpPoints() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /back 回到上一次传送前位置
        dispatcher.register(Commands.literal("back")
                .executes(ctx -> cmdBack(ctx.getSource())));

        // /tpdeathpoint | /tpdp 死亡点
        dispatcher.register(Commands.literal("tpdeathpoint")
                .executes(ctx -> cmdDeathPoint(ctx.getSource())));
        dispatcher.register(Commands.literal("tpdp")
                .executes(ctx -> cmdDeathPoint(ctx.getSource())));

        // /tpworldspawn | /tpws 世界出生点
        dispatcher.register(Commands.literal("tpworldspawn")
                .executes(ctx -> cmdWorldSpawn(ctx.getSource())));
        dispatcher.register(Commands.literal("tpws")
                .executes(ctx -> cmdWorldSpawn(ctx.getSource())));

        // /tpspawnpoint | /tpsp 个人出生点
        dispatcher.register(Commands.literal("tpspawnpoint")
                .executes(ctx -> cmdTpSpawnPoint(ctx.getSource())));
        dispatcher.register(Commands.literal("tpsp")
                .executes(ctx -> cmdTpSpawnPoint(ctx.getSource())));

        // /tpforce 执行因安全检查而待定的强制传送
        dispatcher.register(Commands.literal("tpforce")
                .executes(ctx -> cmdForce(ctx.getSource())));
    }

    private static int cmdBack(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_BACK)) return 1;
        if (!CommandGuard.checkAndWarnCooldown(player, ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_BACK)) return 1;

        Optional<TeleportUtils.PreviousPos> prevOpt = TeleportUtils.getPreviousPos(player.getStringUUID());
        if (prevOpt.isEmpty()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.back.noLocation", player)
                            .withStyle(ChatFormatting.RED),
                    true);
            return 1;
        }

        TeleportUtils.PreviousPos prev = prevOpt.get();
        ServerLevel prevWorld = ServerMain.SERVER.getLevel(prev.dim());
        if (prevWorld == null) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.common.dimUnavailable", player)
                            .withStyle(ChatFormatting.RED),
                    true);
            return 1;
        }

        if (SafetyGuard.tryTeleport(player, prevWorld, BlockPos.containing(prev.pos()),
                ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_BACK)) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.back.go", player)
                            .withStyle(ChatFormatting.GREEN),
                    true);
            TeleportUtils.clearPreviousPos(player.getStringUUID());
        }
        return 0;
    }

    private static int cmdDeathPoint(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_DEATH_POINT)) return 1;
        if (!CommandGuard.checkAndWarnCooldown(player, ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_DEATH_POINT)) return 1;

        Optional<io.github.zct_studio.teleportgui.types.DeathPoint> opt = DeathPoint.get(player.getStringUUID());
        if (opt.isEmpty()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.common.noLocation", player)
                            .withStyle(ChatFormatting.RED),
                    true);
            return 1;
        }
        io.github.zct_studio.teleportgui.types.DeathPoint dp = opt.get();
        Optional<ServerLevel> optWorld = dp.getWorld();
        if (optWorld.isEmpty()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.common.dimUnavailable", player)
                            .withStyle(ChatFormatting.RED),
                    true);
            return 1;
        }

        if (SafetyGuard.tryTeleport(player, optWorld.get(), dp.getBlockPos(),
                ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_DEATH_POINT)) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.tpdeathpoint.go", player)
                            .withStyle(ChatFormatting.GREEN),
                    true);
        }
        return 0;
    }

    private static int cmdWorldSpawn(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_WORLD_SPAWN)) return 1;
        if (!CommandGuard.checkAndWarnCooldown(player, ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_WORLD_SPAWN)) return 1;

        ServerLevel world = Objects.requireNonNull(ServerMain.SERVER.getLevel(OVERWORLD), "Overworld null");
        BlockPos spawnPos = world.getLevelData().getRespawnData().pos();

        if (SafetyGuard.tryTeleport(player, world, spawnPos,
                ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_WORLD_SPAWN)) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.worldspawn.go", player)
                            .withStyle(ChatFormatting.GREEN),
                    true);
        }
        return 0;
    }

    private static int cmdTpSpawnPoint(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_SPAWN_POINT)) return 1;
        if (!CommandGuard.checkAndWarnCooldown(player, ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_SPAWN_POINT)) return 1;

        ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();
        LevelData.RespawnData respawnData = null;
        if (respawnConfig != null) {
            respawnData = respawnConfig.respawnData();
        }

        if (respawnData != null && !respawnData.equals(LevelData.RespawnData.DEFAULT)) {
            ServerLevel level = ServerMain.SERVER.getLevel(respawnData.dimension());
            if (level != null) {
                if (SafetyGuard.tryTeleport(player, level, respawnData.pos(),
                        ConfigKeys.MODULE_TELEPORT, ConfigKeys.TP_SPAWN_POINT)) {
                    player.sendSystemMessage(
                            Tr_i18n.tr("commands.teleportgui.tpspawnpoint.goPersonal", player)
                                    .withStyle(ChatFormatting.GREEN),
                            true);
                }
                return 0;
            }
        }
        return cmdWorldSpawn(source);
    }

    private static int cmdForce(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        SafetyGuard.forcePending(player);
        return 0;
    }
}
