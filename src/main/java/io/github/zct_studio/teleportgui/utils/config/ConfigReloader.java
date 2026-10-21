package io.github.zct_studio.teleportgui.utils.config;

import io.github.zct_studio.teleportgui.ServerMain;
import io.github.zct_studio.teleportgui.commands.Registry;
import net.minecraft.server.MinecraftServer;

public final class ConfigReloader {

    private ConfigReloader() {}

    public static boolean reloadConfig() {
        if (CommandLoader.CONFIG_FILE == null) return false;
        CommandLoader.load(CommandLoader.CONFIG_FILE);
        rebuildCommandTree();
        return true;
    }

    public static void reloadCommandTree() {
        rebuildCommandTree();
    }

    private static void rebuildCommandTree() {
        MinecraftServer server = ServerMain.SERVER;
        if (server == null) return;
        Registry.rebuild(server.getCommands().getDispatcher());
    }
}
