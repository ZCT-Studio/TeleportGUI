package io.github.zct_studio.teleportgui;

import io.github.zct_studio.teleportgui.client.ClientMain;
import net.fabricmc.api.ClientModInitializer;

public final class FabricClientEntry implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientMain.init();
    }
}
