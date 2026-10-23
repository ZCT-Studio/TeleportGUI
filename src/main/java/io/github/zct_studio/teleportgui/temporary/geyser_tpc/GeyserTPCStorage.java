package io.github.zct_studio.teleportgui.temporary.geyser_tpc;

import java.util.ArrayList;
import java.util.List;

/**
 * GeyserTPC（模组前身）的 storage.json 结构，仅用于反序列化。
 *
 * <h2>为什么字段名是首字母大写</h2>
 * 老模组直接用 Java 字段名当作 JSON 键，例如 {@code Warps}、{@code Players}。
 * 这里的字段名必须与老文件逐字一致，否则读不出来。不要「顺手规范化」成小写。
 *
 * <h2>老结构与新结构的差异</h2>
 * <pre>
 * 老结构                        新结构
 * Warps                 -&gt;     warps
 * Players               -&gt;     players
 *   UUID                -&gt;       uuid
 *   DefaultHome         -&gt;       defaultHome
 *   Homes               -&gt;       homes
 *   位置的 world         -&gt;       位置的 worldId
 * </pre>
 * 老模组没有 {@code notificationGui} / {@code allowRequest}，迁移后沿用新结构的默认值。
 */
final class GeyserTPCStorage {

    /** 全局传送点列表。老文件的键是 {@code Warps}。 */
    List<GeyserTpcLocation> Warps = new ArrayList<>();

    /** 玩家数据列表。老文件的键是 {@code Players}。 */
    List<GeyserTpcPlayer> Players = new ArrayList<>();

    /**
     * 老模组中的一个坐标点（传送点或家园）。
     */
    static final class GeyserTpcLocation {
        String name;
        int x;
        int y;
        int z;
        /** 维度 id，如 {@code minecraft:overworld}；新结构里叫 {@code worldId}。 */
        String world;
    }

    /**
     * 老模组中的玩家数据。
     */
    static final class GeyserTpcPlayer {
        /** 玩家 UUID；新结构里叫 {@code uuid}。 */
        String UUID;
        /** 默认家园名；新结构里叫 {@code defaultHome}。 */
        String DefaultHome = "";
        /** 家园列表；新结构里叫 {@code homes}。 */
        List<GeyserTpcLocation> Homes = new ArrayList<>();
    }
}
