package io.github.zct_studio.teleportgui.gui;

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
import io.github.zct_studio.teleportgui.Tr_i18n;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class Form {

    private Form() {}

    private static final String CUMULUS_CUSTOM = "org.geysermc.cumulus.form.CustomForm";
    private static final String CUMULUS_SIMPLE = "org.geysermc.cumulus.form.SimpleForm";
    private static final String FLOODGATE_API = "org.geysermc.floodgate.api.FloodgateApi";

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
                    case 0 -> Home.goHome(player, name);
                    case 1 -> {
                        Home.deleteHome(player, name);
                        openHomeForm(player, 0);
                    }
                    case 2 -> {
                        Home.setDefaultHome(player, name);
                        openHomeForm(player, 0);
                    }
                    case 3 -> openRenameForm(player, name, false);
                    default -> openCreateForm(player, false);
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
                    Warp.goToWarp(player, name);
                } else if (action == 1 && canManage) {
                    Warp.deleteWarp(player, name);
                    openWarpForm(player, 0);
                } else if (action == 2 && canManage) {
                    openRenameForm(player, name, true);
                } else if (canManage && action == addIndex) {
                    openCreateForm(player, true);
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
            onValid(b, response -> openCreateForm(player, warp));
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
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    true);
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
                            .withStyle(ChatFormatting.RED),
                    true);
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
                                    .withStyle(ChatFormatting.RED),
                            true);
                } else if (warp) {
                    Warp.setWarp(player, newName);
                } else {
                    Home.setHome(player, newName);
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
                                    .withStyle(ChatFormatting.RED),
                            true);
                } else if (warp) {
                    Warp.renameWarp(player, oldName, newName);
                } else {
                    Home.renameHome(player, oldName, newName);
                }
                if (warp) openWarpForm(player, 0);
                else openHomeForm(player, 0);
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
            actions.add(() -> openTpaRequestsForm(player, 0));
            call(b, "button", text(player, "gui.teleportgui.tpa.tpagui.new"));
            actions.add(() -> openTpaRequestForm(player));

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
                actions.add(() -> openTpaRequestsForm(player, p - 1));
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
                    actions.add(() -> openTpaRequestDialog(player, fromUuid));
                }
            }

            if (p < totalPages - 1) {
                call(b, "button", text(player, "gui.teleportgui.universal.gui.pgdn"));
                actions.add(() -> openTpaRequestsForm(player, p + 1));
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

    private static void openTpaRequestDialog(ServerPlayer player, String fromUuid) {
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
            actions.add(() -> openTpaRequestsForm(player, 0));

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
            });
            send(player, build(b));
        } catch (Throwable t) {
            fallback(player, t);
        }
    }

    public static void openSafetyConfirm(ServerPlayer player, Runnable onForce) {
        try {
            Object b = newBuilder(CUMULUS_SIMPLE);
            call(b, "title", text(player, "commands.teleportgui.common.noSafeLocation"));
            call(b, "content", text(player, "commands.teleportgui.common.safetyIsForLosers"));

            List<Runnable> actions = new ArrayList<>();
            call(b, "button", text(player, "gui.teleportgui.universal.gui.no"));
            actions.add(() -> { });
            call(b, "button", text(player, "commands.teleportgui.common.forceTeleport"));
            actions.add(onForce);

            onValid(b, response -> {
                int id = intValue(response, "clickedButtonId");
                if (id >= 0 && id < actions.size()) actions.get(id).run();
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
            Class<?> apiClass = Class.forName(FLOODGATE_API);
            Object api = apiClass.getMethod("api").invoke(null);
            for (Method m : apiClass.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getName().equals("sendForm") && p.length == 2 && p[0] == UUID.class) {
                    m.setAccessible(true);
                    m.invoke(api, player.getUUID(), form);
                    return;
                }
            }
            throw new NoSuchMethodException("FloodgateApi.sendForm");
        } catch (Throwable t) {
            fallback(player, t);
        }
    }

    private static void fallback(ServerPlayer player, Throwable t) {
        Constants.LOGGER.error("Bedrock form failed for {}", player.getName().getString(), t);
        player.sendSystemMessage(
                Tr_i18n.tr("gui.teleportgui.universal.gui.formError", player)
                        .withStyle(ChatFormatting.RED),
                true);
    }
}
