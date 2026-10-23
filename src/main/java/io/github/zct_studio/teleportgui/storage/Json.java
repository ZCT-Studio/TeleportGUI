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
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public class Json implements Provider {
    private static final String STORAGE_FILE_NAME = "storage.json";

    private final Gson gson = new GsonBuilder().create();
    private final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();

    private Path storageFile;
    private StorageSnapshot data = new StorageSnapshot();

    @Override
    public void init(Path dataDir) throws IOException {
        this.storageFile = dataDir.resolve(STORAGE_FILE_NAME);

        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }

        if (!Files.exists(storageFile)) {
            Files.createFile(storageFile);
        }

        if (Files.size(storageFile) == 0L) {
            this.data = new StorageSnapshot();
            write();
            return;
        }

        String json = Files.readString(storageFile, StandardCharsets.UTF_8);
        StorageSnapshot parsed = gson.fromJson(json, StorageSnapshot.class);
        if (parsed == null) {
            Constants.LOGGER.warn("Storage file was empty or malformed — starting fresh");
            this.data = new StorageSnapshot();
            return;
        }

        this.data = parsed.normalize();
        Constants.LOGGER.info("Loaded storage: {} warps, {} players",
                this.data.warps.size(), this.data.players.size());
    }

    @Override
    public List<NamedLoc> loadWarps() {
        return List.copyOf(data.warps);
    }

    @Override
    public void saveWarp(NamedLoc warp) {
        data.warps.removeIf(w -> Objects.equals(w.getName(), warp.getName()));
        data.warps.add(warp);
        write();
    }

    @Override
    public boolean deleteWarp(String name) {
        boolean removed = data.warps.removeIf(w -> Objects.equals(w.getName(), name));
        if (removed) write();
        return removed;
    }

    @Override
    public List<PlayerData> loadPlayers() {
        return List.copyOf(data.players);
    }

    @Override
    public void savePlayer(PlayerData player) {
        data.players.removeIf(p -> Objects.equals(p.getUuid(), player.getUuid()));
        data.players.add(player);
        write();
    }

    @Override
    public void saveAll(List<NamedLoc> warps, List<PlayerData> players) {
        this.data = StorageSnapshot.of(warps, players);
        write();
    }

    @Override
    public void close() {
    }

    private void write() {
        if (storageFile == null) return;
        try {
            AtomicFiles.write(storageFile, prettyGson.toJson(data));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write storage file " + storageFile, e);
        }
    }
}
