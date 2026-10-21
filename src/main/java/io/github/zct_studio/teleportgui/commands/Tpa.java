package io.github.zct_studio.teleportgui.commands;

import io.github.zct_studio.teleportgui.Styles;
import com.mojang.brigadier.CommandDispatcher;
import io.github.zct_studio.teleportgui.ServerMain;
import io.github.zct_studio.teleportgui.storage.Manager;
import io.github.zct_studio.teleportgui.utils.CommandGuard;
import io.github.zct_studio.teleportgui.utils.SafetyGuard;
import io.github.zct_studio.teleportgui.utils.TeleportUtils;
import io.github.zct_studio.teleportgui.utils.config.CommandLoader;
import io.github.zct_studio.teleportgui.utils.config.ConfigKeys;
import io.github.zct_studio.teleportgui.gui.Form;
import io.github.zct_studio.teleportgui.gui.GUI;
import io.github.zct_studio.teleportgui.Tr_i18n;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.minecraft.commands.Commands.argument;

public final class Tpa {

    public static final CopyOnWriteArrayList<TpaRequest> REQUESTS = new CopyOnWriteArrayList<>();

    private Tpa() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpa")
                .then(argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            ServerPlayer sender = ctx.getSource().getPlayerOrException();
                            return cmdSendRequest(sender, target, false);
                        })));

        dispatcher.register(Commands.literal("tpahere")
                .then(argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            ServerPlayer sender = ctx.getSource().getPlayerOrException();
                            return cmdSendRequest(sender, target, true);
                        })));

        dispatcher.register(Commands.literal("tpaaccept")
                .then(argument("player", EntityArgument.player())
                        .suggests(new io.github.zct_studio.teleportgui.suggestionProviders.Tpa())
                        .executes(ctx -> {
                            ServerPlayer from = EntityArgument.getPlayer(ctx, "player");
                            ServerPlayer to = ctx.getSource().getPlayerOrException();
                            return cmdAccept(from, to);
                        })));

        dispatcher.register(Commands.literal("tpaY")
                .then(argument("player", EntityArgument.player())
                        .suggests(new io.github.zct_studio.teleportgui.suggestionProviders.Tpa())
                        .executes(ctx -> {
                            ServerPlayer from = EntityArgument.getPlayer(ctx, "player");
                            ServerPlayer to = ctx.getSource().getPlayerOrException();
                            return cmdAccept(from, to);
                        })));

        dispatcher.register(Commands.literal("tpadeny")
                .then(argument("player", EntityArgument.player())
                        .suggests(new io.github.zct_studio.teleportgui.suggestionProviders.Tpa())
                        .executes(ctx -> {
                            ServerPlayer from = EntityArgument.getPlayer(ctx, "player");
                            ServerPlayer to = ctx.getSource().getPlayerOrException();
                            return cmdDeny(from, to);
                        })));

        dispatcher.register(Commands.literal("tpaN")
                .then(argument("player", EntityArgument.player())
                        .suggests(new io.github.zct_studio.teleportgui.suggestionProviders.Tpa())
                        .executes(ctx -> {
                            ServerPlayer from = EntityArgument.getPlayer(ctx, "player");
                            ServerPlayer to = ctx.getSource().getPlayerOrException();
                            return cmdDeny(from, to);
                        })));

        dispatcher.register(Commands.literal("gtpa")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    GUI.openTpa(player);
                    return 0;
                }));
    }

    public static List<TpaRequest> getRequestsFor(String recipientUuid) {
        return REQUESTS.stream()
                .filter(r -> Objects.equals(recipientUuid, r.toUuid))
                .toList();
    }

    private static int cmdSendRequest(ServerPlayer from, ServerPlayer to, boolean here) {
        if (!CommandGuard.checkPermission(from, ConfigKeys.MODULE_TPA, ConfigKeys.TPA_REQUEST)) return 1;
        String node = here ? ConfigKeys.TPA_REQUEST_HERE : ConfigKeys.TPA_REQUEST;
        if (!CommandGuard.checkAndWarnCooldown(from, ConfigKeys.MODULE_TPA, node)) return 1;

        if (sendRequest(from, to, here)) {
            CommandGuard.recordSuccess(from, ConfigKeys.MODULE_TPA, node);
        }
        return 0;
    }

    private static int cmdAccept(ServerPlayer from, ServerPlayer to) {
        if (!CommandGuard.checkPermission(to, ConfigKeys.MODULE_TPA, ConfigKeys.TPA_ACCEPT)) return 1;
        accept(from, to);
        return 0;
    }

    private static int cmdDeny(ServerPlayer from, ServerPlayer to) {
        if (!CommandGuard.checkPermission(to, ConfigKeys.MODULE_TPA, ConfigKeys.TPA_DENY)) return 1;
        deny(from, to);
        return 0;
    }

    private static boolean sendRequest(ServerPlayer from, ServerPlayer to, boolean here) {
        if (from.getUUID().equals(to.getUUID())) {
            from.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.tpa.self", from)
                            .withStyle(Styles.INFO),
                    true);
            return false;
        }

        long existing = REQUESTS.stream()
                .filter(r -> Objects.equals(from.getStringUUID(), r.fromUuid))
                .filter(r -> Objects.equals(to.getStringUUID(), r.toUuid))
                .count();

        if (existing >= 1) {
            from.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.tpa.alreadySent", from,
                            Component.literal(to.getName().getString())
                                    .withStyle(Styles.EMPHASIS))
                            .withStyle(Styles.INFO),
                    true);
            return false;
        }

        new TpaRequest(from.getStringUUID(), to.getStringUUID(), here);

        from.sendSystemMessage(
                Tr_i18n.tr(here
                                ? "commands.teleportgui.tpa.sentHere"
                                : "commands.teleportgui.tpa.sent", from,
                        Component.literal(to.getName().getString())
                                .withStyle(Styles.EMPHASIS)),
                true);

        MutableComponent receivedMsg = Tr_i18n.tr(here
                        ? "commands.teleportgui.tpa.receivedHere"
                        : "commands.teleportgui.tpa.received", to,
                        Component.literal(from.getName().getString())
                                .withStyle(Styles.INFO_STRONG))
                .withStyle(Styles.INFO);

        if (TeleportUtils.isBedrockPlayer(to)) {
            boolean autoPop = Manager
                    .getPlayer(to.getStringUUID())
                    .map(io.github.zct_studio.teleportgui.types.PlayerData::isNotificationGuiEnabled)
                    .orElse(true);

            MutableComponent bedrockMsg = Component.empty()
                    .append(receivedMsg);

            if (!autoPop) {
                bedrockMsg.append("\n")
                        .append(Tr_i18n.tr("commands.teleportgui.tpa.be.hint", to)
                                .withStyle(Styles.MUTED_STRONG));
            }
            to.sendSystemMessage(bedrockMsg, false);

            if (autoPop) {
                if (TeleportUtils.hasFloodgate()) {
                    Form.openTpaRequestsForm(to, 0);
                } else {
                    GUI.openTpaRequests(to);
                }
            }
        } else {
            to.sendSystemMessage(
                    Component.empty()
                            .append(receivedMsg)
                            .append("\n")
                            .append(Tr_i18n.tr("commands.teleportgui.tpa.accept", to)
                                    .withStyle(Styles.SUCCESS_STRONG)
                                    .withStyle(s -> s.withClickEvent(
                                            new ClickEvent.RunCommand(
                                                    "/tpaaccept " + from.getName().getString()))))
                            .append(" ")
                            .append(Tr_i18n.tr("commands.teleportgui.tpa.deny", to)
                                    .withStyle(Styles.ERROR_STRONG)
                                    .withStyle(s -> s.withClickEvent(
                                            new ClickEvent.RunCommand(
                                                    "/tpadeny " + from.getName().getString())))),
                    false);
        }
        return true;
    }

    public static void accept(ServerPlayer from, ServerPlayer to) {
        Optional<TpaRequest> reqOpt = REQUESTS.stream()
                .filter(r -> Objects.equals(from.getStringUUID(), r.fromUuid))
                .filter(r -> Objects.equals(to.getStringUUID(), r.toUuid))
                .findFirst();

        if (reqOpt.isEmpty()) {
            to.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.tpa.notFound", to)
                            .withStyle(Styles.ERROR),
                    true);
            return;
        }

        TpaRequest req = reqOpt.get();

        int expireSec = CommandLoader.getRequestExpireSeconds(
                ConfigKeys.MODULE_TPA, ConfigKeys.TPA_REQUEST);
        if (expireSec > 0 && (System.currentTimeMillis() - req.createdAtMs) > expireSec * 1000L) {
            Component expired = Tr_i18n.tr("commands.teleportgui.tpa.expired", to)
                    .withStyle(Styles.ERROR);
            to.sendSystemMessage(expired, true);
            REQUESTS.remove(req);
            return;
        }

        ServerPlayer destination = req.here ? from : to;
        ServerPlayer toTeleport = req.here ? to : from;

        BlockPos destPos = TeleportUtils.getTeleportPos(destination);
        ServerLevel level = destination.level();

        if (SafetyGuard.tryTeleport(toTeleport, level, destPos,
                ConfigKeys.MODULE_TPA, ConfigKeys.TPA_ACCEPT)) {
            from.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.tpa.accepted", from)
                            .withStyle(Styles.BODY),
                    true);
            to.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.tpa.accepted", to)
                            .withStyle(Styles.SUCCESS),
                    true);
        }

        REQUESTS.remove(req);
    }

    public static void deny(ServerPlayer from, ServerPlayer to) {
        Optional<TpaRequest> reqOpt = REQUESTS.stream()
                .filter(r -> Objects.equals(from.getStringUUID(), r.fromUuid))
                .filter(r -> Objects.equals(to.getStringUUID(), r.toUuid))
                .findFirst();

        if (reqOpt.isEmpty()) {
            to.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.tpa.notFound", to)
                            .withStyle(Styles.ERROR),
                    true);
            return;
        }

        TpaRequest req = reqOpt.get();

        int expireSec = CommandLoader.getRequestExpireSeconds(
                ConfigKeys.MODULE_TPA, ConfigKeys.TPA_REQUEST);
        if (expireSec > 0 && (System.currentTimeMillis() - req.createdAtMs) > expireSec * 1000L) {
            Component expired = Tr_i18n.tr("commands.teleportgui.tpa.expired", to)
                    .withStyle(Styles.ERROR);
            to.sendSystemMessage(expired, true);
            REQUESTS.remove(req);
            return;
        }

        REQUESTS.remove(req);

        from.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.tpa.denied", from)
                        .withStyle(Styles.BODY),
                true);
        to.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.tpa.denied", to)
                        .withStyle(Styles.ERROR_STRONG),
                true);
    }

    public static void requestByName(ServerPlayer from, String targetName, boolean here) {
        if (targetName == null || targetName.isBlank()) return;
        ServerPlayer target = ServerMain.SERVER == null ? null
                : ServerMain.SERVER
                        .getPlayerList().getPlayerByName(targetName.trim());
        if (target == null) {
            from.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.tpa.playerNotFound", from,
                                    Component.literal(targetName.trim()))
                            .withStyle(Styles.ERROR),
                    true);
            return;
        }
        if (!CommandGuard.checkPermission(from, ConfigKeys.MODULE_TPA, ConfigKeys.TPA_REQUEST)) return;
        String node = here ? ConfigKeys.TPA_REQUEST_HERE : ConfigKeys.TPA_REQUEST;
        if (!CommandGuard.checkAndWarnCooldown(from, ConfigKeys.MODULE_TPA, node)) return;
        if (sendRequest(from, target, here)) {
            CommandGuard.recordSuccess(from, ConfigKeys.MODULE_TPA, node);
        }
    }

    public static ServerPlayer resolveOnline(String uuid) {
        if (ServerMain.SERVER == null || uuid == null) return null;
        try {
            return ServerMain.SERVER
                    .getPlayerList().getPlayer(java.util.UUID.fromString(uuid));
        } catch (Exception e) {
            return null;
        }
    }

    public static class TpaRequest {
        public final String fromUuid;
        public final String toUuid;
        public final boolean here;
        public final long createdAtMs;

        public TpaRequest(String fromUuid, String toUuid, boolean here) {
            this.fromUuid = fromUuid;
            this.toUuid = toUuid;
            this.here = here;
            this.createdAtMs = System.currentTimeMillis();
            REQUESTS.add(this);
        }
    }
}
