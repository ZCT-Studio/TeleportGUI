package io.github.zct_studio.teleportgui.platform;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class ConfigDir {
    private ConfigDir() {
    }

    public static Path get() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
