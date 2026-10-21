package io.github.zct_studio.teleportgui.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.zct_studio.teleportgui.Styles;
import io.github.zct_studio.teleportgui.Tr_i18n;
import io.github.zct_studio.teleportgui.storage.Manager;
import io.github.zct_studio.teleportgui.suggestionProviders.CommandGroup;
import io.github.zct_studio.teleportgui.suggestionProviders.ConfigKey;
import io.github.zct_studio.teleportgui.types.PlayerData;
import io.github.zct_studio.teleportgui.utils.config.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class GUI {

    private static final String LITERAL_NOTIFICATION_GUI = "notification_gui";
    private static final List<String> NODE_FIELDS = List.of(
            ConfigKeys.FIELD_COOLDOWN,
            ConfigKeys.FIELD_MIN_DISTANCE,
            ConfigKeys.FIELD_MAX_DISTANCE,
            ConfigKeys.FIELD_EXPIRE,
            ConfigKeys.FIELD_EXPIRE_REQUEST,
            ConfigKeys.FIELD_MAX_COUNT);
    private static final String FIELD_LEVEL = "level";
    private static final int MAX_PERMISSION_LEVEL = 4;
    private GUI() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("teleportgui")
                .then(Commands.literal(ConfigKeys.TG_BE)
                        .then(Commands.literal(LITERAL_NOTIFICATION_GUI)
                                .executes(context -> cmdGetNotificationGui(context.getSource()))
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> cmdNotificationGui(context.getSource(),
                                                BoolArgumentType.getBool(context, "value"))))))
                .then(Commands.literal(ConfigKeys.TG_CONFIG)
                        .then(Commands.argument("key", StringArgumentType.string())
                                .suggests(new ConfigKey())
                                .executes(context -> cmdGetConfig(context.getSource(),
                                        StringArgumentType.getString(context, "key")))
                                .then(Commands.argument("value", IntegerArgumentType.integer())
                                        .executes(context -> cmdConfig(context.getSource(),
                                                StringArgumentType.getString(context, "key"),
                                                IntegerArgumentType.getInteger(context, "value"))))))
                .then(Commands.literal(ConfigKeys.TG_COMMAND)
                        .then(Commands.argument("command", StringArgumentType.word())
                                .suggests(new CommandGroup())
                                .executes(context -> cmdGetCommand(context.getSource(),
                                        StringArgumentType.getString(context, "command")))
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> cmdSetCommand(context.getSource(),
                                                StringArgumentType.getString(context, "command"),
                                                BoolArgumentType.getBool(context, "value"))))))
                .then(Commands.literal(ConfigKeys.TG_RELOAD)
                        .then(Commands.literal(ConfigKeys.TG_COMMAND)
                                .executes(context -> cmdReloadCommand(context.getSource())))
                        .then(Commands.literal(ConfigKeys.TG_CONFIG)
                                .executes(context -> cmdReloadConfig(context.getSource())))));
    }

    private static int cmdNotificationGui(CommandSourceStack source, boolean enabled)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerData data = Manager.getPlayer(player.getStringUUID())
                .orElseGet(() -> Manager.addPlayer(player.getStringUUID()));
        data.setNotificationGuiEnabled(enabled);

        player.sendSystemMessage(
                Tr_i18n.tr(enabled
                                        ? "commands.teleportgui.teleportgui.notificationGui.on"
                                        : "commands.teleportgui.teleportgui.notificationGui.off",
                                player)
                        .withStyle(enabled ? Styles.SUCCESS : Styles.WARNING),
                false);
        return 0;
    }

    private static int cmdGetNotificationGui(CommandSourceStack source)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean enabled = Manager.getPlayer(player.getStringUUID())
                .map(PlayerData::isNotificationGuiEnabled)
                .orElse(true);

        player.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.teleportgui.notificationGui.get", player,
                                Component.literal(enabled ? "true" : "false"))
                        .withStyle(Styles.SUCCESS),
                false);
        return 0;
    }

    private static int cmdGetConfig(CommandSourceStack source, String key)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!requireOwner(player)) return 1;

        if (!configKeys().contains(key)) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.teleportgui.config.unknownKey", player,
                                    Component.literal(key))
                            .withStyle(Styles.ERROR),
                    false);
            return 1;
        }

        String current = currentConfigValue(key);
        if (current == null) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.teleportgui.config.unknownKey", player,
                                    Component.literal(key))
                            .withStyle(Styles.ERROR),
                    false);
            return 1;
        }

        player.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.teleportgui.config.get", player,
                                Component.literal(key), Component.literal(current))
                        .withStyle(Styles.SUCCESS),
                false);
        return 0;
    }

    private static int cmdConfig(CommandSourceStack source, String key, int value)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!requireOwner(player)) return 1;

        if (!configKeys().contains(key) || !applyConfig(key, value)) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.teleportgui.config.unknown", player,
                                    Component.literal(key))
                            .withStyle(Styles.ERROR),
                    false);
            return 1;
        }

        CommandLoader.save();
        player.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.teleportgui.config.set", player,
                                Component.literal(key), Component.literal(String.valueOf(value)))
                        .withStyle(Styles.SUCCESS),
                false);
        return 0;
    }

    private static int cmdGetCommand(CommandSourceStack source, String command)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!requireOwner(player)) return 1;

        String group = Registry.groupNameOf(command);
        if (group == null) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.teleportgui.command.unknownKey", player,
                                    Component.literal(command))
                            .withStyle(Styles.ERROR),
                    false);
            return 1;
        }

        boolean enabled = CommandLoader.isCommandEnabled(group);
        player.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.teleportgui.command.get", player,
                                Component.literal(group),
                                Component.literal(enabled ? "true" : "false"))
                        .withStyle(Styles.SUCCESS),
                false);
        return 0;
    }

    private static int cmdSetCommand(CommandSourceStack source, String command, boolean enabled)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!requireOwner(player)) return 1;

        String group = Registry.groupNameOf(command);
        if (group == null) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.teleportgui.command.unknown", player,
                                    Component.literal(command))
                            .withStyle(Styles.ERROR),
                    false);
            return 1;
        }

        CommandLoader.setCommandEnabled(group, enabled);
        Registry.rebuild(source.getServer().getCommands().getDispatcher());
        player.sendSystemMessage(
                Tr_i18n.tr(enabled
                                        ? "commands.teleportgui.teleportgui.command.enabled"
                                        : "commands.teleportgui.teleportgui.command.disabled",
                                player, Component.literal(group))
                        .withStyle(Styles.SUCCESS),
                false);
        return 0;
    }

    private static int cmdReloadCommand(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!requireOwner(player)) return 1;

        ConfigReloader.reloadCommandTree();
        player.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.teleportgui.reload.command", player)
                        .withStyle(Styles.SUCCESS),
                false);
        return 0;
    }

    private static int cmdReloadConfig(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!requireOwner(player)) return 1;

        if (!ConfigReloader.reloadConfig()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.teleportgui.reload.configFailed", player)
                            .withStyle(Styles.ERROR),
                    false);
            return 1;
        }
        player.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.teleportgui.reload.config", player)
                        .withStyle(Styles.SUCCESS),
                false);
        return 0;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean requireOwner(ServerPlayer player) {
        if (player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.OWNERS))) {
            return true;
        }
        player.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.common.noPermission", player)
                        .withStyle(Styles.ERROR_STRONG),
                false);
        return false;
    }

    public static List<String> configKeys() {
        List<String> keys = new ArrayList<>();
        collect(keys, ConfigKeys.MODULE_HOME, CommandLoader.CONFIG.home);
        collect(keys, ConfigKeys.MODULE_WARP, CommandLoader.CONFIG.warp);
        collect(keys, ConfigKeys.MODULE_TPA, CommandLoader.CONFIG.tpa);
        collect(keys, ConfigKeys.MODULE_TELEPORT, CommandLoader.CONFIG.teleport);
        return keys;
    }

    private static void collect(List<String> keys, String module, LinkedHashMap<String, CommandNode> map) {
        if (map == null) return;
        for (String node : map.keySet()) {
            for (String field : NODE_FIELDS) {
                keys.add(module + "." + node + "." + field);
            }
            keys.add(module + "." + node + "." + ConfigKeys.FIELD_PERMISSION + "." + FIELD_LEVEL);
        }
    }

    static boolean applyConfig(String key, int value) {
        String[] parts = key.split("\\.");
        if (parts.length == 3) {
            CommandNode node = nodeOf(parts[0], parts[1]);
            if (node == null) return false;
            switch (parts[2]) {
                case ConfigKeys.FIELD_COOLDOWN -> node.cooldownSeconds = value;
                case ConfigKeys.FIELD_MIN_DISTANCE -> node.minDistance = value;
                case ConfigKeys.FIELD_MAX_DISTANCE -> node.maxDistance = value;
                case ConfigKeys.FIELD_EXPIRE -> node.expireSeconds = value;
                case ConfigKeys.FIELD_EXPIRE_REQUEST -> node.requestExpireSeconds = value;
                case ConfigKeys.FIELD_MAX_COUNT -> node.maxCount = value;
                default -> {
                    return false;
                }
            }
            return true;
        }
        if (parts.length == 4
                && ConfigKeys.FIELD_PERMISSION.equals(parts[2])
                && FIELD_LEVEL.equals(parts[3])) {
            if (value < 0 || value > MAX_PERMISSION_LEVEL) return false;
            CommandNode node = nodeOf(parts[0], parts[1]);
            if (node == null) return false;
            if (node.permission == null) node.permission = new PermissionNode(value);
            else node.permission.level = value;
            return true;
        }
        return false;
    }

    static String currentConfigValue(String key) {
        String[] parts = key.split("\\.");
        if (parts.length == 3) {
            if (nodeOf(parts[0], parts[1]) == null) return null;
            Integer value = switch (parts[2]) {
                case ConfigKeys.FIELD_COOLDOWN ->
                        CommandLoader.getCooldown(parts[0], parts[1]);
                case ConfigKeys.FIELD_MIN_DISTANCE ->
                        CommandLoader.getMinDistance(parts[0], parts[1]);
                case ConfigKeys.FIELD_MAX_DISTANCE ->
                        CommandLoader.getMaxDistance(parts[0], parts[1]);
                case ConfigKeys.FIELD_EXPIRE ->
                        CommandLoader.getExpireSeconds(parts[0], parts[1]);
                case ConfigKeys.FIELD_EXPIRE_REQUEST ->
                        CommandLoader.getRequestExpireSeconds(parts[0], parts[1]);
                case ConfigKeys.FIELD_MAX_COUNT ->
                        CommandLoader.getMaxCount(parts[0], parts[1]);
                default -> null;
            };
            return value == null ? null : String.valueOf(value);
        }
        if (parts.length == 4
                && ConfigKeys.FIELD_PERMISSION.equals(parts[2])
                && FIELD_LEVEL.equals(parts[3])) {
            if (nodeOf(parts[0], parts[1]) == null) return null;
            return String.valueOf(CommandLoader.getPermissionLevel(parts[0], parts[1]));
        }
        return null;
    }

    private static CommandNode nodeOf(String module, String node) {
        LinkedHashMap<String, CommandNode> map = moduleMap(module);
        return map == null ? null : map.get(node);
    }

    static LinkedHashMap<String, CommandNode> moduleMap(String module) {
        return switch (module) {
            case ConfigKeys.MODULE_HOME -> CommandLoader.CONFIG.home;
            case ConfigKeys.MODULE_WARP -> CommandLoader.CONFIG.warp;
            case ConfigKeys.MODULE_TPA -> CommandLoader.CONFIG.tpa;
            case ConfigKeys.MODULE_TELEPORT -> CommandLoader.CONFIG.teleport;
            default -> null;
        };
    }
}
