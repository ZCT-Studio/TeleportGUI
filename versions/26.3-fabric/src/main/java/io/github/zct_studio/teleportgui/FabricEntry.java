package io.github.zct_studio.teleportgui;

import net.fabricmc.api.ModInitializer;

/**
 * Fabric-side entrypoint. Delegates to shared TeleportGuiMod.init().
 */
public final class FabricEntry implements ModInitializer {

    @Override
    public void onInitialize() {
        ServerMain.init();
    }
}
