package io.github.zct_studio.teleportgui;

import io.github.zct_studio.teleportgui.platform.ModVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Constants {

    public static final String MOD_ID = "teleportgui";
    public static final String MOD_NAME = "TeleportGUI";
    public static final String VERSION = ModVersion.get();

    public static final String CLASS_GEYSER_API = "org.geysermc.geyser.api.GeyserApi";
    public static final String CLASS_FLOODGATE_API = "org.geysermc.floodgate.api.FloodgateApi";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private Constants() {
    }
}
