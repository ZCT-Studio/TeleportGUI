package io.github.zct_studio.teleportgui.geyser;

import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.EventBus;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.ClientEmoteEvent;

import io.github.zct_studio.teleportgui.utils.TeleportUtils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public final class GeyserApiBridge {
    private static volatile GeyserApiBridge INSTANCE;
    private static volatile MinecraftServer SERVER_INSTANCE;

    private final GeyserApi api;
    private final EventBus<EventRegistrar> eventBus;

    private Object emoteSubscriber;

    private final Set<String> triggerUuids = ConcurrentHashMap.newKeySet();

    private volatile boolean registered = false;

    private GeyserApiBridge(GeyserApi api) {
        this.api = api;
        this.eventBus = api.eventBus();
    }

    public static void setServerInstance(MinecraftServer server) {
        SERVER_INSTANCE = server;
    }

    private static MinecraftServer getServerInstance() {
        return SERVER_INSTANCE;
    }

    public static GeyserApiBridge tryInit() {
        if (INSTANCE != null) return INSTANCE;

        GeyserApi api = GeyserApi.api();
        if (api == null) return null;

        GeyserApiBridge bridge = new GeyserApiBridge(api);
        INSTANCE = bridge;
        return bridge;
    }

    public static GeyserApiBridge getInstance() {
        return INSTANCE;
    }

    public static boolean isInitialized() {
        return INSTANCE != null;
    }

    public static void reset() {
        if (INSTANCE != null) {
            INSTANCE.unregisterAll();
            INSTANCE = null;
        }
    }

    public void updateConfig(List<String> triggerUuids) {
        this.triggerUuids.clear();

        if (triggerUuids != null) {
            for (String uuid : triggerUuids) {
                if (uuid != null && !uuid.isBlank()) {
                    this.triggerUuids.add(uuid.toLowerCase());
                }
            }
        }
    }

    public synchronized void registerEmoteListener() {
        if (registered) return;
        if (triggerUuids.isEmpty()) {
            return;
        }

        emoteSubscriber = eventBus.subscribe(
                org.geysermc.geyser.api.event.EventRegistrar.of(this),
                ClientEmoteEvent.class,
                this::handleEmoteEvent);
        registered = true;
    }

    @SuppressWarnings("unchecked")
    public synchronized void unregisterAll() {
        if (emoteSubscriber != null) {
            eventBus.unsubscribe((org.geysermc.geyser.api.event.EventSubscriber<org.geysermc.geyser.api.event.EventRegistrar, ? extends org.geysermc.event.Event>) emoteSubscriber);
            emoteSubscriber = null;
        }
        registered = false;
    }

    private void handleEmoteEvent(ClientEmoteEvent event) {
        if (!registered) return;

        String emoteId = event.emoteId();
        if (emoteId == null) return;

        String lowerEmoteId = emoteId.toLowerCase();

        boolean isTrigger = triggerUuids.contains(lowerEmoteId);

        if (!isTrigger) return;

        try {
            event.setCancelled(true);
        } catch (Throwable ignored) {
            // 理论上 ClientEmoteEvent 总是可取消的，但万一呢
        }

        GeyserConnection connection = event.connection();
        if (connection == null) return;

        UUID playerUuid = connection.javaUuid();
        if (playerUuid == null) return;

        // Geyser 事件可能在非主线程触发，必须使用 server.execute()
        MinecraftServer server = getServerInstance();
        if (server == null) return;

        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player == null) return;

            if (!TeleportUtils.isGeyserPlayer(player)) return;

            if (!TeleportUtils.getPlayerEmoteGUIEnabled(player)) return;

            GeyserEmoteMenu.openMenu(player);
        });
    }
}
