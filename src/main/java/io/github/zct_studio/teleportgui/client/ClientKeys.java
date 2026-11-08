package io.github.zct_studio.teleportgui.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class ClientKeys {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("teleportgui", "shortcuts"));
    private static final KeyMapping[] KEYS = {
            key("tpa", InputConstants.KEY_BACKSLASH),
            key("tpaRequest", InputConstants.KEY_SEMICOLON),
            key("home", InputConstants.KEY_HOME),
            key("warp", InputConstants.UNKNOWN.getValue()),
            key("back", InputConstants.UNKNOWN.getValue()),
            key("deathLocation", InputConstants.UNKNOWN.getValue())
    };
    private static final String[] COMMANDS = {"gtpa", "gtpa req", "ghome", "gwarp", "back", "tpdeathpoint"};

    private ClientKeys() {}

    private static KeyMapping key(String name, int key) {
        return new KeyMapping("key.teleportgui." + name, key, CATEGORY);
    }

    public static void init() {
        for (KeyMapping key : KEYS) KeyMappingRegistry.register(key);
        ClientTickEvent.CLIENT_POST.register(client -> {
            for (int i = 0; i < KEYS.length; i++) {
                while (KEYS[i].consumeClick()) {
                    if (client.player != null && client.getConnection() != null) {
                        client.getConnection().sendCommand(COMMANDS[i]);
                    }
                }
            }
        });
    }
}
