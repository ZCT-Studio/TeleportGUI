package io.github.zct_studio.teleportgui.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.tree.CommandNode;
import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.ServerMain;
import io.github.zct_studio.teleportgui.utils.config.CommandLoader;
import io.github.zct_studio.teleportgui.utils.config.ConfigKeys;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public final class Registry {

    private static final LinkedHashMap<String, Group> GROUPS = new LinkedHashMap<>();
    private static final Field ROOT_CHILDREN = findField("children");
    private static final Field ROOT_LITERALS = findField("literals");

    /** 原版 {@code /reload} 的命令名，用于识别配置重载。 */
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
        if (ROOT_CHILDREN == null && hasManagedCommand(dispatcher)) {
            Constants.LOGGER.warn("Command tree access is unavailable; keeping the existing command tree");
            return;
        }
        removeManagedCommands(dispatcher);
        registerFeatureCommands(dispatcher);
        ensureGuiCommand(dispatcher);
        removeDisabledCommands(dispatcher);
    }

    public static void rebuild(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (ROOT_CHILDREN == null) {
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

    private static void removeLiteral(CommandDispatcher<CommandSourceStack> dispatcher, String literal) {
        Object root = dispatcher.getRoot();
        removeFromMap(ROOT_CHILDREN, root, literal);
        // Literal nodes are also indexed in a separate map used for suggestions.
        removeFromMap(ROOT_LITERALS, root, literal);
    }

    private static void removeFromMap(Field field, Object root, String literal) {
        if (field == null) return;
        try {
            Object value = field.get(root);
            if (value instanceof Map<?, ?> map) {
                map.remove(literal);
            }
        } catch (IllegalAccessException e) {
            Constants.LOGGER.error("Failed to remove command '{}' from the command tree", literal, e);
        }
    }

    private static void resyncCommandTree() {
        MinecraftServer server = ServerMain.SERVER;
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            server.getCommands().sendCommands(player);
        }
    }

    private static Field findField(String name) {
        try {
            Field field = CommandNode.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            Constants.LOGGER.error("Failed to access the Brigadier command tree field '{}'; "
                    + "disabling commands at runtime will not work", name, e);
            return null;
        }
    }

    public record Group(String name, List<String> literals) {
    }
}
