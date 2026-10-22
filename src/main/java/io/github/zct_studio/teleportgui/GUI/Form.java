package io.github.zct_studio.teleportgui.gui;

import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.ServerMain;
import io.github.zct_studio.teleportgui.Styles;
import io.github.zct_studio.teleportgui.Tr_i18n;
import io.github.zct_studio.teleportgui.commands.Home;
import io.github.zct_studio.teleportgui.commands.Tpa;
import io.github.zct_studio.teleportgui.commands.Warp;
import io.github.zct_studio.teleportgui.storage.Manager;
import io.github.zct_studio.teleportgui.types.NamedLoc;
import io.github.zct_studio.teleportgui.types.PlayerData;
import io.github.zct_studio.teleportgui.utils.CommandGuard;
import io.github.zct_studio.teleportgui.utils.TeleportUtils;
import io.github.zct_studio.teleportgui.utils.config.CommandLoader;
import io.github.zct_studio.teleportgui.utils.config.ConfigKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 基岩版（Bedrock）玩家使用的 Floodgate 表单界面。
 *
 * <h2>为什么全部用反射</h2>
 * Geyser / Floodgate / Cumulus 都是<b>可选</b>依赖。如果在字段或方法签名里直接引用它们的类型，
 * 没装这些插件的服务器在加载本类时就会抛 {@link NoClassDefFoundError}，整个模组起不来。
 * 因此这里所有 API 调用都通过「字符串类名 + 反射」完成，缺失时自然走到
 * {@link #fallback}，降级为聊天栏提示或普通箱子菜单。
 *
 * <h2>表单是怎么构建的</h2>
 * Cumulus 的惯用写法是
 * {@code CustomForm.builder().title(...).dropdown(...).validResultHandler(...).build()}。
 * 这里用 {@link #newBuilder} 拿到 builder，再用 {@link #call} 按「方法名 + 参数个数」匹配并调用，
 * 回调则通过 {@link #onValid} 注入一个 {@link Consumer} 代理来接收响应对象。
 *
 * <h2>响应下标极易出错</h2>
 * CustomForm 的响应按「表单上所有组件」的顺序编号，<b>label 也占一个下标</b>。
 * 因此每插入一个 label，后面的 dropdown / input 下标都要整体后移。
 * 调整任何表单的组件顺序时，务必同步检查对应回调里的下标常量。
 *
 * <h2>{@link #FORM_SEQ} 的作用</h2>
 * 异步回调里经常需要「关闭表单 → 执行动作 → 重新打开表单」。若此时玩家已经自己关了表单
 * 或者打开了别的界面，再重新打开就会打断玩家。用一个自增序号记录「表单被打开过几次」，
 * 只有序号没变才允许重新打开，避免竞态导致的界面闪烁。
 */
@SuppressWarnings("unused")
public final class Form {
    private static final String CUMULUS_CUSTOM = "org.geysermc.cumulus.form.CustomForm";
    private static final String CUMULUS_SIMPLE = "org.geysermc.cumulus.form.SimpleForm";
    private static final String CUMULUS_MODAL = "org.geysermc.cumulus.form.ModalForm";
    private static final String CUMULUS_FORM = "org.geysermc.cumulus.form.Form";
    private static final String FLOODGATE_API = "org.geysermc.floodgate.api.FloodgateApi";
    private static final Map<UUID, Integer> FORM_SEQ = new ConcurrentHashMap<>();

    private Form() {
    }

    public static void openHomeForm(ServerPlayer player) {
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

            // CustomForm 的响应按“全部组件”的下标读取，label 也会占一个下标。
            final int entryIdx = def.isEmpty() ? 1 : 2;
            final int actionIdx = entryIdx + 1;
            onValid(b, response -> {
                int entry = intValue(response, "getDropdown", entryIdx);
                int action = intValue(response, "getDropdown", actionIdx);
                if (entry < 0 || entry >= homes.size()) return;
                String name = homes.get(entry).getName();
                switch (action) {
                    case 0 -> reopen(player, () -> Home.goHome(player, name));
                    case 1 -> afterAction(player,
                            () -> Home.deleteHome(player, name),
                            () -> openHomeForm(player));
                    case 2 -> afterAction(player,
                            () -> Home.setDefaultHome(player, name),
                            () -> openHomeForm(player));
                    case 3 -> reopen(player, () -> openRenameForm(player, name, false));
                    default -> reopen(player, () -> openCreateForm(player, false));
                }
            });
            send(player, build(b));
        } catch (Throwable t) {
            fallback(player, t);
        }
    }

    public static void openWarpForm(ServerPlayer player) {
        List<NamedLoc> warps = Manager.getWarps();
        String title = text(player, "gui.teleportgui.warp.warpgui.title");

        boolean canCreate = CommandGuard.hasPermission(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_CREATE);
        boolean canDelete = CommandGuard.hasPermission(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_DEL);
        boolean canRename = CommandGuard.hasPermission(player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_RENAME);

        if (warps.isEmpty()) {
            if (canCreate) openCreateEntryForm(player, true);
            else notice(player, title, text(player, "gui.teleportgui.warp.warpgui.empty"));
            return;
        }

        List<String> actions = new ArrayList<>();
        actions.add(text(player, "gui.teleportgui.universal.gui.tp"));
        final int deleteIndex;
        if (canDelete) {
            deleteIndex = actions.size();
            actions.add(text(player, "gui.teleportgui.universal.gui.del"));
        } else {
            deleteIndex = -1;
        }
        final int renameIndex;
        if (canRename) {
            renameIndex = actions.size();
            actions.add(text(player, "gui.teleportgui.universal.gui.rename"));
        } else {
            renameIndex = -1;
        }
        final int createIndex;
        if (canCreate) {
            createIndex = actions.size();
            actions.add(text(player, "gui.teleportgui.universal.gui.add"));
        } else {
            createIndex = -1;
        }

        try {
            Object b = newBuilder(CUMULUS_CUSTOM);
            call(b, "title", title);
            call(b, "label", text(player, "gui.teleportgui.be.summary", String.valueOf(warps.size())));
            call(b, "dropdown", text(player, "gui.teleportgui.be.selectEntry"),
                    warps.stream().map(NamedLoc::getName).toList());
            call(b, "dropdown", text(player, "gui.teleportgui.be.selectAction"), actions);

            final int entryIdx = 1;
            final int actionIdx = 2;
            onValid(b, response -> {
                int entry = intValue(response, "getDropdown", entryIdx);
                int action = intValue(response, "getDropdown", actionIdx);
                if (entry < 0 || entry >= warps.size()) return;
                String name = warps.get(entry).getName();
                if (action == 0) {
                    reopen(player, () -> Warp.goToWarp(player, name));
                } else if (action == deleteIndex) {
                    afterAction(player,
                            () -> Warp.deleteWarp(player, name),
                            () -> openWarpForm(player));
                } else if (action == renameIndex) {
                    reopen(player, () -> openRenameForm(player, name, true));
                } else if (action == createIndex) {
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
        if (warp && !CommandGuard.hasPermission(
                player, ConfigKeys.MODULE_WARP, ConfigKeys.WARP_CREATE)) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.common.noPermission", player)
                            .withStyle(Styles.ERROR_STRONG),
                    true);
            reopen(player, () -> openWarpForm(player));
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
                if (warp) openWarpForm(player);
                else openHomeForm(player);
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
                    reopen(player, () -> openWarpForm(player));
                } else {
                    Home.setHome(player, newName);
                    reopen(player, () -> openHomeForm(player));
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
                // 0 是“原名” label，输入框在 1。
                String newName = stringValue(response, "getInput", 1).trim();
                if (newName.isEmpty()) {
                    player.sendSystemMessage(
                            Tr_i18n.tr("gui.teleportgui.rename.newEmpty", player)
                                    .withStyle(Styles.ERROR),
                            true);
                    reopen(player, () -> openRenameForm(player, oldName, warp));
                } else if (warp) {
                    Warp.renameWarp(player, oldName, newName);
                    reopen(player, () -> openWarpForm(player));
                } else {
                    Home.renameHome(player, oldName, newName);
                    reopen(player, () -> openHomeForm(player));
                }
            });
            send(player, build(b));
        } catch (Throwable t) {
            fallback(player, t);
        }
    }

    public static void openTpaForm(ServerPlayer player) {
        PlayerData data = Manager.getPlayer(player.getStringUUID())
                .orElseGet(() -> Manager.addPlayer(player.getStringUUID()));
        boolean allowed = data.isRequestAllowed();
        boolean notifEnabled = data.isNotificationGuiEnabled();
        try {
            Object b = newBuilder(CUMULUS_SIMPLE);
            call(b, "title", text(player, "gui.teleportgui.tpa.tpagui.title"));
            call(b, "content", text(player, "gui.teleportgui.be.tpaSummary"));

            List<Runnable> actions = new ArrayList<>();
            call(b, "button", text(player, "gui.teleportgui.be.viewRequests"));
            actions.add(() -> reopen(player, () -> openTpaRequestsForm(player, 0)));
            call(b, "button", text(player, "gui.teleportgui.tpa.tpagui.new"));
            actions.add(() -> reopen(player, () -> openTpaRequestForm(player)));
            call(b, "button", text(player, allowed
                    ? "gui.teleportgui.tpa.tpagui.allowOn"
                    : "gui.teleportgui.tpa.tpagui.allowOff"));
            actions.add(() -> {
                data.setRequestAllowed(!allowed);
                reopen(player, () -> openTpaForm(player));
            });
            call(b, "button", text(player, notifEnabled
                    ? "gui.teleportgui.be.notifGuiOn"
                    : "gui.teleportgui.be.notifGuiOff"));
            actions.add(() -> {
                data.setNotificationGuiEnabled(!notifEnabled);
                reopen(player, () -> openTpaForm(player));
            });

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
        @SuppressWarnings("MathClampMigration") int p = Math.max(0, Math.min(page, totalPages - 1));

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
                    actions.add(() -> reopen(player, () -> openTpaRequestDialog(player, fromUuid, p, false)));
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

    public static void openTpaRequestDialog(ServerPlayer player, String fromUuid, int page) {
        openTpaRequestDialog(player, fromUuid, page, false);
    }

    public static void openTpaRequestDialog(ServerPlayer player, String fromUuid, int page,
                                            boolean autoOpened) {
        ServerPlayer from = Tpa.resolveOnline(fromUuid);
        Tpa.TpaRequest req = Tpa.findRequest(toSelfUuid(from, fromUuid), player.getStringUUID());
        try {
            Object b = newBuilder(CUMULUS_SIMPLE);
            call(b, "title", text(player, "gui.teleportgui.tpa.tpareqdialog.title"));
            call(b, "content", requestDialogContent(player, from, req));

            List<Runnable> actions = new ArrayList<>();
            call(b, "button", text(player, "gui.teleportgui.tpa.tpareqdialog.accept"));
            actions.add(() -> {
                if (from != null) Tpa.acceptChecked(from, player);
            });
            call(b, "button", text(player, "gui.teleportgui.tpa.tpareqdialog.deny"));
            actions.add(() -> {
                if (from != null) Tpa.denyChecked(from, player);
            });
            if (autoOpened) {
                call(b, "button", text(player, "gui.teleportgui.universal.gui.close"));
                actions.add(() -> {
                });
            } else {
                call(b, "button", text(player, "gui.teleportgui.universal.gui.back"));
                actions.add(() -> reopen(player, () -> openTpaRequestsForm(player, page)));
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

    private static String toSelfUuid(ServerPlayer from, String fromUuid) {
        return from == null ? fromUuid : from.getStringUUID();
    }

    private static String requestDialogContent(ServerPlayer player, ServerPlayer from,
                                               Tpa.TpaRequest req) {
        if (from == null) return text(player, "commands.teleportgui.tpa.notFound");
        String name = from.getName().getString();
        if (req == null) return name;
        return name + " " + text(player, req.here
                ? "gui.teleportgui.tpa.tpagui.here"
                : "gui.teleportgui.tpa.tpagui.tocome");
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
            List<String> targets = new ArrayList<>(names.size() + 1);
            targets.add(text(player, "gui.teleportgui.universal.gui.back"));
            targets.addAll(names);

            Object b = newBuilder(CUMULUS_CUSTOM);
            call(b, "title", title);
            call(b, "dropdown", text(player, "gui.teleportgui.tpa.tpagui.target"), targets, 0);
            call(b, "dropdown", text(player, "gui.teleportgui.tpa.tpagui.type"),
                    List.of(text(player, "gui.teleportgui.tpa.tpagui.typeTpa"),
                            text(player, "gui.teleportgui.tpa.tpagui.typeHere")));

            onValid(b, response -> {
                int selected = intValue(response, "getDropdown", 0);
                int type = intValue(response, "getDropdown", 1);
                int target = selected - 1;
                if (target >= 0 && target < names.size()) {
                    Tpa.requestByName(player, names.get(target), type == 1);
                } else {
                    reopen(player, () -> openTpaForm(player));
                }
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
            call(b, "button1", text(player, "commands.teleportgui.common.forceTeleport"));
            call(b, "button2", text(player, "gui.teleportgui.universal.gui.cancel"));

            onValid(b, response -> {
                if (intValue(response, "getClickedButtonId") == 0) onForce.run();
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
            onValid(b, response -> {
            });
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

    /**
     * 在当前服务器 tick 的末尾重新打开一个表单。
     * <p>
     * 有意延迟到 {@code server.execute} 而不是立即打开：表单回调是在「表单刚刚关闭」时
     * 触发的，此时立刻下发新表单，客户端有时会丢掉或卡住。
     * <p>
     * 打开前做两道检查：
     * <ol>
     *   <li>玩家仍然在线（下线后打开菜单会抛异常）。</li>
     *   <li>表单序号没变——如果玩家在此期间已经关掉表单或自己开了别的界面，
     *       序号会变化，此时不再打扰玩家。</li>
     * </ol>
     *
     * @param player 目标玩家
     * @param seq    发起本次刷新时记录的表单序号
     * @param opener 真正负责打开表单的动作
     */
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
        long ageSec = req.ageSeconds();
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

    /**
     * 按「方法名 + 参数个数」反射查找方法。
     * <p>
     * 不能直接 {@code getMethod(name, 具体类型...)}，因为各版本 Cumulus 的重载签名不同
     * （比如 {@code dropdown} 有带默认值和不带默认值两种，参数类型也可能是 {@code List}
     * 或其实现类）。这里退化为按名字和个数匹配，再用 {@link #accepts} 逐个检查可赋值性。
     *
     * @param type 目标类
     * @param name 方法名
     * @param args 计划传入的实参
     * @return 第一个参数都能接受的方法
     * @throws NoSuchMethodException 没有匹配的方法
     */
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

    /**
     * 判断实参能否传给该形参类型。
     * <p>
     * 反射的 {@code Method.invoke} 要求装箱类型能自动解包到基本类型，
     * 但用来「挑选方法」时 {@link Class#isInstance} 对基本类型恒为 false，
     * 所以需要单独列出装箱映射。
     *
     * @param param 形参类型
     * @param arg   实参
     * @return 是否可以安全传入
     */
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

    /**
     * 给表单 builder 注册「有效提交」回调。
     * <p>
     * Cumulus 的 {@code validResultHandler} 接受的是一系列 {@link Consumer}（分别对应
     * CustomForm / SimpleForm / ModalForm 的响应类型）。由于类型在编译期不可见，
     * 这里用一个动态代理冒充 {@link Consumer}：任何 {@code accept} 调用都会把响应交给
     * 外层传入的 {@code handler}。代理对象只在 {@code accept} 上做实际工作，
     * 其余方法（{@code equals}/{@code hashCode} 等）返回 null 即可——它们不会被调用。
     *
     * @param builder 表单 builder
     * @param handler 收到响应时执行的逻辑，参数是 Cumulus 的响应对象
     * @throws Exception 反射调用失败
     */
    private static void onValid(Object builder, Consumer<Object> handler) throws Exception {
        Object proxy = Proxy.newProxyInstance(
                Form.class.getClassLoader(),
                new Class<?>[]{Consumer.class},
                (p, method, args) -> {
                    if ("accept".equals(method.getName()) && args != null && args.length == 1) {
                        handler.accept(args[0]);
                    }
                    //noinspection SuspiciousInvocationHandlerImplementation
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

    @SuppressWarnings("SameParameterValue")
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
