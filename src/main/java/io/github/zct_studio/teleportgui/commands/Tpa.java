package io.github.zct_studio.teleportgui.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.github.zct_studio.teleportgui.ServerMain;
import io.github.zct_studio.teleportgui.Styles;
import io.github.zct_studio.teleportgui.Tr_i18n;
import io.github.zct_studio.teleportgui.gui.Form;
import io.github.zct_studio.teleportgui.gui.GUI;
import io.github.zct_studio.teleportgui.storage.Manager;
import io.github.zct_studio.teleportgui.storage.RuntimeStore;
import io.github.zct_studio.teleportgui.types.PlayerData;
import io.github.zct_studio.teleportgui.utils.CommandGuard;
import io.github.zct_studio.teleportgui.utils.SafetyGuard;
import io.github.zct_studio.teleportgui.utils.TeleportUtils;
import io.github.zct_studio.teleportgui.utils.config.CommandLoader;
import io.github.zct_studio.teleportgui.utils.config.ConfigKeys;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.minecraft.commands.Commands.argument;

public final class Tpa {

    /**
     * 待处理的传送请求。
     * <p>
     * 所有读写都发生在服务端主线程（命令执行、容器点击、tick 回调），因此用普通
     * ArrayList 即可。之前用的是 CopyOnWriteArrayList，但它的 remove 会复制整个底层数组，
     * cleanup 里一边遍历一边删是 O(n²)，请求一多就明显变慢。
     * <p>
     * 注意：本字段不持有锁，请勿在异步线程访问。
     */
    private static final List<TpaRequest> REQUESTS = new ArrayList<>();

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

