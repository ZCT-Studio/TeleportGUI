package io.github.zct_studio.teleportgui.platform;

import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class ConfigDir {
    private ConfigDir() {
    }

    public static Path get() {
        return FMLPaths.CONFIGDIR.get();
    }
}
