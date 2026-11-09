/*
 * Copyright (c) 2026 ZCT Studio
 * SPDX-License-Identifier: MIT
 */

package io.github.zct_studio.teleportgui;

import io.github.zct_studio.teleportgui.client.ClientMain;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeClientEntry {
    public NeoForgeClientEntry() {
        ClientMain.init();
    }
}
