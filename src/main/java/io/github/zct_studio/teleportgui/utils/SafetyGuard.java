package io.github.zct_studio.teleportgui.utils;

import io.github.zct_studio.teleportgui.Styles;
import io.github.zct_studio.teleportgui.Tr_i18n;
import io.github.zct_studio.teleportgui.gui.Form;
import io.github.zct_studio.teleportgui.gui.GUI;
import io.github.zct_studio.teleportgui.utils.config.CommandLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class SafetyGuard {

    /**
     * 等待玩家确认「强制传送」的目标位置。
     * 只在服务端主线程访问（命令执行与容器/表单回调），因此使用普通 HashMap。
     * 注意：本字段不持有锁，请勿在异步线程访问。
     */
    private static final Map<String, Pending> PENDING = new HashMap<>();

    private SafetyGuard() {
    }

    /**
     * 直接尝试传送到目标位置。
     *
     * @return {@code true} 表示已经完成传送；{@code false} 表示目标附近找不到安全落点，
     *         已把位置记入待确认队列并给玩家弹出确认提示，需要等玩家点「强制传送」后才真正传送。
     *         调用方若需要「确认后才做的事」（例如消耗一条 TPA 请求），请使用
     *         {@link #tryTeleport(ServerPlayer, ServerLevel, BlockPos, String, String, Runnable)}。
     */
    public static boolean tryTeleport(ServerPlayer player, ServerLevel world, BlockPos target,
                                      String module, String node) {
        return tryTeleport(player, world, target, module, node, null);
    }

    /**
     * 直接尝试传送到目标位置，并在「强制传送完成」时执行回调。
     *
     * @param onForced 玩家确认强制传送并成功传送后执行；可以直接传送时不执行
     * @return 同 {@link #tryTeleport(ServerPlayer, ServerLevel, BlockPos, String, String)}
     */
    public static boolean tryTeleport(ServerPlayer player, ServerLevel world, BlockPos target,
                                      String module, String node, Runnable onForced) {
        BlockPos destination = TeleportUtils.getStandablePos(world, target);
        if (!CommandLoader.isSafetyCheckEnabled(module, node)) {
            return doTeleport(player, world, center(destination), module, node);
        }
        var safe = TeleportUtils.findSafeBlock(destination, world);
        if (safe.isPresent()) {
            return doTeleport(player, world, center(safe.get()), module, node);
        }
        PENDING.put(player.getStringUUID(),
                new Pending(world.dimension(), center(destination), module, node, onForced));
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
        // 传送已经发生，再执行「确认后才该做」的收尾动作（例如消耗 TPA 请求）。
        if (p.onForced() != null) {
            p.onForced().run();
        }
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

    /**
     * 一条等待玩家确认的强制传送。
     *
     * @param dim      目标维度
     * @param pos      目标坐标
     * @param module   触发本次传送的功能模块，用于记录冷却
     * @param node     触发本次传送的节点，用于记录冷却
     * @param onForced 强制传送成功后要执行的回调，可为 {@code null}
     */
    public record Pending(ResourceKey<Level> dim, Vec3 pos, String module, String node,
                          Runnable onForced) {
    }
}
