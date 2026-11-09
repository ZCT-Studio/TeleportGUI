/*
 * Copyright (c) 2026 ZCT Studio
 * SPDX-License-Identifier: MIT
 */

package io.github.zct_studio.teleportgui;

import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public final class NeoForgeEntry {

    public NeoForgeEntry() {
        ServerMain.init("NeoForge");
    }
}
