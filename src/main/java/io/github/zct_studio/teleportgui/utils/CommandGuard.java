package io.github.zct_studio.teleportgui.utils;

import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.ServerMain;
import io.github.zct_studio.teleportgui.Styles;
import io.github.zct_studio.teleportgui.Tr_i18n;
import io.github.zct_studio.teleportgui.storage.RuntimeStore;
import io.github.zct_studio.teleportgui.utils.config.CommandLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CommandGuard {
    /**
     * 玩家 UUID → (命令 id → 冷却到期 tick)。
     * <p>
     * 只在服务端主线程访问（命令执行与 tick 回调），因此不需要并发容器。
     * <p>
     * 注意：本字段不持有锁，请勿在异步线程访问。
     */
    private static final Map<UUID, Map<NodeKey, Long>> COOLDOWNS = new HashMap<>();

    private CommandGuard() {
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean checkPermission(ServerPlayer player, String module, String node) {
        if (hasPermission(player, module, node)) return true;
        int requiredLevel = CommandLoader.getPermissionLevel(module, node);
        Constants.LOGGER.debug("{} failed permission check for {}.{} (required level {})",
                player.getName().getString(), module, node, requiredLevel);
        player.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.common.noPermission", player)
                        .withStyle(Styles.ERROR_STRONG),
                true);
        return false;
    }

    public static boolean hasPermission(ServerPlayer player, String module, String node) {
        int requiredLevel = CommandLoader.getPermissionLevel(module, node);
        if (requiredLevel < 0) return true;
        return player.permissions().hasPermission(
                new Permission.HasCommandLevel(intToPermissionLevel(requiredLevel)));
    }

    private static PermissionLevel intToPermissionLevel(int level) {
        PermissionLevel[] values = PermissionLevel.values();
        @SuppressWarnings("MathClampMigration") int clamped = Math.max(0, Math.min(level, values.length - 1));
        return values[clamped];
    }

    public static int checkCooldown(ServerPlayer player, String module, String node) {
        int cooldownSec = CommandLoader.getCooldown(module, node);
        if (cooldownSec <= 0) return 0;

        Map<NodeKey, Long> playerCooldowns = COOLDOWNS.get(player.getUUID());
        if (playerCooldowns == null) return 0;

        Long expiresAt = playerCooldowns.get(new NodeKey(module, node));
        if (expiresAt == null) return 0;

        long now = tick();
        long remaining = expiresAt - now;
        if (remaining <= 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, (remaining + 19L) / 20L);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean checkAndWarnCooldown(ServerPlayer player, String module, String node) {
        int remaining = checkCooldown(player, module, node);
        if (remaining <= 0) return true;
        player.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.common.cooldown", player,
                                Component.literal(String.valueOf(remaining)))
                        .withStyle(Styles.INFO),
                true);
        return false;
    }

    public static void recordSuccess(ServerPlayer player, String module, String node) {
        int cooldownSec = CommandLoader.getCooldown(module, node);
        if (cooldownSec <= 0) return;

        long expiresAt = tick() + cooldownSec * 20L;
        COOLDOWNS.computeIfAbsent(player.getUUID(), key -> new HashMap<>())
                .put(new NodeKey(module, node), expiresAt);
        RuntimeStore.markDirty();
    }

    public static void cleanup(long now) {
        boolean changed = false;
        for (Iterator<Map<NodeKey, Long>> it = COOLDOWNS.values().iterator(); it.hasNext(); ) {
            Map<NodeKey, Long> playerCooldowns = it.next();
            if (playerCooldowns.values().removeIf(expiresAt -> expiresAt <= now)) {
                changed = true;
            }
            if (playerCooldowns.isEmpty()) {
                it.remove();
                changed = true;
            }
        }
        if (changed) RuntimeStore.markDirty();
    }

    public static List<RuntimeStore.CooldownEntry> snapshot(long now) {
        List<RuntimeStore.CooldownEntry> result = new ArrayList<>();
        for (var playerEntry : COOLDOWNS.entrySet()) {
            String uuid = playerEntry.getKey().toString();
            for (var commandEntry : playerEntry.getValue().entrySet()) {
                long remaining = commandEntry.getValue() - now;
                if (remaining <= 0) continue;

                RuntimeStore.CooldownEntry entry = new RuntimeStore.CooldownEntry();
                entry.uuid = uuid;
                entry.module = commandEntry.getKey().module();
                entry.node = commandEntry.getKey().node();
                entry.remainingTicks = remaining;
                result.add(entry);
            }
        }
        return result;
    }

    public static void loadPersisted(List<RuntimeStore.CooldownEntry> entries, long now) {
        COOLDOWNS.clear();
        if (entries == null) return;

        for (RuntimeStore.CooldownEntry entry : entries) {
            if (entry == null || entry.uuid == null || entry.module == null || entry.node == null) continue;
            if (entry.remainingTicks <= 0) continue;

            try {
                UUID uuid = UUID.fromString(entry.uuid);
                COOLDOWNS.computeIfAbsent(uuid, key -> new HashMap<>())
                        .put(new NodeKey(entry.module, entry.node), now + entry.remainingTicks);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static long tick() {
        MinecraftServer server = ServerMain.SERVER;
        return server == null ? 0L : server.getTickCount();
    }

    /**
     * 一条命令的配置节点标识，例如 {@code tpa.request} 拆成 module=tpa、node=request。
     */
    private record NodeKey(String module, String node) {
    }
}
