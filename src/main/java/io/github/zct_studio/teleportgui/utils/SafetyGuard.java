package io.github.zct_studio.teleportgui.utils;

import io.github.zct_studio.teleportgui.Styles;
import io.github.zct_studio.teleportgui.utils.config.CommandLoader;
import io.github.zct_studio.teleportgui.gui.Form;
import io.github.zct_studio.teleportgui.gui.GUI;
import io.github.zct_studio.teleportgui.Tr_i18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SafetyGuard {

    private SafetyGuard() {}

    private static final Map<String, Pending> PENDING = new ConcurrentHashMap<>();

    public record Pending(ResourceKey<Level> dim, Vec3 pos, String module, String node) {}

    public static boolean tryTeleport(ServerPlayer player, ServerLevel world, BlockPos target,
                                      String module, String node) {
        if (!CommandLoader.isSafetyCheckEnabled(module, node)) {
            return doTeleport(player, world, center(target), module, node);
        }
        var safe = TeleportUtils.findSafeBlock(target, world);
        if (safe.isPresent()) {
            return doTeleport(player, world, center(safe.get()), module, node);
        }
        PENDING.put(player.getStringUUID(), new Pending(world.dimension(), center(target), module, node));
        prompt(player);
        return false;
    }

    public static void forcePending(ServerPlayer player) {
        Pending p = PENDING.remove(player.getStringUUID());
        if (p == null) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.common.noPending", player)
                            .withStyle(Styles.ERROR),
                    true);
            return;
        }
        var server = TeleportUtils.getServer();
        ServerLevel world = server == null ? null : server.getLevel(p.dim());
        if (world == null) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.common.dimUnavailable", player)
                            .withStyle(Styles.ERROR),
                    true);
            return;
        }
        doTeleport(player, world, p.pos(), p.module(), p.node());
        player.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.common.forceTeleportDone", player)
                        .withStyle(Styles.SUCCESS),
                true);
    }

    private static boolean doTeleport(ServerPlayer player, ServerLevel world, Vec3 pos,
                                      String module, String node) {
        TeleportUtils.teleport(player, world, pos);
        CommandGuard.recordSuccess(player, module, node);
        return true;
    }

    private static Vec3 center(BlockPos p) {
        return new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
    }

    private static void prompt(ServerPlayer player) {
        if (TeleportUtils.isBedrockPlayer(player)) {
            if (TeleportUtils.hasFloodgate()) {
                Form.openSafetyConfirm(
                        player, () -> forcePending(player));
            } else {
                GUI.openSafetyConfirm(
                        player, () -> forcePending(player));
            }
            return;
        }
        player.sendSystemMessage(
                Component.empty()
                        .append(Tr_i18n.tr("commands.teleportgui.common.noSafeLocation", player)
                                .withStyle(Styles.ERROR_STRONG))
                        .append("\n")
                        .append(Tr_i18n.tr("commands.teleportgui.common.safetyIsForLosers", player)
                                .withStyle(Styles.BODY))
                        .append("\n")
                        .append(Tr_i18n.tr("commands.teleportgui.common.forceTeleport", player)
                                .withStyle(Styles.ACTION_FORCE)
                                .withStyle(s -> s.withClickEvent(
                                        new ClickEvent.RunCommand("/tpforce")))),
                false);
    }
}
