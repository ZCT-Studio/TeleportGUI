package io.github.zct_studio.teleportgui.storage;

import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.types.NamedLoc;
import io.github.zct_studio.teleportgui.types.PlayerData;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Manager {

    public static Path STORAGE_FOLDER;
    public static Path STORAGE_FILE;
    public static StorageData DATA;
    public static Provider provider;

    private static boolean INITIALIZING = false;

    private Manager() {
    }

    public static void init(Path saveDir) {
        STORAGE_FOLDER = saveDir.resolve(Constants.MOD_ID + "/");

        INITIALIZING = true;
        try {
            provider = new Json();
            STORAGE_FILE = STORAGE_FOLDER.resolve("storage.json");
            provider.init(STORAGE_FOLDER);

            DATA = new StorageData();

            List<NamedLoc> loadedWarps = provider.loadWarps();
            if (loadedWarps != null) {
                for (NamedLoc w : loadedWarps) {
                    DATA.addWarpSilent(w);
                }
            }

            List<PlayerData> loadedPlayers = provider.loadPlayers();
            if (loadedPlayers != null) {
                for (PlayerData p : loadedPlayers) {
                    DATA.addPlayerSilent(p);
                }
            }

            Constants.LOGGER.info("Storage initialized (json): {} warps, {} players",
                    DATA.getWarps().size(), DATA.rawPlayersSize());
        } catch (Exception e) {
            Constants.LOGGER.error("Failed to initialize storage!", e);
            throw new RuntimeException("Storage initialization failed", e);
        } finally {
            INITIALIZING = false;
        }
    }

    /**
     * 把当前内存中的全部数据写成一份快照落盘。
     * 这是一次整体写入，不会逐条调用 provider 的单项保存方法。
     */
    public static void save() {
        if (INITIALIZING) return;
        if (DATA == null || provider == null) return;

        try {
            provider.saveAll(DATA.getWarps(), DATA.playersSnapshot());
        } catch (Exception e) {
            Constants.LOGGER.error("Failed to save storage!", e);
        }
    }

    public static void close() {
        if (provider != null) {
            try {
                provider.close();
            } catch (Exception e) {
                Constants.LOGGER.error("Failed to close storage provider", e);
            }
        }
    }

    public static List<NamedLoc> getWarps() {
        return DATA == null ? List.of() : DATA.getWarps();
    }

    public static Optional<NamedLoc> getWarp(String name) {
        return DATA == null ? Optional.empty() : DATA.getWarp(name);
    }

    public static Optional<PlayerData> getPlayer(String uuid) {
        return DATA == null ? Optional.empty() : DATA.getPlayer(uuid);
    }

    public static PlayerData addPlayer(String uuid) {
        if (DATA == null) DATA = new StorageData();
        return DATA.addPlayer(uuid);
    }

    public static boolean addWarp(NamedLoc warp) {
        if (DATA == null) DATA = new StorageData();
        return DATA.addWarp(warp);
    }

    public static void removeWarp(NamedLoc warp) {
        if (DATA != null) {
            DATA.removeWarp(warp);
        }
        if (provider != null) {
            provider.deleteWarp(warp.getName());
        }
    }

    /**
     * 运行期的数据容器：负责增删查和写盘时机。
     * 落盘结构由 {@link StorageSnapshot} 统一描述，这里不再重复声明字段模型。
     */
    public static class StorageData {
        private final StorageSnapshot snapshot = new StorageSnapshot();

        private List<NamedLoc> warps() {
            return snapshot.warps;
        }

        private List<PlayerData> players() {
            return snapshot.players;
        }

        public List<NamedLoc> getWarps() {
            return List.copyOf(warps());
        }

        public int rawPlayersSize() {
            return players().size();
        }

        public Optional<NamedLoc> getWarp(String name) {
            return warps().stream()
                    .filter(w -> Objects.equals(w.getName(), name))
                    .findFirst();
        }

        public Optional<PlayerData> getPlayer(String uuid) {
            return players().stream()
                    .filter(p -> Objects.equals(p.getUuid(), uuid))
                    .findFirst();
        }

        public PlayerData addPlayer(String uuid) {
            return getPlayer(uuid).orElseGet(() -> {
                PlayerData p = new PlayerData(uuid);
                players().add(p);
                save();
                return p;
            });
        }

        void addPlayerSilent(PlayerData p) {
            getPlayer(p.getUuid()).orElseGet(() -> {
                players().add(p);
                return p;
            });
        }

        public boolean addWarp(NamedLoc warp) {
            if (getWarp(warp.getName()).isPresent()) {
                return true;
            }
            warps().add(warp);
            save();
            return false;
        }

        @SuppressWarnings("UnusedReturnValue")
        boolean addWarpSilent(NamedLoc warp) {
            if (getWarp(warp.getName()).isPresent()) {
                return true;
            }
            warps().add(warp);
            return false;
        }

        public void removeWarp(NamedLoc warp) {
            warps().remove(warp);
            save();
        }

        List<PlayerData> playersSnapshot() {
            return List.copyOf(players());
        }
    }
}
