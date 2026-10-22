package io.github.zct_studio.teleportgui.storage;

import io.github.zct_studio.teleportgui.types.NamedLoc;
import io.github.zct_studio.teleportgui.types.PlayerData;

import java.util.ArrayList;
import java.util.List;

/**
 * storage.json 的序列化模型。
 * <p>
 * 这是整个存储层唯一的落盘结构：{@link Json} 直接用它读写信件，{@link Manager} 用它做快照。
 * 以前 Json 和 Manager 各自维护了一份同名且字段相同的内部类，改动一边很容易漏掉另一边。
 * <p>
 * 字段名即 JSON 键名，重命名会破坏已有存档，修改时请谨慎。
 */
public final class StorageSnapshot {

    /** 所有公共传送点。 */
    public List<NamedLoc> warps = new ArrayList<>();

    /** 所有玩家数据。 */
    public List<PlayerData> players = new ArrayList<>();

    /**
     * 用给定内容构建快照，并对 null 做兜底，保证序列化结果里没有 null 字段。
     */
    public static StorageSnapshot of(List<NamedLoc> warps, List<PlayerData> players) {
        StorageSnapshot snapshot = new StorageSnapshot();
        snapshot.warps = warps == null ? new ArrayList<>() : new ArrayList<>(warps);
        snapshot.players = players == null ? new ArrayList<>() : new ArrayList<>(players);
        return snapshot;
    }

    /**
     * 补齐反序列化后可能为 null 的集合字段。
     */
    StorageSnapshot normalize() {
        if (warps == null) warps = new ArrayList<>();
        if (players == null) players = new ArrayList<>();
        return this;
    }
}
