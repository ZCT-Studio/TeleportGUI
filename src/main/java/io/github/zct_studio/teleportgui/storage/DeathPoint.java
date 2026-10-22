package io.github.zct_studio.teleportgui.storage;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Optional;

/**
 * 玩家最近一次死亡的位置，供 {@code /tpdeathpoint} 使用。
 * <p>
 * 数据只保存在内存中，玩家退出时由 {@code ServerMain} 调用 {@link #remove(String)} 清理，
 * 避免表随着玩家进出无限增长。
 */
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
