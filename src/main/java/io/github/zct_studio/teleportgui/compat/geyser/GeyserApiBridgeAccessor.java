/*
 * Copyright (c) 2026 ZCT Studio
 * SPDX-License-Identifier: MIT
 */

package io.github.zct_studio.teleportgui.compat.geyser;

import net.minecraft.server.MinecraftServer;

public interface GeyserApiBridgeAccessor {
    MinecraftServer getServer();
}
