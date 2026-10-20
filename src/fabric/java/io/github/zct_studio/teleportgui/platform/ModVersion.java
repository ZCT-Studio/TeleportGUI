package io.github.zct_studio.teleportgui.platform;

import io.github.zct_studio.teleportgui.Constants;
import net.fabricmc.loader.api.FabricLoader;

public final class ModVersion {
    private ModVersion() {
    }

    public static String get() {
        return FabricLoader.getInstance()
                .getModContainer(Constants.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown version");
    }
}
