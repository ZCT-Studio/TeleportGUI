package io.github.zct_studio.teleportgui.storage;

import io.github.zct_studio.teleportgui.types.NamedLoc;
import io.github.zct_studio.teleportgui.types.PlayerData;

import java.util.ArrayList;
import java.util.List;

public final class StorageSnapshot {

    public List<NamedLoc> warps = new ArrayList<>();

    public List<PlayerData> players = new ArrayList<>();

    public static StorageSnapshot of(List<NamedLoc> warps, List<PlayerData> players) {
        StorageSnapshot snapshot = new StorageSnapshot();
        snapshot.warps = warps == null ? new ArrayList<>() : new ArrayList<>(warps);
        snapshot.players = players == null ? new ArrayList<>() : new ArrayList<>(players);
        return snapshot;
    }

    StorageSnapshot normalize() {
        if (warps == null) warps = new ArrayList<>();
        if (players == null) players = new ArrayList<>();
        return this;
    }
}
