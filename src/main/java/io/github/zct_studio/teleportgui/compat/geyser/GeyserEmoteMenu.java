/*
 * Copyright (c) 2026 ZCT Studio
 * SPDX-License-Identifier: MIT
 */

package io.github.zct_studio.teleportgui.compat.geyser;

import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.ServerMain;
import io.github.zct_studio.teleportgui.Tr_i18n;
import io.github.zct_studio.teleportgui.commands.Tpa;
import io.github.zct_studio.teleportgui.gui.Form;
import io.github.zct_studio.teleportgui.gui.GUI;
import io.github.zct_studio.teleportgui.storage.Manager;
import io.github.zct_studio.teleportgui.types.PlayerData;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("unused")
public final class GeyserEmoteMenu {

    private static final ConcurrentMap<UUID, Long> COOLDOWN_MAP = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 1500; // 1.5s冷却

    private GeyserEmoteMenu() {
    }

    public static void openMenu(ServerPlayer player) {
        UUID uuid = player.getUUID();
        long now = System.currentTimeMillis();

        Long lastOpen = COOLDOWN_MAP.get(uuid);
        if (lastOpen != null && now - lastOpen < COOLDOWN_MS) {
            return;
        }
        COOLDOWN_MAP.put(uuid, now);

        var server = ServerMain.SERVER;
        if (server == null) return;

        server.execute(() -> {
            if (server.getPlayerList().getPlayer(uuid) != player) return;

            PlayerData data = Manager.getPlayer(player.getStringUUID())
                    .orElseGet(() -> Manager.addPlayer(player.getStringUUID()));

            try {
                Class<?> simpleFormClass = Class.forName("org.geysermc.cumulus.form.SimpleForm");
                Object builder = simpleFormClass.getMethod("builder").invoke(null);
                Class<?> builderClass = builder.getClass();

                Class<?> consumerClass = Class.forName("java.util.function.Consumer");
                Object responseHandler = Proxy.newProxyInstance(
                        GeyserEmoteMenu.class.getClassLoader(),
                        new Class<?>[]{consumerClass},
                        (proxy, method, args) -> {
                            if ("accept".equals(method.getName()) && args != null && args.length == 1) {
                                server.execute(() -> {
                                    if (server.getPlayerList().getPlayer(uuid) != player) return;
                                    handleResponse(player, args[0]);
                                });
                            }
                            return null;
                        });

                builderClass.getMethod("validResultHandler", consumerClass).invoke(builder, responseHandler);

                String onText = Tr_i18n.raw("gui.teleportgui.be.emoteGUI.on", player);
                String offText = Tr_i18n.raw("gui.teleportgui.be.emoteGUI.off", player);
                String toggleText = data.isEmoteGUIEnabled() ? onText : offText;

                builderClass.getMethod("title", String.class).invoke(builder,
                        Tr_i18n.tr("gui.teleportgui.be.emoteGUI.title", player).getString());

                builderClass.getMethod("button", String.class).invoke(builder,
                        Tr_i18n.tr("gui.teleportgui.be.emoteGUI.tpa", player).getString());
                int requestCount = Tpa.getRequestsFor(player.getStringUUID()).size();
                builderClass.getMethod("button", String.class).invoke(builder,
                        Tr_i18n.tr("gui.teleportgui.be.emoteGUI.tpaRequests", player).getString()
                                + (requestCount > 0 ? " (" + requestCount + ")" : ""));
                builderClass.getMethod("button", String.class).invoke(builder,
                        Tr_i18n.tr("gui.teleportgui.be.emoteGUI.home", player).getString());
                builderClass.getMethod("button", String.class).invoke(builder,
                        Tr_i18n.tr("gui.teleportgui.be.emoteGUI.warp", player).getString());
                builderClass.getMethod("button", String.class).invoke(builder,
                        Tr_i18n.tr("gui.teleportgui.be.emoteGUI.back", player).getString());
                builderClass.getMethod("button", String.class).invoke(builder,
                        Tr_i18n.tr("gui.teleportgui.be.emoteGUI.deathLocation", player).getString());
                builderClass.getMethod("button", String.class).invoke(builder, toggleText);

                Object form = builderClass.getMethod("build").invoke(builder);

                Form.sendViaGeyser(player, form);

            } catch (Throwable e) {
                Constants.LOGGER.error("Failed to open emote trigger form for {}", player.getName().getString(), e);
            }
        });
    }

    private static void handleResponse(ServerPlayer player, Object response) {
        try {
            Method getClickedButtonId = response.getClass().getMethod("clickedButtonId");
            getClickedButtonId.setAccessible(true);
            int buttonId = (Integer) getClickedButtonId.invoke(response);

            switch (buttonId) {
                case 0 -> GUI.openTpa(player);
                case 1 -> GUI.openTpaRequests(player);
                case 2 -> GUI.openHome(player);
                case 3 -> GUI.openWarp(player);
                case 4 -> Form.openEmoteBackConfirm(player);
                case 5 -> Form.openEmoteTpDeathLocationConfirm(player);
                case 6 -> toggleEmoteGUI(player);
                default -> {}
            }
        } catch (Throwable e) {
            Constants.LOGGER.debug("Error handling emote trigger form response", e);
        }
    }

    private static void toggleEmoteGUI(ServerPlayer player) {
        PlayerData data = Manager.getPlayer(player.getStringUUID())
                .orElseGet(() -> Manager.addPlayer(player.getStringUUID()));
        boolean newState = !data.isEmoteGUIEnabled();
        data.setEmoteGUIEnabled(newState);

        openMenu(player);
    }

    public static void clearCooldown(UUID uuid) {
        COOLDOWN_MAP.remove(uuid);
    }
}