        dispatcher.register(Commands.literal("tpay")
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

        dispatcher.register(Commands.literal("tpan")
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
        long now = currentTick();
        return REQUESTS.stream()
                .filter(r -> Objects.equals(recipientUuid, r.toUuid))
                .filter(r -> !r.isExpired(now))
                .toList();
    }

    public static TpaRequest findRequest(String fromUuid, String toUuid) {
        long now = currentTick();
        return REQUESTS.stream()
                .filter(r -> Objects.equals(fromUuid, r.fromUuid))
                .filter(r -> Objects.equals(toUuid, r.toUuid))
                .filter(r -> !r.isExpired(now))
                .findFirst()
                .orElse(null);
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
        if (!acceptChecked(from, to)) return 1;
        return 0;
    }

    private static int cmdDeny(ServerPlayer from, ServerPlayer to) {
        if (!denyChecked(from, to)) return 1;
        return 0;
    }

    /**
     * 接受请求前先校验权限，供命令、GUI 和表单共用，避免绕过 {@code tpa.accept} 节点。
     * 返回 false 表示权限不足或没有待处理请求。
     */
    public static boolean acceptChecked(ServerPlayer from, ServerPlayer to) {
        if (!CommandGuard.checkPermission(to, ConfigKeys.MODULE_TPA, ConfigKeys.TPA_ACCEPT)) return false;
        return accept(from, to);
    }

    /**
     * 拒绝请求前先校验权限，供命令、GUI 和表单共用，避免绕过 {@code tpa.deny} 节点。
     */
    public static boolean denyChecked(ServerPlayer from, ServerPlayer to) {
        if (!CommandGuard.checkPermission(to, ConfigKeys.MODULE_TPA, ConfigKeys.TPA_DENY)) return false;
        return deny(from, to);
    }

    private static boolean sendRequest(ServerPlayer from, ServerPlayer to, boolean here) {
        if (from.getUUID().equals(to.getUUID())) {
            from.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.tpa.self", from)
                            .withStyle(Styles.INFO),
                    true);
            return false;
        }

        boolean allowed = Manager.getPlayer(to.getStringUUID())
                .map(PlayerData::isRequestAllowed)
                .orElse(true);
        if (!allowed) {
            from.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.tpa.targetDisabled", from,
                                    Component.literal(to.getName().getString())
                                            .withStyle(Styles.EMPHASIS))
                            .withStyle(Styles.ERROR),
                    false);
            return false;
        }

        long now = currentTick();
        boolean purged = REQUESTS.removeIf(request ->
                Objects.equals(from.getStringUUID(), request.fromUuid)
                        && Objects.equals(to.getStringUUID(), request.toUuid)
                        && request.isExpired(now));
        if (purged) RuntimeStore.markDirty();

        long existing = REQUESTS.stream()
                .filter(r -> Objects.equals(from.getStringUUID(), r.fromUuid))
                .filter(r -> Objects.equals(to.getStringUUID(), r.toUuid))
                .filter(r -> !r.isExpired(now))
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
        persist();

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
                    .map(PlayerData::isNotificationGuiEnabled)
                    .orElse(CommandLoader.isNotifGuiDefaultEnabled());

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
                    Form.openTpaRequestDialog(to, from.getStringUUID(), 0, true);
                } else {
                    GUI.openTpaRequestActions(to, from.getStringUUID(), true);
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

    /**
     * 接受一个 TPA 请求。调用方必须先完成权限校验（命令走 {@link #acceptChecked}，
     * GUI/表单同样只能通过 {@link #acceptChecked} 进入）。
     * <p>
     * 如果目标位置附近找不到安全落点，{@link SafetyGuard} 会改为等待玩家确认「强制传送」。
     * 这种情况下请求<b>不会被提前删除</b>，而是把删除动作挂到确认回调上，
     * 等玩家真的被传送过去之后才消耗掉，避免出现「请求没了但人也没传过去」的中间态。
     *
     * @return 找到并处理了请求（含已进入等待确认状态）时返回 true
     */
    public static boolean accept(ServerPlayer from, ServerPlayer to) {
        long now = currentTick();
        Optional<TpaRequest> reqOpt = REQUESTS.stream()
                .filter(r -> Objects.equals(from.getStringUUID(), r.fromUuid))
                .filter(r -> Objects.equals(to.getStringUUID(), r.toUuid))
                .findFirst();

        if (reqOpt.isEmpty()) {
            to.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.tpa.notFound", to)
                            .withStyle(Styles.ERROR),
                    true);
            return false;
        }

        TpaRequest req = reqOpt.get();
        if (req.isExpired(now)) {
            Component expired = Tr_i18n.tr("commands.teleportgui.tpa.expired", to)
                    .withStyle(Styles.ERROR);
            to.sendSystemMessage(expired, true);
            removeRequest(req);
            return false;
        }

        ServerPlayer destination = req.here ? from : to;
        ServerPlayer toTeleport = req.here ? to : from;

        BlockPos destPos = TeleportUtils.getTeleportPos(destination);
        ServerLevel level = destination.level();

        // 强制传送成功后才消耗请求；直接传送成功则下面的 removeRequest 会立即执行。
        boolean teleported = SafetyGuard.tryTeleport(toTeleport, level, destPos,
                ConfigKeys.MODULE_TPA, ConfigKeys.TPA_ACCEPT, () -> removeRequest(req));

        if (teleported) {
            from.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.tpa.accepted", from)
                            .withStyle(Styles.BODY),
                    true);
            to.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.tpa.accepted", to)
                            .withStyle(Styles.SUCCESS),
                    true);
            removeRequest(req);
        }
        // teleported == false 时玩家正在确认强制传送，请求保留在列表里，
        // 由上面注册的回调在传送完成后删除。
        return true;
    }

    /**
     * 拒绝一个 TPA 请求。调用方必须先完成权限校验（见 {@link #denyChecked}）。
     *
     * @return 找到并处理了请求时返回 true
     */
    public static boolean deny(ServerPlayer from, ServerPlayer to) {
        long now = currentTick();
        Optional<TpaRequest> reqOpt = REQUESTS.stream()
                .filter(r -> Objects.equals(from.getStringUUID(), r.fromUuid))
                .filter(r -> Objects.equals(to.getStringUUID(), r.toUuid))
                .findFirst();

        if (reqOpt.isEmpty()) {
            to.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.tpa.notFound", to)
                            .withStyle(Styles.ERROR),
                    true);
            return false;
        }

        TpaRequest req = reqOpt.get();
        if (req.isExpired(now)) {
            Component expired = Tr_i18n.tr("commands.teleportgui.tpa.expired", to)
                    .withStyle(Styles.ERROR);
            to.sendSystemMessage(expired, true);
            removeRequest(req);
            return false;
        }

        removeRequest(req);

        from.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.tpa.denied", from)
                        .withStyle(Styles.BODY),
                true);
        to.sendSystemMessage(
                Tr_i18n.tr("commands.teleportgui.tpa.denied", to)
                        .withStyle(Styles.ERROR_STRONG),
                true);
        return true;
    }

    private static void removeRequest(TpaRequest request) {
        REQUESTS.remove(request);
        RuntimeStore.markDirty();
        persist();
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

    private static void persist() {
        long now = currentTick();
        RuntimeStore.save(CommandGuard.snapshot(now), snapshot(now));
    }

    public static void cleanup(long now) {
        if (REQUESTS.removeIf(request -> request.isExpired(now))) {
            RuntimeStore.markDirty();
        }
    }

    public static List<RuntimeStore.TpaEntry> snapshot(long now) {
        List<RuntimeStore.TpaEntry> result = new ArrayList<>();
        for (TpaRequest request : REQUESTS) {
            long remaining = request.remainingTicks(now);
            if (remaining <= 0) continue;

            RuntimeStore.TpaEntry entry = new RuntimeStore.TpaEntry();
            entry.from = request.fromUuid;
            entry.to = request.toUuid;
            entry.here = request.here;
            entry.ageTicks = request.ageTicks(now);
            entry.remainingTicks = remaining;
            result.add(entry);
        }
        return result;
    }

    public static void loadPersisted(List<RuntimeStore.TpaEntry> entries, long now) {
        REQUESTS.clear();
        if (entries == null) return;

        for (RuntimeStore.TpaEntry entry : entries) {
            if (entry == null || entry.from == null || entry.to == null) continue;
            if (entry.remainingTicks <= 0) continue;

            long createdAtTick = now - Math.max(0L, entry.ageTicks);
            long expiresAtTick = entry.remainingTicks == Long.MAX_VALUE
                    ? Long.MAX_VALUE
                    : now + entry.remainingTicks;
            REQUESTS.add(new TpaRequest(entry.from, entry.to, entry.here,
                    createdAtTick, expiresAtTick));
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

    private static long currentTick() {
        MinecraftServer server = ServerMain.SERVER;
        return server == null ? 0L : server.getTickCount();
    }

    public static class TpaRequest {
        public final String fromUuid;
        public final String toUuid;
        public final boolean here;
        public final long createdAtTick;
        public final long expiresAtTick;

        public TpaRequest(String fromUuid, String toUuid, boolean here) {
            long now = currentTick();
            this.fromUuid = fromUuid;
            this.toUuid = toUuid;
            this.here = here;
            this.createdAtTick = now;
            int expireSec = CommandLoader.getExpireSeconds(ConfigKeys.MODULE_TPA,
                    here ? ConfigKeys.TPA_REQUEST_HERE : ConfigKeys.TPA_REQUEST);
            this.expiresAtTick = expireSec > 0 ? now + expireSec * 20L : Long.MAX_VALUE;
            REQUESTS.add(this);
        }

        private TpaRequest(String fromUuid, String toUuid, boolean here,
                           long createdAtTick, long expiresAtTick) {
            this.fromUuid = fromUuid;
            this.toUuid = toUuid;
            this.here = here;
            this.createdAtTick = createdAtTick;
            this.expiresAtTick = expiresAtTick;
        }

        public long ageSeconds() {
            return Math.max(0L, (currentTick() - createdAtTick) / 20L);
        }

        public long ageTicks(long now) {
            return Math.max(0L, now - createdAtTick);
        }

        public boolean isExpired(long now) {
            return expiresAtTick != Long.MAX_VALUE && now >= expiresAtTick;
        }

        public long remainingTicks(long now) {
            return expiresAtTick == Long.MAX_VALUE
                    ? Long.MAX_VALUE
                    : Math.max(0L, expiresAtTick - now);
        }
    }
}
