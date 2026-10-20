package io.github.zct_studio.teleportgui.gui;

import io.github.zct_studio.teleportgui.Styles;
import io.github.zct_studio.teleportgui.commands.Home;
import io.github.zct_studio.teleportgui.commands.Tpa;
import io.github.zct_studio.teleportgui.commands.Warp;
import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.storage.Manager;
import io.github.zct_studio.teleportgui.ServerMain;
import io.github.zct_studio.teleportgui.types.NamedLoc;
import io.github.zct_studio.teleportgui.types.PlayerData;
import io.github.zct_studio.teleportgui.utils.config.CommandLoader;
import io.github.zct_studio.teleportgui.utils.config.ConfigKeys;
import io.github.zct_studio.teleportgui.utils.TeleportUtils;
import io.github.zct_studio.teleportgui.Tr_i18n;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class Form {

    private Form() {}

    private static final String CUMULUS_CUSTOM = "org.geysermc.cumulus.form.CustomForm";
    private static final String CUMULUS_SIMPLE = "org.geysermc.cumulus.form.SimpleForm";
    private static final String CUMULUS_MODAL = "org.geysermc.cumulus.form.ModalForm";
    private static final String CUMULUS_FORM = "org.geysermc.cumulus.form.Form";
    private static final String FLOODGATE_API = "org.geysermc.floodgate.api.FloodgateApi";

    private static final Map<UUID, Integer> FORM_SEQ = new ConcurrentHashMap<>();

    public static void openHomeForm(ServerPlayer player, int page) {
        Optional<PlayerData> opt = Manager.getPlayer(player.getStringUUID());
        List<NamedLoc> homes = opt.map(PlayerData::getHomes).orElseGet(List::of);
        String def = opt.map(PlayerData::getDefaultHome).orElse("");
        String title = text(player, "gui.teleportgui.home.homegui.title");

        if (homes.isEmpty()) {
            openCreateEntryForm(player, false);
            return;
        }
        try {
            Object b = newBuilder(CUMULUS_CUSTOM);
            call(b, "title", title);
            call(b, "label", text(player, "gui.teleportgui.be.summary", String.valueOf(homes.size())));
            if (!def.isEmpty()) {
                call(b, "label", text(player, "gui.teleportgui.be.defaultHome", def));
            }
            call(b, "dropdown", text(player, "gui.teleportgui.be.selectEntry"),
                    homes.stream().map(NamedLoc::getName).toList());
            call(b, "dropdown", text(player, "gui.teleportgui.be.selectAction"),
                    List.of(text(player, "gui.teleportgui.universal.gui.tp"),
                            text(player, "gui.teleportgui.universal.gui.del"),
                            text(player, "gui.teleportgui.home.homeactiongui.defaulthome"),
                            text(player, "gui.teleportgui.universal.gui.rename"),
                            text(player, "gui.teleportgui.universal.gui.add")));

            onValid(b, response -> {
                int entry = intValue(response, "getDropdown", 0);
                int action = intValue(response, "getDropdown", 1);
                if (entry < 0 || entry >= homes.size()) return;
                String name = homes.get(entry).getName();
                switch (action) {
                    case 0 -> reopen(player, () -> Home.goHome(player, name));
                    case 1 -> afterAction(player,
                            () -> Home.deleteHome(player, name),
                            () -> openHomeForm(player, page));
                    case 2 -> afterAction(player,
                            () -> Home.setDefaultHome(player, name),
                            () -> openHomeForm(player, page));
                    case 3 -> reopen(player, () -> openRenameForm(player, name, false));
                    default -> reopen(player, () -> openCreateForm(player, false));
                }
            });
            send(player, build(b));
        } catch (Throwable t) {
            fallback(player, t);
        }
    }

    public static void openWarpForm(ServerPlayer player, int page) {
        List<NamedLoc> warps = Manager.getWarps();
        String title = text(player, "gui.teleportgui.warp.warpgui.title");

        boolean canManage = player.permissions().hasPermission(
                new Permission.HasCommandLevel(PermissionLevel.OWNERS));

        if (warps.isEmpty()) {
            if (canManage) openCreateEntryForm(player, true);
            else notice(player, title, text(player, "gui.teleportgui.warp.warpgui.empty"));
            return;
        }

        List<String> actions = new ArrayList<>();
        actions.add(text(player, "gui.teleportgui.universal.gui.tp"));
        if (canManage) {
            actions.add(text(player, "gui.teleportgui.universal.gui.del"));
            actions.add(text(player, "gui.teleportgui.universal.gui.rename"));
            actions.add(text(player, "gui.teleportgui.universal.gui.add"));
        }
        final int addIndex = actions.size() - 1;

        try {
            Object b = newBuilder(CUMULUS_CUSTOM);
            call(b, "title", title);
            call(b, "label", text(player, "gui.teleportgui.be.summary", String.valueOf(warps.size())));
            call(b, "dropdown", text(player, "gui.teleportgui.be.selectEntry"),
                    warps.stream().map(NamedLoc::getName).toList());
            call(b, "dropdown", text(player, "gui.teleportgui.be.selectAction"), actions);

            onValid(b, response -> {
                int entry = intValue(response, "getDropdown", 0);
                int action = intValue(response, "getDropdown", 1);
                if (entry < 0 || entry >= warps.size()) return;
                String name = warps.get(entry).getName();
                if (action == 0) {
                    reopen(player, () -> Warp.goToWarp(player, name));
                } else if (action == 1 && canManage) {
                    afterAction(player,
                            () -> Warp.deleteWarp(player, name),
                            () -> openWarpForm(player, page));
                } else if (action == 2 && canManage) {
                    reopen(player, () -> openRenameForm(player, name, true));
                } else if (canManage && action == addIndex) {
                    reopen(player, () -> openCreateForm(player, true));
                }
            });
            send(player, build(b));
        } catch (Throwable t) {
            fallback(player, t);
        }
    }

    private static void openCreateEntryForm(ServerPlayer player, boolean warp) {
        String title = text(player, warp ? "gui.teleportgui.warp.warpgui.title"
                : "gui.teleportgui.home.homegui.title");
        String empty = text(player, warp ? "gui.teleportgui.warp.warpgui.empty"
                : "gui.teleportgui.home.homegui.empty");
        try {
            Object b = newBuilder(CUMULUS_SIMPLE);
            call(b, "title", title);
            call(b, "content", empty);
            call(b, "button", text(player, "gui.teleportgui.universal.gui.add"));
            onValid(b, response -> reopen(player, () -> openCreateForm(player, warp)));
            send(player, build(b));
        } catch (Throwable t) {
            fallback(player, t);
        }
    }

    private static void openCreateForm(ServerPlayer player, boolean warp) {
        if (warp && !player.permissions().hasPermission(
                new Permission.HasCommandLevel(PermissionLevel.OWNERS))) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.common.noPermission", player)
                            .withStyle(Styles.ERROR_STRONG),
                    true);
            reopen(player, () -> openWarpForm(player, 0));
            return;
        }
        int max = CommandLoader.getMaxCount(
                warp ? ConfigKeys.MODULE_WARP : ConfigKeys.MODULE_HOME,
                warp ? ConfigKeys.WARP_CREATE : ConfigKeys.HOME_CREATE);
        int count = warp
                ? Manager.getWarps().size()
                : Manager.getPlayer(player.getStringUUID())
                        .map(d -> d.getHomes().size()).orElse(0);
        if (max >= 0 && count >= max) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.common.maxCountReached", player,
                                    Component.literal(String.valueOf(max)))
                            .withStyle(Styles.ERROR),
                    true);
            reopen(player, () -> {
                if (warp) openWarpForm(player, 0);
                else openHomeForm(player, 0);
            });
            return;
        }
        try {
            Object b = newBuilder(CUMULUS_CUSTOM);
            call(b, "title", text(player, "gui.teleportgui.rename.createTitle"));
            call(b, "input", text(player, "gui.teleportgui.rename.createInput"), "");
            onValid(b, response -> {
                String newName = stringValue(response, "getInput", 0).trim();
                if (newName.isEmpty()) {
                    player.sendSystemMessage(
                            Tr_i18n.tr("gui.teleportgui.rename.newEmpty", player)
                                    .withStyle(Styles.ERROR),
                            true);
                    reopen(player, () -> openCreateForm(player, warp));
                } else if (warp) {
                    Warp.setWarp(player, newName);
                    reopen(player, () -> openWarpForm(player, 0));
                } else {
                    Home.setHome(player, newName);
                    reopen(player, () -> openHomeForm(player, 0));
                }
            });
            send(player, build(b));
        } catch (Throwable t) {
            fallback(player, t);
        }
    }

    private static void openRenameForm(ServerPlayer player, String oldName, boolean warp) {
        try {
            Object b = newBuilder(CUMULUS_CUSTOM);
            call(b, "title", text(player, "gui.teleportgui.rename.title", oldName));
            call(b, "label", text(player, "gui.teleportgui.rename.old", oldName));
            call(b, "input", text(player, "gui.teleportgui.rename.input"), "");

            onValid(b, response -> {
                String newName = stringValue(response, "getInput", 0).trim();
                if (newName.isEmpty()) {
                    player.sendSystemMessage(
                            Tr_i18n.tr("gui.teleportgui.rename.newEmpty", player)
                                    .withStyle(Styles.ERROR),
                            true);
                    reopen(player, () -> openRenameForm(player, oldName, warp));
                } else if (warp) {
                    Warp.renameWarp(player, oldName, newName);
                    reopen(player, () -> openWarpForm(player, 0));
                } else {
                    Home.renameHome(player, oldName, newName);
                    reopen(player, () -> openHomeForm(player, 0));
                }
            });
            send(player, build(b));
        } catch (Throwable t) {
            fallback(player, t);
        }
    }

    public static void openTpaForm(ServerPlayer player) {
        openTpaForm(player, 0);
    }

    public static void openTpaForm(ServerPlayer player, int page) {
        try {
            Object b = newBuilder(CUMULUS_SIMPLE);
            call(b, "title", text(player, "gui.teleportgui.tpa.tpagui.title"));
            call(b, "content", text(player, "gui.teleportgui.be.tpaSummary"));

            List<Runnable> actions = new ArrayList<>();
            call(b, "button", text(player, "gui.teleportgui.be.viewRequests"));
            actions.add(() -> reopen(player, () -> openTpaRequestsForm(player, 0)));
            call(b, "button", text(player, "gui.teleportgui.tpa.tpagui.new"));
            actions.add(() -> reopen(player, () -> openTpaRequestForm(player)));

            onValid(b, response -> {
                int id = intValue(response, "clickedButtonId");
                if (id >= 0 && id < actions.size()) actions.get(id).run();
            });
            send(player, build(b));
        } catch (Throwable t) {
            fallback(player, t);
        }
    }

    public static void openTpaRequestsForm(ServerPlayer player, int page) {
        List<Tpa.TpaRequest> requests = Tpa.getRequestsFor(player.getStringUUID());
        int perPage = 45;
        int totalPages = Math.max(1, (requests.size() + perPage - 1) / perPage);
        int p = Math.max(0, Math.min(page, totalPages - 1));

        try {
            Object b = newBuilder(CUMULUS_SIMPLE);
            call(b, "title", text(player, "gui.teleportgui.universal.gui.titlePage",
                    text(player, "gui.teleportgui.tpa.tpagui.title"),
                    String.valueOf(p + 1), String.valueOf(totalPages)));

            List<Runnable> actions = new ArrayList<>();
            if (p > 0) {
                call(b, "button", text(player, "gui.teleportgui.universal.gui.pgup"));
                actions.add(() -> reopen(player, () -> openTpaRequestsForm(player, p - 1)));
            }

            if (requests.isEmpty()) {
                call(b, "content", text(player, "gui.teleportgui.tpa.tpagui.empty"));
            } else {
                int start = p * perPage;
                int end = Math.min(start + perPage, requests.size());
                for (int i = start; i < end; i++) {
                    Tpa.TpaRequest req = requests.get(i);
                    final String fromUuid = req.fromUuid;
                    call(b, "button", text(player, "gui.teleportgui.tpa.tpagui.line",
                            String.valueOf(i + 1),
                            senderName(req),
                            text(player, req.here
                                    ? "gui.teleportgui.tpa.tpagui.here"
                                    : "gui.teleportgui.tpa.tpagui.tocome"),
                            ageText(req)));
                    actions.add(() -> reopen(player, () -> openTpaRequestDialog(player, fromUuid, p)));
                }
            }

            if (p < totalPages - 1) {
                call(b, "button", text(player, "gui.teleportgui.universal.gui.pgdn"));
                actions.add(() -> reopen(player, () -> openTpaRequestsForm(player, p + 1)));
            }

            onValid(b, response -> {
                int id = intValue(response, "clickedButtonId");
                if (id >= 0 && id < actions.size()) actions.get(id).run();
            });
            send(player, build(b));
        } catch (Throwable t) {
            fallback(player, t);
        }
    }

    private static void openTpaRequestDialog(ServerPlayer player, String fromUuid, int page) {
        ServerPlayer from = Tpa.resolveOnline(fromUuid);
        try {
            Object b = newBuilder(CUMULUS_SIMPLE);
            call(b, "title", text(player, "gui.teleportgui.tpa.tpareqdialog.title"));
            call(b, "content", from == null
                    ? text(player, "commands.teleportgui.tpa.notFound")
                    : from.getName().getString());

            List<Runnable> actions = new ArrayList<>();
            call(b, "button", text(player, "gui.teleportgui.tpa.tpareqdialog.accept"));
            actions.add(() -> {
                if (from != null) Tpa.accept(from, player);
            });
            call(b, "button", text(player, "gui.teleportgui.tpa.tpareqdialog.deny"));
            actions.add(() -> {
                if (from != null) Tpa.deny(from, player);
            });
            call(b, "button", text(player, "gui.teleportgui.universal.gui.back"));
            actions.add(() -> reopen(player, () -> openTpaRequestsForm(player, page)));

            onValid(b, response -> {
                int id = intValue(response, "clickedButtonId");
                if (id >= 0 && id < actions.size()) actions.get(id).run();
            });
            send(player, build(b));
        } catch (Throwable t) {
            fallback(player, t);
        }
    }

    private static void openTpaRequestForm(ServerPlayer player) {
        MinecraftServer server = ServerMain.SERVER;
        if (server == null) return;

        List<String> names = server.getPlayerList().getPlayers().stream()
                .filter(p -> !p.getUUID().equals(player.getUUID()))
                .map(p -> p.getName().getString())
                .toList();

        String title = text(player, "gui.teleportgui.tpa.tpagui.new");
        if (names.isEmpty()) {
            notice(player, title, text(player, "gui.teleportgui.tpa.tpagui.noPlayers"));
            return;
        }
        try {
            Object b = newBuilder(CUMULUS_CUSTOM);
            call(b, "title", title);
            call(b, "dropdown", text(player, "gui.teleportgui.tpa.tpagui.target"), names);
            call(b, "dropdown", text(player, "gui.teleportgui.tpa.tpagui.type"),
                    List.of(text(player, "gui.teleportgui.tpa.tpagui.typeTpa"),
                            text(player, "gui.teleportgui.tpa.tpagui.typeHere")));

            onValid(b, response -> {
                int target = intValue(response, "getDropdown", 0);
                int type = intValue(response, "getDropdown", 1);
                if (target >= 0 && target < names.size()) {
                    Tpa.requestByName(player, names.get(target), type == 1);
                }
                reopen(player, () -> openTpaForm(player));
            });
            send(player, build(b));
        } catch (Throwable t) {
            fallback(player, t);
        }
    }

    public static void openSafetyConfirm(ServerPlayer player, Runnable onForce) {
        try {
            Object b = newBuilder(CUMULUS_MODAL);
            call(b, "title", text(player, "commands.teleportgui.common.noSafeLocation"));
            call(b, "content", text(player, "commands.teleportgui.common.safetyIsForLosers"));
            call(b, "button1", text(player, "gui.teleportgui.universal.gui.cancel"));
            call(b, "button2", text(player, "commands.teleportgui.common.forceTeleport"));

            onValid(b, response -> {
                if (intValue(response, "getClickedButtonId") == 1) onForce.run();
            });
            send(player, build(b));
        } catch (Throwable t) {
            fallback(player, t);
        }
    }

    private static void notice(ServerPlayer player, String title, String content) {
        try {
            Object b = newBuilder(CUMULUS_SIMPLE);
            call(b, "title", title);
            call(b, "content", content);
            call(b, "button", text(player, "gui.teleportgui.universal.gui.close"));
            onValid(b, response -> { });
            send(player, build(b));
        } catch (Throwable t) {
            fallback(player, t);
        }
    }

    private static String text(ServerPlayer player, String key, String... args) {
        return Tr_i18n.text(key, player, args);
    }

    private static void afterAction(ServerPlayer player, Runnable action, Runnable opener) {
        int seq = formSequence(player);
        action.run();
        if (formSequence(player) != seq) return;
        reopen(player, seq, opener);
    }

    private static void reopen(ServerPlayer player, Runnable opener) {
        reopen(player, formSequence(player), opener);
    }

    private static void reopen(ServerPlayer player, int seq, Runnable opener) {
        MinecraftServer server = ServerMain.SERVER;
        if (server == null) return;
        server.execute(() -> {
            if (server.getPlayerList().getPlayer(player.getUUID()) != player) return;
            if (formSequence(player) != seq) return;
            opener.run();
        });
    }

    private static int formSequence(ServerPlayer player) {
        return FORM_SEQ.getOrDefault(player.getUUID(), 0);
    }

    private static String senderName(Tpa.TpaRequest req) {
        ServerPlayer from = Tpa.resolveOnline(req.fromUuid);
        return from == null ? req.fromUuid.substring(0, Math.min(8, req.fromUuid.length()))
                : from.getName().getString();
    }

    private static String ageText(Tpa.TpaRequest req) {
        long ageSec = (System.currentTimeMillis() - req.createdAtMs) / 1000;
        return ageSec < 60 ? ageSec + "s" : ageSec / 60 + "m";
    }

    private static Object newBuilder(String className) throws Exception {
        return Class.forName(className).getMethod("builder").invoke(null);
    }

    private static Object call(Object target, String method, Object... args) throws Exception {
        Method m = find(target.getClass(), method, args);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private static Method find(Class<?> type, String name, Object... args) throws NoSuchMethodException {
        for (Method m : type.getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
            Class<?>[] params = m.getParameterTypes();
            boolean ok = true;
            for (int i = 0; i < params.length; i++) {
                if (!accepts(params[i], args[i])) {
                    ok = false;
                    break;
                }
            }
            if (ok) return m;
        }
        throw new NoSuchMethodException(name);
    }

    private static boolean accepts(Class<?> param, Object arg) {
        if (arg == null) return !param.isPrimitive();
        if (param.isInstance(arg)) return true;
        if (param == int.class) return arg instanceof Integer;
        if (param == boolean.class) return arg instanceof Boolean;
        if (param == float.class) return arg instanceof Float;
        if (param == long.class) return arg instanceof Long;
        if (param == double.class) return arg instanceof Double;
        return false;
    }

    private static void onValid(Object builder, Consumer<Object> handler) throws Exception {
        Object proxy = Proxy.newProxyInstance(
                Form.class.getClassLoader(),
                new Class<?>[]{Consumer.class},
                (p, method, args) -> {
                    if ("accept".equals(method.getName()) && args != null && args.length == 1) {
                        handler.accept(args[0]);
                    }
                    return null;
                });
        call(builder, "validResultHandler", proxy);
    }

    private static Object build(Object builder) throws Exception {
        return call(builder, "build");
    }

    private static int intValue(Object target, String method, Object... args) {
        try {
            Object v = call(target, method, args);
            return v instanceof Integer i ? i : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static String stringValue(Object target, String method, Object... args) {
        try {
            Object v = call(target, method, args);
            return v == null ? "" : String.valueOf(v);
        } catch (Throwable t) {
            return "";
        }
    }

    private static void send(ServerPlayer player, Object form) {
        try {
            Object api = TeleportUtils.floodgateApi();
            if (api == null) throw new IllegalStateException("Floodgate API not available");
            Class<?> apiClass = Class.forName(FLOODGATE_API);
            Method m = apiClass.getMethod("sendForm", UUID.class, Class.forName(CUMULUS_FORM));
            m.setAccessible(true);
            if (Boolean.FALSE.equals(m.invoke(api, player.getUUID(), form))) {
                throw new IllegalStateException("Floodgate refused to send the form");
            }
            FORM_SEQ.merge(player.getUUID(), 1, Integer::sum);
        } catch (Throwable t) {
            fallback(player, t);
        }
    }

    private static void fallback(ServerPlayer player, Throwable t) {
        Constants.LOGGER.error("Bedrock form failed for {}", player.getName().getString(), t);
        player.sendSystemMessage(
                Tr_i18n.tr("gui.teleportgui.universal.gui.formError", player)
                        .withStyle(Styles.ERROR),
                true);
    }
}
