package io.github.zct_studio.teleportgui;

import io.github.zct_studio.teleportgui.client.ClientKeys;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeClientEntry {
    public NeoForgeClientEntry() {
        ClientKeys.init();
    }
}
