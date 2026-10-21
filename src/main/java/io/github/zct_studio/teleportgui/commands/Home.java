package io.github.zct_studio.teleportgui.commands;

import io.github.zct_studio.teleportgui.Styles;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.zct_studio.teleportgui.types.NamedLoc;
import io.github.zct_studio.teleportgui.types.PlayerData;
import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.gui.GUI;
import io.github.zct_studio.teleportgui.storage.Manager;
import io.github.zct_studio.teleportgui.utils.CommandGuard;
import io.github.zct_studio.teleportgui.utils.CommandUtils;
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

import java.util.List;
import java.util.Optional;

import static net.minecraft.commands.Commands.argument;

public final class Home {

    private Home() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("home")

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
                                .suggests(new io.github.zct_studio.teleportgui.suggestionProviders.Home())
                                .executes(context -> cmdGo(context.getSource(),
                                        StringArgumentType.getString(context, "name")))))

                .then(Commands.literal("del")
                        .then(argument("name", StringArgumentType.string())
                                .suggests(new io.github.zct_studio.teleportgui.suggestionProviders.Home())
                                .executes(context -> cmdDel(context.getSource(),
                                        StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("delete")
                        .then(argument("name", StringArgumentType.string())
                                .suggests(new io.github.zct_studio.teleportgui.suggestionProviders.Home())
                                .executes(context -> cmdDel(context.getSource(),
                                        StringArgumentType.getString(context, "name")))))

                .then(Commands.literal("rename")
                        .then(argument("old", StringArgumentType.string())
                                .suggests(new io.github.zct_studio.teleportgui.suggestionProviders.Home())
                                .then(argument("new", StringArgumentType.string())
                                        .executes(context -> cmdRename(context.getSource(),
                                                StringArgumentType.getString(context, "old"),
                                                StringArgumentType.getString(context, "new"))))))

                .then(Commands.literal("list")
                        .executes(context -> cmdList(context.getSource())))

                .then(Commands.literal("default")
                        .then(argument("name", StringArgumentType.string())
                                .suggests(new io.github.zct_studio.teleportgui.suggestionProviders.Home())
                                .executes(context -> cmdDefault(context.getSource(),
                                        StringArgumentType.getString(context, "name")))))

                .then(Commands.literal("gui")
                        .executes(context -> cmdGui(context.getSource())))

        );

        dispatcher.register(Commands.literal("ghome")
                .executes(context -> cmdGui(context.getSource())));
    }

    private static int cmdCreate(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        try {
            if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_HOME, ConfigKeys.HOME_CREATE)) return 1;
            if (!CommandGuard.checkAndWarnCooldown(player, ConfigKeys.MODULE_HOME, ConfigKeys.HOME_CREATE)) return 1;
            setHome(player, name);
        } catch (Exception e) {
            Constants.LOGGER.error("Error while setting a home!", e);
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.setError", player)
                            .withStyle(Styles.ERROR_STRONG),
                    true);
            return 1;
        }
        return 0;
    }

    private static int cmdGo(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        try {
            if (!CommandGuard.checkAndWarnCooldown(player, ConfigKeys.MODULE_HOME, ConfigKeys.HOME_GO)) return 1;
            goHome(player, name == null ? "" : name);
        } catch (Exception e) {
            Constants.LOGGER.error("Error while going to a home!", e);
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.goError", player)
                            .withStyle(Styles.ERROR_STRONG),
                    true);
            return 1;
        }
        return 0;
    }

    private static int cmdDel(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        try {
            if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_HOME, ConfigKeys.HOME_DEL)) return 1;
            if (!CommandGuard.checkAndWarnCooldown(player, ConfigKeys.MODULE_HOME, ConfigKeys.HOME_DEL)) return 1;
            deleteHome(player, name);
        } catch (Exception e) {
            Constants.LOGGER.error("Error while deleting a home!", e);
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.deleteError", player)
                            .withStyle(Styles.ERROR_STRONG),
                    true);
            return 1;
        }
        return 0;
    }

    private static int cmdRename(CommandSourceStack source, String oldName, String newName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        try {
            if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_HOME, ConfigKeys.HOME_RENAME)) return 1;
            if (!CommandGuard.checkAndWarnCooldown(player, ConfigKeys.MODULE_HOME, ConfigKeys.HOME_RENAME)) return 1;
            renameHome(player, oldName, newName);
        } catch (Exception e) {
            Constants.LOGGER.error("Error while renaming a home!", e);
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.renameError", player)
                            .withStyle(Styles.ERROR_STRONG),
                    true);
            return 1;
        }
        return 0;
    }

    private static int cmdDefault(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        try {
            if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_HOME, ConfigKeys.HOME_DEFAULT)) return 1;
            if (!CommandGuard.checkAndWarnCooldown(player, ConfigKeys.MODULE_HOME, ConfigKeys.HOME_DEFAULT)) return 1;
            setDefaultHome(player, name);
        } catch (Exception e) {
            Constants.LOGGER.error("Error while setting the default home!", e);
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.defaultError", player)
                            .withStyle(Styles.ERROR_STRONG),
                    true);
            return 1;
        }
        return 0;
    }

    private static int cmdList(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        try {
            if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_HOME, ConfigKeys.HOME_LIST)) return 1;
            printHomes(player);
        } catch (Exception e) {
            Constants.LOGGER.error("Error while printing homes!", e);
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.homes.error", player)
                            .withStyle(Styles.ERROR_STRONG),
                    true);
            return 1;
        }
        return 0;
    }

    private static int cmdGui(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!CommandGuard.checkPermission(player, ConfigKeys.MODULE_HOME, ConfigKeys.HOME_GUI)) return 1;
        GUI.openHome(player);
        return 0;
    }

    public static void setHome(ServerPlayer player, String homeName) {
        homeName = homeName.trim().toLowerCase();
        if (!CommandUtils.isValidName(player, homeName)) return;

        PlayerData playerStorage = Manager.addPlayer(player.getStringUUID());

        boolean alreadyExists = playerStorage.getHome(homeName).isPresent();
        int maxCount = CommandLoader.getMaxCount(ConfigKeys.MODULE_HOME, ConfigKeys.HOME_CREATE);
        if (!alreadyExists && maxCount >= 0 && playerStorage.getHomes().size() >= maxCount) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.common.maxCountReached", player,
                                    Component.literal(String.valueOf(maxCount)))
                            .withStyle(Styles.ERROR),
                    true);
            return;
        }

        BlockPos blockPos = TeleportUtils.getTeleportPos(player);
        String worldString = player.level().dimension().identifier().toString();

        NamedLoc home = new NamedLoc(homeName, blockPos, worldString);

        boolean homeExists = playerStorage.addHome(home);

        if (homeExists) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.exists", player)
                            .withStyle(Styles.ERROR),
                    true);
        } else {
            if (playerStorage.getHomes().size() == 1) {
                playerStorage.setDefaultHome(homeName);
            }

            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.set", player),
                    true);
            CommandGuard.recordSuccess(player, ConfigKeys.MODULE_HOME, ConfigKeys.HOME_CREATE);
        }
    }

    public static void goHome(ServerPlayer player, String homeName) {
        homeName = homeName.toLowerCase();

        Optional<PlayerData> optStorage = Manager.getPlayer(player.getStringUUID());
        if (optStorage.isEmpty()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.homeless", player)
                            .withStyle(Styles.INFO),
                    true);
            return;
        }

        PlayerData playerStorage = optStorage.get();

        if (homeName.isEmpty()) {
            String defaultHome = playerStorage.getDefaultHome();
            if (defaultHome.isEmpty()) {
                player.sendSystemMessage(
                        Tr_i18n.tr("commands.teleportgui.home.defaultNone", player)
                                .withStyle(Styles.INFO),
                        true);
                return;
            }
            homeName = defaultHome;
        }

        Optional<NamedLoc> optHome = playerStorage.getHome(homeName);
        if (optHome.isEmpty()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.notFound", player)
                            .withStyle(Styles.INFO),
                    true);
            return;
        }

        NamedLoc home = optHome.get();

        Optional<ServerLevel> optWorld = home.getWorld();
        if (optWorld.isEmpty()) {
            Constants.LOGGER.warn("({}) Error while going to home \"{}\"! Couldn't find world with id: \"{}\". Available worlds: {}",
                    player.getName().getString(),
                    home.getName(),
                    home.getWorldId(),
                    TeleportUtils.getWorldIds());

            MutableComponent available = Component.literal(String.join(", ", TeleportUtils.getWorldIds()));
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.common.worldNotFound",
                                    player,
                                    Component.literal(home.getWorldId()),
                                    available)
                            .withStyle(Styles.ERROR_STRONG),
                    true);
            return;
        }

        ServerLevel homeWorld = optWorld.get();
        BlockPos teleportPos = home.getBlockPos();

        if (TeleportUtils.getTeleportPos(player).equals(teleportPos) && player.level() == homeWorld) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.goSame", player)
                            .withStyle(Styles.INFO),
                    true);
            CommandGuard.recordSuccess(player, ConfigKeys.MODULE_HOME, ConfigKeys.HOME_GO);
        } else if (SafetyGuard.tryTeleport(player, homeWorld, teleportPos,
                ConfigKeys.MODULE_HOME, ConfigKeys.HOME_GO)) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.go", player),
                    true);
        }
    }

    public static void deleteHome(ServerPlayer player, String homeName) {
        homeName = homeName.toLowerCase();

        Optional<PlayerData> optStorage = Manager.getPlayer(player.getStringUUID());
        if (optStorage.isEmpty()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.homeless", player)
                            .withStyle(Styles.INFO),
                    true);
            return;
        }

        PlayerData playerStorage = optStorage.get();

        Optional<NamedLoc> optHome = playerStorage.getHome(homeName);
        if (optHome.isEmpty()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.notFound", player)
                            .withStyle(Styles.ERROR),
                    true);
            return;
        }

        playerStorage.deleteHome(optHome.get());

        if (playerStorage.getDefaultHome().equals(homeName)) {
            playerStorage.setDefaultHome("");
        }

        player.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.home.delete", player),
                true);
        CommandGuard.recordSuccess(player, ConfigKeys.MODULE_HOME, ConfigKeys.HOME_DEL);
    }

    public static void renameHome(ServerPlayer player, String homeName, String newName) {
        homeName = homeName.trim().toLowerCase();
        newName = newName.trim().toLowerCase();
        if (!CommandUtils.isValidName(player, newName)) return;

        Optional<PlayerData> optStorage = Manager.getPlayer(player.getStringUUID());
        if (optStorage.isEmpty()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.homeless", player)
                            .withStyle(Styles.INFO),
                    true);
            return;
        }

        PlayerData playerStorage = optStorage.get();

        if (playerStorage.getHome(newName).isPresent()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.common.nameExists", player)
                            .withStyle(Styles.ERROR),
                    true);
            return;
        }

        Optional<NamedLoc> optHome = playerStorage.getHome(homeName);
        if (optHome.isEmpty()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.notFound", player)
                            .withStyle(Styles.ERROR),
                    true);
            return;
        }

        optHome.get().setName(newName);

        if (playerStorage.getDefaultHome().equals(homeName)) {
            playerStorage.setDefaultHome(newName);
        }

        player.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.home.rename", player),
                true);
        CommandGuard.recordSuccess(player, ConfigKeys.MODULE_HOME, ConfigKeys.HOME_RENAME);
    }

    public static void setDefaultHome(ServerPlayer player, String homeName) {
        homeName = homeName.toLowerCase();

        Optional<PlayerData> optStorage = Manager.getPlayer(player.getStringUUID());
        if (optStorage.isEmpty()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.homeless", player)
                            .withStyle(Styles.INFO),
                    true);
            return;
        }

        PlayerData playerStorage = optStorage.get();

        if (playerStorage.getHome(homeName).isEmpty()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.notFound", player)
                            .withStyle(Styles.ERROR),
                    true);
            return;
        }

        if (playerStorage.getDefaultHome().equals(homeName)) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.defaultSame", player)
                            .withStyle(Styles.INFO),
                    true);
            return;
        }

        playerStorage.setDefaultHome(homeName);

        MutableComponent homeNameComp = Component.literal(homeName);
        player.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.home.default", player, homeNameComp),
                true);
        CommandGuard.recordSuccess(player, ConfigKeys.MODULE_HOME, ConfigKeys.HOME_DEFAULT);
    }

    static void printHomes(ServerPlayer player) {
        Optional<PlayerData> optStorage = Manager.getPlayer(player.getStringUUID());
        if (optStorage.isEmpty()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.homeless", player)
                            .withStyle(Styles.INFO),
                    true);
            return;
        }

        PlayerData playerStorage = optStorage.get();
        List<NamedLoc> homes = playerStorage.getHomes();

        if (homes.isEmpty()) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.home.homeless", player)
                            .withStyle(Styles.INFO),
                    true);
            return;
        }

        MutableComponent message = Component.empty();

        message.append(
                Tr_i18n.tr("commands.teleportgui.homes.homes", player)
                        .withStyle(Styles.HEADER)
        );

        for (NamedLoc currentHome : homes) {
            String name = String.format("  - %s", currentHome.getName());
            String coords = String.format("[X%d Y%d Z%d]", currentHome.getX(), currentHome.getY(), currentHome.getZ());
            String dimension = String.format(" [%s]", currentHome.getWorldId());

            message.append("\n");

            message.append(Component.literal(name)
                    .withStyle(Styles.INFO)
            );

            if (playerStorage.getDefaultHome().equals(currentHome.getName())) {
                message.append(" ")
                        .append(Tr_i18n.tr("commands.teleportgui.common.default", player)
                                .withStyle(Styles.INFO_STRONG)
                        );
            }

            message.append("\n");

            final String coordString = String.format("X%d Y%d Z%d", currentHome.getX(), currentHome.getY(), currentHome.getZ());
            final String worldId = currentHome.getWorldId();

            message.append(Component.literal("     | ")
                            .withStyle(Styles.INFO)
                    )
                    .append(Component.literal(coords)
                            .withStyle(Styles.ACTION)
                            .withStyle(style -> style.withClickEvent(
                                    new ClickEvent.CopyToClipboard(coordString)))
                            .withStyle(style -> style.withHoverEvent(
                                    new HoverEvent.ShowText(
                                            Tr_i18n.tr("commands.teleportgui.common.hoverCopy", player))))
                    )
                    .append(Component.literal(dimension)
                            .withStyle(Styles.ACTION_SECONDARY)
                            .withStyle(style -> style.withClickEvent(
                                    new ClickEvent.CopyToClipboard(worldId)))
                            .withStyle(style -> style.withHoverEvent(
                                    new HoverEvent.ShowText(
                                            Tr_i18n.tr("commands.teleportgui.common.hoverCopy", player))))
                    );

            message.append("\n");

            if (TeleportUtils.isBedrockPlayer(player)) {
                message.append(Tr_i18n.tr("commands.teleportgui.common.be.prompt", player));
            } else {
                message.append(Component.literal("     | ")
                                .withStyle(Styles.INFO)
                        )
                        .append(Tr_i18n.tr("commands.teleportgui.common.tp", player)
                                .withStyle(Styles.SUCCESS)
                                .withStyle(style -> style.withClickEvent(
                                        new ClickEvent.RunCommand(
                                                String.format("/home go %s", CommandUtils.quote(currentHome.getName())))))
                        )
                        .append(" ");

                if (!playerStorage.getDefaultHome().equals(currentHome.getName())) {
                    message.append(Tr_i18n.tr("commands.teleportgui.common.defaultPrompt", player)
                                    .withStyle(Styles.ACTION_SOFT)
                                    .withStyle(style -> style.withClickEvent(
                                            new ClickEvent.RunCommand(
                                                    String.format("/home default %s", CommandUtils.quote(currentHome.getName())))))
                            )
                            .append(" ");
                }

                message.append(Tr_i18n.tr("commands.teleportgui.common.delete", player)
                        .withStyle(Styles.ERROR)
                        .withStyle(style -> style.withClickEvent(
                                new ClickEvent.SuggestCommand(
                                        String.format("/home del %s", CommandUtils.quote(currentHome.getName())))))
                );
            }

            message.append("\n");
        }

        player.sendSystemMessage(message, false);
    }
}
