package io.github.zct_studio.teleportgui;

import net.neoforged.fml.common.Mod;

/**
 * NeoForge-side entrypoint. Delegates to shared ServerMain.init().
 */
@Mod(Constants.MOD_ID)
public final class NeoForgeEntry {

    public NeoForgeEntry() {
        ServerMain.init();
    }
}
