package io.github.zct_studio.teleportgui.storage;

import io.github.zct_studio.teleportgui.types.NamedLoc;
import io.github.zct_studio.teleportgui.types.PlayerData;
import io.github.zct_studio.teleportgui.Constants;

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
    private static String LAST_INIT_TYPE = null;

    private Manager() {
    }

    public static void init(Path saveDir) {
        init(saveDir, "json");
    }

    public static void init(Path saveDir, String type) {
        if (LAST_INIT_TYPE != null && !LAST_INIT_TYPE.equalsIgnoreCase(type)) {
            Constants.LOGGER.warn(
                    "Storage backend changed from '{}' to '{}'. Data migration is not automatic — see docs.",
                    LAST_INIT_TYPE, type);
        }

        STORAGE_FOLDER = saveDir.resolve(Constants.MOD_ID + "/");

        INITIALIZING = true;
        try {
            if ("sqlite".equalsIgnoreCase(type)) {
                provider = new Sqlite();
                STORAGE_FILE = STORAGE_FOLDER.resolve("storage.db");
            } else {
                provider = new Json();
                STORAGE_FILE = STORAGE_FOLDER.resolve("storage.json");
            }

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

            Constants.LOGGER.info("Storage initialized ({}): {} warps, {} players",
                    type, DATA.getWarps().size(), DATA.rawPlayersSize());
        } catch (Exception e) {
            Constants.LOGGER.error("Failed to initialize storage!", e);
            throw new RuntimeException("Storage initialization failed", e);
        } finally {
            INITIALIZING = false;
        }
        LAST_INIT_TYPE = type;
    }

    public static void save() {
        if (INITIALIZING) return;
        if (DATA == null || provider == null) return;

        try {
            for (NamedLoc w : DATA.getWarps()) {
                provider.saveWarp(w);
            }
            for (PlayerData p : DATA.playersSnapshot()) {
                provider.savePlayer(p);
            }
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
        PlayerData p = DATA.addPlayer(uuid);
        return p;
    }

    public static boolean addWarp(NamedLoc warp) {
        if (DATA == null) DATA = new StorageData();
        boolean exists = DATA.addWarp(warp);
        return exists;
    }

    public static void removeWarp(NamedLoc warp) {
        if (DATA != null) {
            DATA.removeWarp(warp);
        }
        if (provider != null) {
            provider.deleteWarp(warp.getName());
        }
    }

    public static class StorageData {
        private final java.util.ArrayList<NamedLoc> warps = new java.util.ArrayList<>();
        private final java.util.ArrayList<PlayerData> players = new java.util.ArrayList<>();

        public List<NamedLoc> getWarps() {
            return List.copyOf(warps);
        }

        public int rawPlayersSize() {
            return players.size();
        }

        public Optional<NamedLoc> getWarp(String name) {
            return warps.stream()
                    .filter(w -> Objects.equals(w.getName(), name))
                    .findFirst();
        }

        public Optional<PlayerData> getPlayer(String uuid) {
            return players.stream()
                    .filter(p -> Objects.equals(p.getUuid(), uuid))
                    .findFirst();
        }

        public PlayerData addPlayer(String uuid) {
            return getPlayer(uuid).orElseGet(() -> {
                PlayerData p = new PlayerData(uuid);
                players.add(p);
                save();
                return p;
            });
        }

        void addPlayerSilent(PlayerData p) {
            getPlayer(p.getUuid()).orElseGet(() -> {
                players.add(p);
                return p;
            });
        }

        public boolean addWarp(NamedLoc warp) {
            if (getWarp(warp.getName()).isPresent()) {
                return true;
            }
            warps.add(warp);
            save();
            return false;
        }

        boolean addWarpSilent(NamedLoc warp) {
            if (getWarp(warp.getName()).isPresent()) {
                return true;
            }
            warps.add(warp);
            return false;
        }

        public void removeWarp(NamedLoc warp) {
            warps.remove(warp);
            save();
        }

        List<PlayerData> playersSnapshot() {
            return List.copyOf(players);
        }
    }
}
