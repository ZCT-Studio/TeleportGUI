/*
 * Copyright (c) 2026 ZCT Studio
 * SPDX-License-Identifier: MIT
 */

package io.github.zct_studio.teleportgui;

import net.fabricmc.api.ModInitializer;

public final class FabricEntry implements ModInitializer {

    @Override
    public void onInitialize() {
        ServerMain.init("Fabric");
    }
}
