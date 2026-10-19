package io.github.zct_studio.teleportgui;

import net.fabricmc.api.ModInitializer;

public final class FabricEntry implements ModInitializer {

    @Override
    public void onInitialize() {
        ServerMain.init();
    }
}
