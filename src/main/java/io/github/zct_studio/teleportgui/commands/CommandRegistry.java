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

/**
 * Owns the mod's top-level commands and can rebuild the command tree at runtime.
 * <p>
 * Brigadier has no public API for removing a root child, so the root's private
 * {@code children} and {@code literals} maps are accessed reflectively. These
 * field names are part of the Brigadier library (not Minecraft), so they are
 * stable across game versions.
 */
public final class CommandRegistry {

    private CommandRegistry() {}

    public record Group(String name, List<String> literals) {}

    private static final LinkedHashMap<String, Group> GROUPS = new LinkedHashMap<>();

    static {
        group(ConfigKeys.COMMAND_HOME, "home", "ghome");
        group(ConfigKeys.COMMAND_WARP, "warp", "gwarp");
        group(ConfigKeys.COMMAND_TPA, "tpa", "tpahere", "tpaaccept", "tpaY", "tpadeny", "tpaN", "gtpa");
        group(ConfigKeys.COMMAND_BACK, "back");
        group(ConfigKeys.COMMAND_DEATH_POINT, "tpdeathpoint", "tpdp");
        group(ConfigKeys.COMMAND_WORLD_SPAWN, "tpworldspawn", "tpws");
        group(ConfigKeys.COMMAND_SPAWN_POINT, "tpspawnpoint", "tpsp");
        group(ConfigKeys.COMMAND_FORCE, "tpforce");
        group(ConfigKeys.COMMAND_RTP, "rtp");
    }

    private static void group(String name, String... literals) {
        GROUPS.put(name, new Group(name, List.of(literals)));
    }

    private static final Field ROOT_CHILDREN = findField("children");
    private static final Field ROOT_LITERALS = findField("literals");

    /** Registers all feature commands, then removes the disabled ones. */
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

    /** Rebuilds the command tree from the current config and refreshes all clients. */
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

    /**
     * Resolves a config group name or any command literal (including aliases) to
     * its group name. For example, both {@code tpdeathpoint} and {@code tpdp}
     * resolve to {@code tpdeathpoint}.
     */
    public static String groupNameOf(String name) {
        if (GROUPS.containsKey(name)) return name;
        for (Group group : GROUPS.values()) {
            if (group.literals().contains(name)) return group.name();
        }
        return null;
    }

    public static boolean isReloadCommand(ParseResults<CommandSourceStack> results) {
        var nodes = results.getContext().getNodes();
        if (!nodes.isEmpty()) {
            String name = nodes.get(0).getNode().getName();
            if ("reload".equals(name) || "minecraft:reload".equals(name)) return true;
        }
        String input = results.getReader().getString().trim();
        return "reload".equals(input)
                || input.startsWith("reload ")
                || "minecraft:reload".equals(input)
                || input.startsWith("minecraft:reload ");
    }

    private static void registerFeatureCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        Home.register(dispatcher);
        Warp.register(dispatcher);
        Tpa.register(dispatcher);
        TpPoints.register(dispatcher);
        Rtp.register(dispatcher);
    }

    private static void ensureGuiCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (dispatcher.getRoot().getChild("teleportgui") == null) {
            GUI.register(dispatcher);
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
}
