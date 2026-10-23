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

@SuppressWarnings("unused")
public final class Registry {

    private static final LinkedHashMap<String, Group> GROUPS = new LinkedHashMap<>();

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

    public static boolean isReloadCommand(ParseResults<CommandSourceStack> results) {
        var nodes = results.getContext().getNodes();
        if (!nodes.isEmpty()) {
            return RELOAD_LITERAL.equals(nodes.getFirst().getNode().getName());
        }
        return startsWithCommand(results.getReader().getString(), RELOAD_LITERAL);
    }

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
