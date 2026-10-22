package io.github.zct_studio.teleportgui.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.types.NamedLoc;
import io.github.zct_studio.teleportgui.types.PlayerData;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public class Json implements Provider {
    private final Gson gson = new GsonBuilder().create();
    @SuppressWarnings("FieldCanBeLocal")
    private Path dataDir;
    private Path storageFile;
    private StorageData data;

    @Override
    public void init(Path dataDir) throws IOException {
        this.dataDir = dataDir;
        this.storageFile = dataDir.resolve("storage.json");

        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }

        if (!Files.exists(storageFile)) {
            Files.createFile(storageFile);
        }

        if (Files.size(storageFile) == 0L) {
            this.data = new StorageData();
            write();
            return;
        }

        String json = Files.readString(storageFile);
        this.data = gson.fromJson(json, StorageData.class);
        if (this.data == null) {
            Constants.LOGGER.warn("Storage file was empty or malformed — starting fresh");
            this.data = new StorageData();
        } else {
            if (this.data.warps == null) this.data.warps = new ArrayList<>();
            if (this.data.players == null) this.data.players = new ArrayList<>();
            Constants.LOGGER.info("Loaded storage: {} warps, {} players",
                    this.data.warps.size(), this.data.players.size());
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
    public void saveAll(List<NamedLoc> warps, List<PlayerData> players) {
        StorageData snapshot = new StorageData();
        if (warps != null) snapshot.warps.addAll(warps);
        if (players != null) snapshot.players.addAll(players);
        this.data = snapshot;
        write();
    }

    @Override
    public void close() {
        // JSON 没有持久连接，无需关闭
    }

    private void write() {
        if (data == null || storageFile == null) return;
        try {
            Gson pretty = new GsonBuilder().setPrettyPrinting().create();
            byte[] json = pretty.toJson(data).getBytes(StandardCharsets.UTF_8);
            Files.write(storageFile, json,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write storage file " + storageFile, e);
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
