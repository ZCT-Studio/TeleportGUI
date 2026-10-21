package io.github.zct_studio.teleportgui.utils;

import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.Styles;
import io.github.zct_studio.teleportgui.Tr_i18n;
import io.github.zct_studio.teleportgui.utils.config.CommandLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.concurrent.ConcurrentHashMap;

public final class CommandGuard {
    private static final ConcurrentHashMap<String, Long> LAST_SUCCESS = new ConcurrentHashMap<>();

    private CommandGuard() {
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean checkPermission(ServerPlayer player, String module, String node) {
        int requiredLevel = CommandLoader.getPermissionLevel(module, node);
        if (requiredLevel < 0) return true;
        PermissionLevel pLevel = intToPermissionLevel(requiredLevel);
        if (player.permissions().hasPermission(new Permission.HasCommandLevel(pLevel))) return true;
        Constants.LOGGER.debug("{} failed permission check for {}.{} (required level {})",
                player.getName().getString(), module, node, requiredLevel);
        player.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.common.noPermission", player)
                        .withStyle(Styles.ERROR_STRONG),
                true);
        return false;
    }

    private static PermissionLevel intToPermissionLevel(int level) {
        PermissionLevel[] values = PermissionLevel.values();
        @SuppressWarnings("MathClampMigration") int clamped = Math.max(0, Math.min(level, values.length - 1));
        return values[clamped];
    }

    public static int checkCooldown(ServerPlayer player, String module, String node) {
        int cooldownSec = CommandLoader.getCooldown(module, node);
        if (cooldownSec <= 0) return 0;
        String key = makeKey(player, module, node);
        Long lastSuccess = LAST_SUCCESS.get(key);
        if (lastSuccess == null) return 0;
        long now = System.currentTimeMillis() / 1000;
        long elapsed = now - lastSuccess;
        if (elapsed >= cooldownSec) return 0;
        return (int) (cooldownSec - elapsed);
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
        String key = makeKey(player, module, node);
        LAST_SUCCESS.put(key, System.currentTimeMillis() / 1000);
    }

    private static String makeKey(ServerPlayer player, String module, String node) {
        return player.getStringUUID() + ":" + module + "." + node;
    }
}
