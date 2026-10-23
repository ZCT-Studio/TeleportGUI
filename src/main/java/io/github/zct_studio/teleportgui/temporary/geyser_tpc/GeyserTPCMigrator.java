package io.github.zct_studio.teleportgui.temporary.geyser_tpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.storage.Manager;
import io.github.zct_studio.teleportgui.storage.StorageSnapshot;
import io.github.zct_studio.teleportgui.types.NamedLoc;
import io.github.zct_studio.teleportgui.types.PlayerData;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 从模组前身 GeyserTPC 的存档自动迁移数据。
 *
 * <h2>这是临时功能</h2>
 * 整个 {@code temporary} 包都是过渡性代码，目的是让已经从 GeyserTPC 升级过来的服务器不丢数据。
 * 等大部分服务器完成升级后，这个包可以整包删除。
 *
 * <h2>不改动老类</h2>
 * 迁移逻辑完全位于本包内，只调用 {@link Manager} 与 {@link PlayerData} 现有的公开方法，
 * 不给老类增加任何接口。
 *
 * <h2>触发条件</h2>
 * 以下条件全部满足时才会迁移：
 * <ol>
 *   <li>新的 {@code teleportgui/storage.json} 不存在，或存在但没有任何传送点与玩家；</li>
 *   <li>老路径 {@code <世界目录>/GeyserTPC/storage.json} 存在且能解析出数据。</li>
 * </ol>
 * 只要新存档里已经有数据，就完全不碰它，避免重复迁移或覆盖玩家后来的改动。
 *
 * <h2>不删除老文件</h2>
 * 迁移只读取老存档，不移动也不删除。玩家确认无误后可以自行清理 {@code GeyserTPC/} 目录。
 *
 * <h2>写盘时机</h2>
 * 迁移在 {@code Manager.init} 完成之后执行，此时可以安全地调用 {@link Manager#save()}。
 * 迁移结束后立即落盘一次，保证下次启动时新存档非空，从而不会重复迁移。
 */
public final class GeyserTPCMigrator {

    private static final String LEGACY_FOLDER = "GeyserTPC";
    private static final String STORAGE_FILE = "storage.json";

    private static final Gson GSON = new GsonBuilder().create();

    private GeyserTPCMigrator() {
    }

    /**
     * 执行一次迁移：读取老存档、转换为新结构、并写回新存档。
     * <p>
     * 需要在 {@code Manager.init} 之后调用——那时新存档已装载完成，
     * 可以直接判断「是否为空」，也能安全地用 {@link Manager#save()} 落盘。
     *
     * @param saveDir  世界根目录
     * @param newStore 新存档文件路径
     */
    public static void run(Path saveDir, Path newStore) {
        Path legacyFile = saveDir.resolve(LEGACY_FOLDER).resolve(STORAGE_FILE);

        if (!Files.isRegularFile(legacyFile)) return;
        if (hasExistingData(newStore)) return;

        try {
            String json = Files.readString(legacyFile, StandardCharsets.UTF_8);
            if (json.isBlank()) return;

            GeyserTPCStorage legacy = GSON.fromJson(json, GeyserTPCStorage.class);
            if (legacy == null) return;

            StorageSnapshot snapshot = convert(legacy);
            if (snapshot.warps.isEmpty() && snapshot.players.isEmpty()) {
                Constants.LOGGER.info("GeyserTPC 存档为空，跳过迁移");
                return;
            }

            for (NamedLoc warp : snapshot.warps) {
                Manager.addWarp(warp);
            }
            for (PlayerData player : snapshot.players) {
                PlayerData target = Manager.addPlayer(player.getUuid());
                for (NamedLoc home : player.getHomes()) {
                    target.addHome(home);
                }
                if (!player.getDefaultHome().isBlank()) {
                    target.setDefaultHome(player.getDefaultHome());
                }
            }
            Manager.save();

            Constants.LOGGER.info("已从 GeyserTPC 迁移存档：{} 个传送点，{} 个玩家（源文件保留在 {}）",
                    snapshot.warps.size(), snapshot.players.size(), legacyFile);
        } catch (IOException e) {
            Constants.LOGGER.error("读取 GeyserTPC 存档失败，跳过迁移：{}", legacyFile, e);
        } catch (RuntimeException e) {
            Constants.LOGGER.error("GeyserTPC 存档格式无法解析，跳过迁移：{}", legacyFile, e);
        }
    }

    /**
     * 判断新存档是否已经含有数据。
     *
     * @param newStore 新存档文件路径
     * @return 已有数据时返回 true
     */
    private static boolean hasExistingData(Path newStore) {
        if (!Files.isRegularFile(newStore)) return false;
        try {
            if (Files.size(newStore) == 0L) return false;
            String json = Files.readString(newStore, StandardCharsets.UTF_8);
            if (json.isBlank()) return false;

            StorageSnapshot existing = GSON.fromJson(json, StorageSnapshot.class);
            if (existing == null) return false;
            return !existing.warps.isEmpty() || !existing.players.isEmpty();
        } catch (IOException | RuntimeException e) {
            Constants.LOGGER.warn("读取现有存档失败，将视为空存档：{}", newStore, e);
            return false;
        }
    }

    /**
     * 把老结构转换成新的 {@link StorageSnapshot}。
     *
     * @param legacy 老存档
     * @return 新结构快照
     */
    private static StorageSnapshot convert(GeyserTPCStorage legacy) {
        List<NamedLoc> warps = new ArrayList<>();
        if (legacy.Warps != null) {
            for (GeyserTPCStorage.GeyserTpcLocation loc : legacy.Warps) {
                NamedLoc warp = toNamedLoc(loc);
                if (warp != null) warps.add(warp);
            }
        }

        List<PlayerData> players = new ArrayList<>();
        if (legacy.Players != null) {
            for (GeyserTPCStorage.GeyserTpcPlayer old : legacy.Players) {
                PlayerData player = toPlayerData(old);
                if (player != null) players.add(player);
            }
        }
        return StorageSnapshot.of(warps, players);
    }

    /**
     * 单个位置点的转换；缺少名字或维度时视为无效数据丢弃。
     *
     * @param loc 老位置点
     * @return 转换结果，无效时返回 null
     */
    private static NamedLoc toNamedLoc(GeyserTPCStorage.GeyserTpcLocation loc) {
        if (loc == null || loc.name == null || loc.name.isBlank() || loc.world == null) return null;
        return new NamedLoc(loc.name, new BlockPos(loc.x, loc.y, loc.z), loc.world);
    }

    /**
     * 单个玩家的转换。
     * <p>
     * 只使用 {@link PlayerData} 现有的公开方法，不给老类增加接口。
     * 这里构造的是尚未入库的孤立对象，{@code addHome} 触发的 {@code Manager.save()}
     * 此时写入的仍是不含这些玩家的旧数据，因此由调用方在入库后统一再保存一次。
     *
     * @param old 老玩家数据
     * @return 转换结果，缺少 UUID 时返回 null
     */
    private static PlayerData toPlayerData(GeyserTPCStorage.GeyserTpcPlayer old) {
        if (old == null || old.UUID == null || old.UUID.isBlank()) return null;

        PlayerData player = new PlayerData(old.UUID);
        if (old.Homes != null) {
            for (GeyserTPCStorage.GeyserTpcLocation loc : old.Homes) {
                NamedLoc home = toNamedLoc(loc);
                if (home != null) player.addHome(home);
            }
        }
        if (old.DefaultHome != null && !old.DefaultHome.isBlank()) {
            player.setDefaultHome(old.DefaultHome);
        }
        return player;
    }
}
