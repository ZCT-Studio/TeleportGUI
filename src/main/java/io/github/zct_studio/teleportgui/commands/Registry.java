package io.github.zct_studio.teleportgui.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.ServerMain;
import io.github.zct_studio.teleportgui.compat.brigadier.BrigadierAccess;
import io.github.zct_studio.teleportgui.utils.config.CommandLoader;
import io.github.zct_studio.teleportgui.utils.config.ConfigKeys;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * 命令注册与「运行期增删命令」的管理器。
 *
 * <h2>为什么要用反射动 Brigadier 的内部结构</h2>
 * Brigadier 的 {@link CommandNode} 只提供 {@code addChild}，<b>没有</b>任何公开的
 * 「移除子节点」API。但本模组需要支持 {@code /teleportgui_admin command home false}
 * 之后立刻让 {@code /home} 从补全和命令分发里消失，因此只能反射拿到底层的两个 map：
 * <ul>
 *   <li>{@code children}：{@code String -> CommandNode}，真正参与命令分发与补全。</li>
 *   <li>{@code literals}：{@code String -> LiteralCommandNode}，供补全加速使用。</li>
 * </ul>
 * 两者必须同时删除，只删一个会出现「命令不能用但补全还列着」或反过来的不一致状态。
 *
 * <h2>反射失败时的降级</h2>
 * 若 Brigadier 改了内部字段名，{@link #ROOT_CHILDREN} 会是 null。此时不抛异常，
 * 只是退化成「启动时注册好命令之后不再依赖运行期增删」，模组仍可正常使用，
 * 代价是配置里的开关需要重启服务器才生效。
 *
 * <h2>{@link #GROUPS} 的意义</h2>
 * 一个功能往往有多个别名（{@code home} / {@code ghome}），而配置文件里的开关是按「功能」
 * 而不是按「别名」记录的。GROUPS 维护「功能名 → 全部别名」的映射，
 * 使得启用/禁用某个功能时能一次性处理它的所有别名。
 */
@SuppressWarnings("unused")
public final class Registry {

    private static final LinkedHashMap<String, Group> GROUPS = new LinkedHashMap<>();

    /**
     * 命令树的内部访问器。所有与 Brigadier 内部结构强相关的代码都在
     * {@code compat.brigadier} 包里，这里只依赖抽象的 {@link BrigadierAccess}。
     */
    private static final BrigadierAccess BRIGADIER = BrigadierAccess.create();

    private static final String RELOAD_LITERAL = "reload";

    static {
        group(ConfigKeys.COMMAND_HOME, "home", "ghome");
        group(ConfigKeys.COMMAND_WARP, "warp", "gwarp");
        group(ConfigKeys.COMMAND_TPA, "tpa", "tpahere", "tpaaccept", "tpay", "tpadeny", "tpan", "gtpa");
        group(ConfigKeys.COMMAND_BACK, "back");
        group(ConfigKeys.COMMAND_DEATH_POINT, "tpdeathpoint", "tpdp");
        group(ConfigKeys.COMMAND_WORLD_SPAWN, "tpworldspawn", "tpws");
        group(ConfigKeys.COMMAND_SPAWN_POINT, "tpspawnpoint", "tpsp");
        group(ConfigKeys.COMMAND_FORCE, "tpforce");
        group(ConfigKeys.COMMAND_RANDOM_TP, "randomtp", "rtp");
    }

    private Registry() {
    }

    private static void group(String name, String... literals) {
        GROUPS.put(name, new Group(name, List.of(literals)));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!BRIGADIER.isAvailable() && hasManagedCommand(dispatcher)) {
            Constants.LOGGER.warn("Command tree access is unavailable; keeping the existing command tree");
            return;
        }
        removeManagedCommands(dispatcher);
        registerFeatureCommands(dispatcher);
        ensureGuiCommand(dispatcher);
        removeDisabledCommands(dispatcher);
    }

    public static void rebuild(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!BRIGADIER.isAvailable()) {
            Constants.LOGGER.warn("Command tree access is unavailable; skipping command tree rebuild");
            return;
        }
        removeManagedCommands(dispatcher);
        registerFeatureCommands(dispatcher);
        ensureGuiCommand(dispatcher);
        removeDisabledCommands(dispatcher);
        resyncCommandTree();
    }

    public static List<String> groupNames() {
        return List.copyOf(GROUPS.keySet());
    }

    public static boolean isKnownGroup(String name) {
        return groupNameOf(name) != null;
    }

    public static String groupNameOf(String name) {
        if (GROUPS.containsKey(name)) return name;
        for (Group group : GROUPS.values()) {
            if (group.literals().contains(name)) return group.name();
        }
        return null;
    }

    /**
     * 判断这次执行的是不是原版的 {@code /reload}。
     * <p>
     * 优先看解析结果里第一个节点是不是 reload（Brigadier 已把 {@code minecraft:reload}
     * 归一化到同一个节点）。只有在没有解析出任何节点（例如命令不完整）时才退回到
     * 文本匹配，并且用「命令名 + 空白/结尾」做严格边界，避免把 {@code reloadfoo}
     * 之类的前缀相同的命令误判成 reload。
     */
    public static boolean isReloadCommand(ParseResults<CommandSourceStack> results) {
        var nodes = results.getContext().getNodes();
        if (!nodes.isEmpty()) {
            return RELOAD_LITERAL.equals(nodes.getFirst().getNode().getName());
        }
        return startsWithCommand(results.getReader().getString(), RELOAD_LITERAL);
    }

    /**
     * 判断 {@code input}（可能带前导 {@code /}）是否以命令 {@code command} 开头，
     * 且其后紧跟空白或字符串结尾。
     */
    private static boolean startsWithCommand(String input, String command) {
        if (input == null) return false;
        int start = 0;
        while (start < input.length() && Character.isWhitespace(input.charAt(start))) {
            start++;
        }
        if (start < input.length() && input.charAt(start) == '/') {
            start++;
        }
        int end = start + command.length();
        if (end > input.length()) return false;
        if (!input.regionMatches(start, command, 0, command.length())) return false;
        return end == input.length() || Character.isWhitespace(input.charAt(end));
    }

    private static void registerFeatureCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 每个模块的 register 都无条件调用 dispatcher.register；Brigadier 遇到同名节点会抛出
        // IllegalArgumentException（"Command name ... is already defined"）。当反射移除逻辑不可用、
        // 或注册流程被重复触发时，重复注册会让服务端起不来，所以先按组做一次幂等判断：
        // 组内只要还有一个别名留在命令树上，就认为该组已注册，整组跳过。
        for (Group group : GROUPS.values()) {
            if (isGroupRegistered(dispatcher, group)) continue;
            registerGroup(dispatcher, group.name());
        }
    }

    private static boolean isGroupRegistered(CommandDispatcher<CommandSourceStack> dispatcher,
                                             Group group) {
        for (String literal : group.literals()) {
            if (dispatcher.getRoot().getChild(literal) != null) {
                return true;
            }
        }
        return false;
    }

    private static void registerGroup(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        switch (name) {
            case ConfigKeys.COMMAND_HOME -> Home.register(dispatcher);
            case ConfigKeys.COMMAND_WARP -> Warp.register(dispatcher);
            case ConfigKeys.COMMAND_TPA -> Tpa.register(dispatcher);
            case ConfigKeys.COMMAND_RANDOM_TP -> RandomTp.register(dispatcher);
            default -> TpPoints.register(dispatcher);
        }
    }

    private static void ensureGuiCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (dispatcher.getRoot().getChild(ConfigKeys.TG_PLAYER) == null) {
            GUI.registerPlayer(dispatcher);
        }
        if (dispatcher.getRoot().getChild(ConfigKeys.TG_ADMIN) == null) {
            GUI.registerAdmin(dispatcher);
        }
        if (dispatcher.getRoot().getChild(ConfigKeys.TG_BE_ROOT) == null) {
            GUI.registerBe(dispatcher);
        }
    }

    private static void removeManagedCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (Group group : GROUPS.values()) {
            for (String literal : group.literals()) {
                removeLiteral(dispatcher, literal);
            }
        }
    }

    private static boolean hasManagedCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (Group group : GROUPS.values()) {
            for (String literal : group.literals()) {
                if (dispatcher.getRoot().getChild(literal) != null) return true;
            }
        }
        return false;
    }

    private static void removeDisabledCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (Group group : GROUPS.values()) {
            if (CommandLoader.isCommandEnabled(group.name())) continue;
            for (String literal : group.literals()) {
                removeLiteral(dispatcher, literal);
            }
        }
    }

    /**
     * 从命令树上移除一个字面量子命令。
     * <p>
     * 具体怎么摸到 Brigadier 的内部 map 由 {@link BrigadierAccess} 决定，
     * 这个类不再直接操作反射，升级 Brigadier 时只需改 {@code compat.brigadier} 包。
     */
    private static void removeLiteral(CommandDispatcher<CommandSourceStack> dispatcher, String literal) {
        BRIGADIER.removeChild(dispatcher.getRoot(), literal);
    }

    private static void resyncCommandTree() {
        MinecraftServer server = ServerMain.SERVER;
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            server.getCommands().sendCommands(player);
        }
    }

    public record Group(String name, List<String> literals) {
    }
}
