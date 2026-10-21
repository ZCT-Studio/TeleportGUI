package io.github.zct_studio.teleportgui.commands;

import io.github.zct_studio.teleportgui.Styles;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.zct_studio.teleportgui.types.NamedLoc;
import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.gui.GUI;
import io.github.zct_studio.teleportgui.storage.Manager;
import io.github.zct_studio.teleportgui.utils.CommandGuard;
import io.github.zct_studio.teleportgui.utils.SafetyGuard;
import io.github.zct_studio.teleportgui.utils.TeleportUtils;
import io.github.zct_studio.teleportgui.utils.config.CommandLoader;
import io.github.zct_studio.teleportgui.utils.config.ConfigKeys;
import io.github.zct_studio.teleportgui.Tr_i18n;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.List;
import java.util.Optional;

import static net.minecraft.commands.Commands.argument;

public final class Warp {

    private Warp() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("warp")
                .executes(context -> cmdList(context.getSource()))

                .then(Commands.literal("create")
                        .then(argument("name", StringArgumentType.string())
                                .executes(context -> cmdCreate(context.getSource(),
                                        StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("add")
                        .then(argument("name", StringArgumentType.string())
                                .executes(context -> cmdCreate(context.getSource(),
                                        StringArgumentType.getString(context, "name")))))

                .then(Commands.literal("go")
                        .then(argument("name", StringArgumentType.string())
                                .suggests(new io.github.zct_studio.teleportgui.suggestionProviders.Warp())
                                .executes(context -> cmdGo(context.getSource(),
                                        StringArgumentType.getString(context, "name")))))

                .then(Commands.literal("del")
                        .then(argument("name", StringArgumentType.string())
                                .suggests(new io.github.zct_studio.teleportgui.suggestionProviders.Warp())
                                .executes(context -> cmdDel(context.getSource(),
                                        StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("delete")
                        .then(argument("name", StringArgumentType.string())
                                .suggests(new io.github.zct_studio.teleportgui.suggestionProviders.Warp())
                                .executes(context -> cmdDel(context.getSource(),
                                        StringArgumentType.getString(context, "name")))))

                .then(Commands.literal("rename")
                        .then(argument("old", StringArgumentType.string())
                                .suggests(new io.github.zct_studio.teleportgui.suggestionProviders.Warp())
                                .then(argument("new", StringArgumentType.string())
                                        .executes(context -> cmdRename(context.getSource(),
                                                StringArgumentType.getString(context, "old"),
                                                StringArgumentType.getString(context, "new"))))))

                .then(Commands.literal("list")
                        .executes(context -> cmdList(context.getSource())))

                .then(Commands.literal("gui")
                        .executes(context -> cmdGui(context.getSource())))
        );

        dispatcher.register(Commands.literal("gwarp")
                .executes(context -> cmdGui(context.getSource())));
    }

    private static int cmdCreate(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        try {
            if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_CREATE)) return 1;
            if (!CommandGuard.checkAndWarnCooldown(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_CREATE)) return 1;
            setWarp(player, name);
        } catch (Exception e) {
            Constants.LOGGER.error("Error while setting the warp!", e);
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.warp.setError", player)
                            .withStyle(Styles.ERROR_STRONG),
                    true);
            return 1;
        }
        return 0;
    }

    private static int cmdGo(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        try {
            if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_GO)) return 1;
            if (!CommandGuard.checkAndWarnCooldown(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_GO)) return 1;
            goToWarp(player, name);
        } catch (Exception e) {
            Constants.LOGGER.error("Error while going to the warp!", e);
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.warp.goError", player)
                            .withStyle(Styles.ERROR_STRONG),
                    true);
            return 1;
        }
        return 0;
    }

    private static int cmdDel(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        try {
            if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_DEL)) return 1;
            if (!CommandGuard.checkAndWarnCooldown(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_DEL)) return 1;
            if (!player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.OWNERS))) {
                player.sendSystemMessage(
                        Tr_i18n.tr("commands.teleportgui.common.noPermission", player)
                                .withStyle(Styles.ERROR_STRONG),
                        true);
                return 1;
            }
            deleteWarp(player, name);
        } catch (Exception e) {
            Constants.LOGGER.error("Error while deleting the warp!", e);
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.warp.deleteError", player)
                            .withStyle(Styles.ERROR_STRONG),
                    true);
            return 1;
        }
        return 0;
    }

    private static int cmdRename(CommandSourceStack source, String oldName, String newName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        try {
            if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_RENAME)) return 1;
            if (!CommandGuard.checkAndWarnCooldown(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_RENAME)) return 1;
            if (!player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.OWNERS))) {
                player.sendSystemMessage(
                        Tr_i18n.tr("commands.teleportgui.common.noPermission", player)
                                .withStyle(Styles.ERROR_STRONG),
                        true);
                return 1;
            }
            renameWarp(player, oldName, newName);
        } catch (Exception e) {
            Constants.LOGGER.error("Error while renaming the warp!", e);
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.warp.renameError", player)
                            .withStyle(Styles.ERROR_STRONG),
                    true);
            return 1;
        }
        return 0;
    }

    private static int cmdList(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        try {
            if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_LIST)) return 1;
            printWarps(player);
        } catch (Exception e) {
            Constants.LOGGER.error("Error while printing warps!", e);
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.warps.error", player)
                            .withStyle(Styles.ERROR_STRONG),
                    true);
            return 1;
        }
        return 0;
    }

    private static int cmdGui(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_GUI)) return 1;
        GUI.openWarp(player);
        return 0;
    }

    public static void setWarp(ServerPlayer player, String warpName) {
        warpName = warpName.trim().toLowerCase();
        if (!isValidName(player, warpName)) return;

        boolean alreadyExists = Manager.getWarp(warpName).isPresent();
        int maxCount = CommandLoader.getMaxCount(ConfigKeys.MODULE_WARP, ConfigKeys.WARP_CREATE);
        if (!alreadyExists && maxCount >= 0 && Manager.getWarps().size() >= maxCount) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.common.maxCountReached", player,
                                    Component.literal(String.valueOf(maxCount)))
                            .withStyle(Styles.ERROR),
                    true);
            return;
        }

        BlockPos blockPos = TeleportUtils.getTeleportPos(player);
        String worldString = player.level().dimension().identifier().toString();

        NamedLoc warp = new NamedLoc(warpName, blockPos, worldString);
        boolean exists = Manager.addWarp(warp);

        if (exists) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.warp.exists", player)
                            .withStyle(Styles.ERROR),
                    true);
        } else {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.warp.set", player),
                    true);
            CommandGuard.recordSuccess(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_CREATE);
        }
    }

    public static void goToWarp(ServerPlayer player, String warpName) {
        warpName = warpName.toLowerCase();

        Optional<NamedLoc> optionalWarp = Manager.getWarp(warpName);
        if (optionalWarp.isEmpty()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.warp.notFound", player)
                            .withStyle(Styles.ERROR),
                    true);
            return;
        }

        NamedLoc warp = optionalWarp.get();

        Optional<ServerLevel> optionalWorld = warp.getWorld();
        if (optionalWorld.isEmpty()) {
            Constants.LOGGER.warn(
                    "({}) Error while going to the warp \"{}\"! \nCouldn't find a world with the id: \"{}\" \nAvailable worlds: {}",
                    player.getName().getString(),
                    warp.getName(),
                    warp.getWorldId(),
                    TeleportUtils.getWorldIds());

            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.common.worldNotFound", player)
                            .withStyle(Styles.ERROR_STRONG),
                    true);
            return;
        }

        ServerLevel warpWorld = optionalWorld.get();
        BlockPos teleportBlockPos = warp.getBlockPos();

        if (TeleportUtils.getTeleportPos(player).equals(teleportBlockPos) && player.level() == warpWorld) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.warp.goSame", player)
                            .withStyle(Styles.INFO),
                    true);
            CommandGuard.recordSuccess(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_GO);
        } else if (SafetyGuard.tryTeleport(player, warpWorld, teleportBlockPos,
                ConfigKeys.MODULE_WARP, ConfigKeys.WARP_GO)) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.warp.go", player),
                    true);
        }
    }

    public static void deleteWarp(ServerPlayer player, String warpName) {
        warpName = warpName.toLowerCase();

        Optional<NamedLoc> optionalWarp = Manager.getWarp(warpName);

        if (optionalWarp.isPresent()) {
            Manager.removeWarp(optionalWarp.get());
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.warp.delete", player),
                    true);
            CommandGuard.recordSuccess(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_DEL);
        } else {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.warp.notFound", player)
                            .withStyle(Styles.ERROR),
                    true);
        }
    }

    public static void renameWarp(ServerPlayer player, String warpName, String newWarpName) {
        warpName = warpName.trim().toLowerCase();
        newWarpName = newWarpName.trim().toLowerCase();
        if (!isValidName(player, newWarpName)) return;

        if (Manager.getWarp(newWarpName).isPresent()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.common.nameExists", player)
                            .withStyle(Styles.ERROR),
                    true);
            return;
        }

        Optional<NamedLoc> warpToRename = Manager.getWarp(warpName);

        if (warpToRename.isPresent()) {
            warpToRename.get().setName(newWarpName);
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.warp.rename", player),
                    true);
            CommandGuard.recordSuccess(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_RENAME);
        } else {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.warp.notFound", player)
                            .withStyle(Styles.ERROR),
                    true);
        }
    }

    static void printWarps(ServerPlayer player) {
        List<NamedLoc> warps = Manager.getWarps();

        if (warps.isEmpty()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.warp.homeless", player)
                            .withStyle(Styles.INFO),
                    true);
            return;
        }

        MutableComponent message = Component.empty();

        message.append(
                Tr_i18n.tr("commands.teleportgui.warps.warps", player)
                        .withStyle(Styles.HEADER));

        for (NamedLoc currentWarp : warps) {

            String name = String.format("  - %s", currentWarp.getName());
            String coords = String.format("[X%d Y%d Z%d]", currentWarp.getX(), currentWarp.getY(), currentWarp.getZ());
            String dimension = String.format(" [%s]", currentWarp.getWorldId());

            boolean canModify = player.permissions().hasPermission(
                    new Permission.HasCommandLevel(PermissionLevel.OWNERS));

            message.append("\n");
            message.append(Component.literal(name).withStyle(Styles.INFO));

            message.append("\n");
            message.append(Component.literal("     | ").withStyle(Styles.INFO))
                    .append(Component.literal(coords)
                            .withStyle(Styles.ACTION)
                            .withStyle(style -> style.withClickEvent(
                                    new ClickEvent.CopyToClipboard(
                                            String.format("X%d Y%d Z%d",
                                                    currentWarp.getX(), currentWarp.getY(), currentWarp.getZ()))))
                            .withStyle(style -> style.withHoverEvent(
                                    new HoverEvent.ShowText(
                                            Tr_i18n.tr("commands.teleportgui.common.hoverCopy", player)))))
                    .append(Component.literal(dimension)
                            .withStyle(Styles.ACTION_SECONDARY)
                            .withStyle(style -> style.withClickEvent(
                                    new ClickEvent.CopyToClipboard(currentWarp.getWorldId())))
                            .withStyle(style -> style.withHoverEvent(
                                    new HoverEvent.ShowText(
                                            Tr_i18n.tr("commands.teleportgui.common.hoverCopy", player)))));

            message.append("\n");

            if (TeleportUtils.isBedrockPlayer(player)) {
                message.append(
                                Tr_i18n.tr("commands.teleportgui.common.be.prompt", player))
                        .append(Component.literal(" /gwarp"));
            } else {
                message.append(Component.literal("     | ").withStyle(Styles.INFO))
                        .append(Tr_i18n.tr("commands.teleportgui.common.tp", player)
                                .withStyle(Styles.SUCCESS)
                                .withStyle(style -> style.withClickEvent(
                                        new ClickEvent.RunCommand(
                                                String.format("/warp go %s", quote(currentWarp.getName())))))
                        )
                        .append(" ");

                if (canModify) {
                    message.append(
                            Tr_i18n.tr("commands.teleportgui.common.delete", player)
                                    .withStyle(Styles.ERROR)
                                    .withStyle(style -> style.withClickEvent(
                                            new ClickEvent.SuggestCommand(
                                                    String.format("/warp del %s", quote(currentWarp.getName()))))));
                }
            }

            message.append("\n");
        }

        player.sendSystemMessage(message, false);
    }

    private static boolean isValidName(ServerPlayer player, String name) {
        if (name.isEmpty() || name.indexOf('"') >= 0 || name.indexOf('\'') >= 0) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.common.invalidName", player)
                            .withStyle(Styles.ERROR),
                    true);
            return false;
        }
        return true;
    }

    /** 用双引号包裹名字，保证名字里有空格时点击聊天消息也能正确解析。 */
    private static String quote(String name) {
        return "\"" + name.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
