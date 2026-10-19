package io.github.zct_studio.teleportgui;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import io.github.zct_studio.teleportgui.commands.Home;
import io.github.zct_studio.teleportgui.commands.Tpa;
import io.github.zct_studio.teleportgui.commands.GUI;
import io.github.zct_studio.teleportgui.commands.TpPoints;
import io.github.zct_studio.teleportgui.commands.Warp;
import io.github.zct_studio.teleportgui.storage.DeathPoint;
import io.github.zct_studio.teleportgui.storage.Manager;
import io.github.zct_studio.teleportgui.utils.config.CommandLoader;
import io.github.zct_studio.teleportgui.utils.config.ModSettingsLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

public final class ServerMain {

    public static MinecraftServer SERVER;
    public static Path SAVE_DIR;

    private ServerMain() {}

    public static void init(String Loader) {
        Constants.LOGGER.info("Initializing {} v{}, on {}", Constants.MOD_NAME, Constants.VERSION, Loader);

        boolean geyserLoaded = isClassPresent("org.geysermc.geyser.api.GeyserApi");
        boolean floodgateLoaded = isClassPresent("org.geysermc.floodgate.api.FloodgateApi");
        if (geyserLoaded && !floodgateLoaded) {
            Constants.LOGGER.warn("Geyser present but Floodgate missing — Bedrock forms unavailable");
        }

        LifecycleEvent.SERVER_STARTED.register(server -> {
            SERVER = server;
            SAVE_DIR = server.getWorldPath(LevelResource.ROOT);
            Path dataDir = SAVE_DIR.resolve(Constants.MOD_ID);
            ModSettingsLoader.load(dataDir.resolve("settings.yml"));
            CommandLoader.load(dataDir.resolve("config.json"));
            String storageType = ModSettingsLoader.SETTINGS.storage.type;
            Constants.LOGGER.info("Initializing storage backend: {}", storageType);
            Manager.init(SAVE_DIR, storageType);
        });

        CommandRegistrationEvent.EVENT.register((dispatcher, ra, env) -> {
            Home.register(dispatcher);
            Warp.register(dispatcher);
            Tpa.register(dispatcher);
            TpPoints.register(dispatcher);
            GUI.register(dispatcher);
        });

        EntityEvent.LIVING_DEATH.register((LivingEntity entity, DamageSource source) -> {
            if (entity instanceof ServerPlayer sp) {
                DeathPoint.set(
                        sp.getStringUUID(),
                        sp.blockPosition(),
                        sp.level().dimension().identifier().toString()
                );
            }
            return EventResult.pass();
        });

        LifecycleEvent.SERVER_STOPPING.register(server -> {
            try { Manager.close(); }
            catch (Exception e) { Constants.LOGGER.error("Error closing storage", e); }
        });
    }

    private static boolean isClassPresent(String className) {
        try { Class.forName(className); return true; }
        catch (ClassNotFoundException e) { return false; }
    }
}
