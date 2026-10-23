package io.github.zct_studio.teleportgui.storage;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Optional;

@SuppressWarnings("unused")
public final class DeathPoint {

    private static final HashMap<String, io.github.zct_studio.teleportgui.types.DeathPoint> deathLocations = new HashMap<>();

    private DeathPoint() {
    }

    public static Optional<io.github.zct_studio.teleportgui.types.DeathPoint> get(String uuid) {
        return Optional.ofNullable(deathLocations.get(uuid));
    }

    public static void set(String uuid, BlockPos pos, String worldId) {
        deathLocations.put(uuid, new io.github.zct_studio.teleportgui.types.DeathPoint(pos, worldId));
    }

    public static void clear() {
        deathLocations.clear();
    }

    public static void remove(String uuid) {
        deathLocations.remove(uuid);
    }
}
