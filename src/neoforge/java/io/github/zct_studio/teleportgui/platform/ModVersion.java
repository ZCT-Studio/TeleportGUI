package io.github.zct_studio.teleportgui.platform;

import io.github.zct_studio.teleportgui.Constants;
import net.neoforged.fml.ModList;

public final class ModVersion {
    private ModVersion() {
    }

    public static String get() {
        ModList modList = ModList.get();
        if (modList == null) {
            return "unknown version";
        }
        return modList.getModContainerById(Constants.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown version");
    }
}
