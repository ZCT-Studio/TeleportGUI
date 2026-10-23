package io.github.zct_studio.teleportgui;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.CommandPerformEvent;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import io.github.zct_studio.teleportgui.commands.Registry;
import io.github.zct_studio.teleportgui.commands.Tpa;
import io.github.zct_studio.teleportgui.storage.DeathPoint;
import io.github.zct_studio.teleportgui.storage.Manager;
import io.github.zct_studio.teleportgui.storage.RuntimeStore;
import io.github.zct_studio.teleportgui.utils.CommandGuard;
import io.github.zct_studio.teleportgui.utils.TeleportUtils;
import io.github.zct_studio.teleportgui.utils.config.CommandLoader;
import io.github.zct_studio.teleportgui.utils.config.ConfigReloader;
import io.github.zct_studio.teleportgui.utils.config.ModSettingsLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("unused")
public final class ServerMain {

    public static MinecraftServer SERVER;
    public static Path SAVE_DIR;

    private ServerMain() {
    }

    public static void init(String Loader) {
        Constants.LOGGER.info("Initializing {} v{}, on {}", Constants.MOD_NAME, Constants.VERSION, Loader);

        boolean geyserLoaded = isClassPresent(Constants.CLASS_GEYSER_API);
        boolean floodgateLoaded = isClassPresent(Constants.CLASS_FLOODGATE_API);
        if (geyserLoaded && !floodgateLoaded) {
            Constants.LOGGER.warn("Geyser present but Floodgate missing — Bedrock forms unavailable");
        }

        LifecycleEvent.SERVER_STARTED.register(server -> {
            SERVER = server;
            SAVE_DIR = server.getWorldPath(LevelResource.ROOT);
            Path dataDir = SAVE_DIR.resolve(Constants.MOD_ID);
            ModSettingsLoader.load(dataDir.resolve("settings.yml"));
            CommandLoader.load(dataDir.resolve("config.json"));
            Manager.init(SAVE_DIR);
            boolean runtimePersistence = ModSettingsLoader.SETTINGS.runtimePersistence;
            RuntimeStore.setEnabled(runtimePersistence);
            long now = server.getTickCount();
            if (runtimePersistence) {
                RuntimeStore.load(dataDir.resolve("runtime.json"));
                CommandGuard.loadPersisted(RuntimeStore.data().cooldowns, now);
                Tpa.loadPersisted(RuntimeStore.data().tpa, now);
            } else {
                CommandGuard.loadPersisted(List.of(), now);
                Tpa.loadPersisted(List.of(), now);
            }
            Registry.rebuild(server.getCommands().getDispatcher());
        });

        CommandRegistrationEvent.EVENT.register((dispatcher, ra, env) ->
                Registry.register(dispatcher));

        CommandPerformEvent.EVENT.register(event -> {
            if (Registry.isReloadCommand(event.getResults())) {
                CommandSourceStack source = event.getResults().getContext().getSource();
                if (source != null && source.permissions().hasPermission(
                        new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))) {
                    MinecraftServer server = SERVER;
                    if (server != null) {
                        server.execute(ConfigReloader::reloadConfig);
                    }
                }
            }
            return EventResult.pass();
        });

        TickEvent.SERVER_POST.register(server -> {
            if (server.getTickCount() % 1200 != 0) return;
            long now = server.getTickCount();
            CommandGuard.cleanup(now);
            Tpa.cleanup(now);
            if (RuntimeStore.isDirty()) {
                RuntimeStore.save(CommandGuard.snapshot(now), Tpa.snapshot(now));
            }
        });

        EntityEvent.LIVING_DEATH.register((LivingEntity entity, DamageSource source) -> {
            if (entity instanceof ServerPlayer sp) {
                DeathPoint.set(
                        sp.getStringUUID(),
                        TeleportUtils.getTeleportPos(sp),
                        sp.level().dimension().identifier().toString()
                );
            }
            return EventResult.pass();
        });

        PlayerEvent.PLAYER_QUIT.register(player -> {
            String uuid = player.getStringUUID();
            TeleportUtils.clearPreviousPos(uuid);
            DeathPoint.remove(uuid);
        });

        LifecycleEvent.SERVER_STOPPING.register(server -> {
            long now = server.getTickCount();
            RuntimeStore.save(CommandGuard.snapshot(now), Tpa.snapshot(now));
            try {
                Manager.close();
            } catch (Exception e) {
                Constants.LOGGER.error("Error closing storage", e);
            }
        });
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
