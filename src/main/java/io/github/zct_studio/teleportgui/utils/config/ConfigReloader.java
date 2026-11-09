/*
 * Copyright (c) 2026 ZCT Studio
 * SPDX-License-Identifier: MIT
 */

package io.github.zct_studio.teleportgui.utils.config;

import io.github.zct_studio.teleportgui.ServerMain;
import io.github.zct_studio.teleportgui.commands.Registry;
import net.minecraft.server.MinecraftServer;

@SuppressWarnings("unused")
public final class ConfigReloader {

    private ConfigReloader() {
    }

    public static boolean reloadConfig() {
        if (CommandLoader.CONFIG_FILE == null) return false;
        CommandLoader.load(CommandLoader.CONFIG_FILE);
        rebuildCommandTree();
        reloadGeyserBridgeConfig();
        return true;
    }

    public static void reloadGeyserBridgeConfig() {
        if (ServerMain.SERVER == null) return;
        ServerMain.SERVER.execute(() -> {
            io.github.zct_studio.teleportgui.utils.config.ModSettingsLoader.load(ServerMain.SAVE_DIR);
            ServerMain.SERVER.execute(ServerMain::initGeyserBridge);
        });
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
