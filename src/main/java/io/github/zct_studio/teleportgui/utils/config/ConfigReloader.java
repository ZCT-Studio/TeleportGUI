package io.github.zct_studio.teleportgui.utils.config;

import io.github.zct_studio.teleportgui.ServerMain;
import io.github.zct_studio.teleportgui.commands.CommandRegistry;
import net.minecraft.server.MinecraftServer;

/**
 * Reloads {@code config.json} and rebuilds the command tree without restarting
 * the server. Used by {@code /teleportgui reload ...} and by vanilla
 * {@code /reload}.
 */
public final class ConfigReloader {

    private ConfigReloader() {}

    /** Reloads config.json, then rebuilds the command tree. */
    public static boolean reloadConfig() {
        if (CommandLoader.CONFIG_FILE == null) return false;
        CommandLoader.load(CommandLoader.CONFIG_FILE);
        rebuildCommandTree();
        return true;
    }

    /** Re-applies the current in-memory command toggles and rebuilds the tree. */
    public static void reloadCommandTree() {
        rebuildCommandTree();
    }

    private static void rebuildCommandTree() {
        MinecraftServer server = ServerMain.SERVER;
        if (server == null) return;
        CommandRegistry.rebuild(server.getCommands().getDispatcher());
    }
}
