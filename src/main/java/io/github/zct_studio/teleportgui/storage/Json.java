package io.github.zct_studio.teleportgui.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.zct_studio.teleportgui.types.NamedLoc;
import io.github.zct_studio.teleportgui.types.PlayerData;
import io.github.zct_studio.teleportgui.Constants;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Json implements Provider {

    private Path dataDir;
    private Path storageFile;
    private StorageData data;
    private final Gson gson = new GsonBuilder().create();

    @Override
    public void init(Path dataDir) throws Exception {
        this.dataDir = dataDir;
        this.storageFile = dataDir.resolve("storage.json");

        try {
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }

            if (!Files.exists(storageFile)) {
                Files.createFile(storageFile);
            }

            if (new File(storageFile.toString()).length() == 0) {
                this.data = new StorageData();
                write();
            } else {
                String json = Files.readString(storageFile);
                this.data = gson.fromJson(json, StorageData.class);
                if (this.data == null) {
                    Constants.LOGGER.warn("Storage file was empty or malformed — starting fresh");
                    this.data = new StorageData();
                } else {
                    Constants.LOGGER.info("Loaded storage: {} warps, {} players",
                            this.data.warps.size(), this.data.players.size());
                }
            }
        } catch (Exception e) {
            throw e;
        } catch (java.lang.Exception e) {
            throw new Exception("Failed to initialize JSON storage", e);
        }
    }

    @Override
    public List<NamedLoc> loadWarps() {
        return data == null ? List.of() : List.copyOf(data.warps);
    }

    @Override
    public void saveWarp(NamedLoc warp) {
        if (data == null) data = new StorageData();
        // Replace if name already exists, otherwise add
        data.warps.removeIf(w -> Objects.equals(w.getName(), warp.getName()));
        data.warps.add(warp);
        write();
    }

    @Override
    public boolean deleteWarp(String name) {
        if (data == null) return false;
        boolean removed = data.warps.removeIf(w -> Objects.equals(w.getName(), name));
        if (removed) write();
        return removed;
    }

    @Override
    public List<PlayerData> loadPlayers() {
        return data == null ? List.of() : List.copyOf(data.players);
    }

    @Override
    public void savePlayer(PlayerData player) {
        if (data == null) data = new StorageData();
        data.players.removeIf(p -> Objects.equals(p.getUuid(), player.getUuid()));
        data.players.add(player);
        write();
    }

    @Override
    public void close() throws Exception {
        // JSON has no persistent connections — nothing to close
    }

    private void write() {
        if (data == null || storageFile == null) return;
        try {
            Gson pretty = new GsonBuilder().setPrettyPrinting().create();
            byte[] json = pretty.toJson(data).getBytes();
            Files.write(storageFile, json,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (java.lang.Exception e) {
            throw new Exception("Failed to write storage file", e);
        }
    }

    private static class StorageData {
        ArrayList<NamedLoc> warps = new ArrayList<>();
        ArrayList<PlayerData> players = new ArrayList<>();

        Optional<NamedLoc> getWarp(String name) {
            return warps.stream()
                    .filter(w -> Objects.equals(w.getName(), name))
                    .findFirst();
        }

        Optional<PlayerData> getPlayer(String uuid) {
            return players.stream()
                    .filter(p -> Objects.equals(p.getUuid(), uuid))
                    .findFirst();
        }
    }
}
