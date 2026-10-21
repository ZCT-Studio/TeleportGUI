package io.github.zct_studio.teleportgui.storage;

import io.github.zct_studio.teleportgui.types.NamedLoc;
import io.github.zct_studio.teleportgui.types.PlayerData;

import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("unused")
public interface Provider {

    void init(Path dataDir) throws Exception;

    List<NamedLoc> loadWarps();

    void saveWarp(NamedLoc warp);

    @SuppressWarnings("UnusedReturnValue")
    boolean deleteWarp(String name);

    List<PlayerData> loadPlayers();

    void savePlayer(PlayerData player);

    void close() throws Exception;
}
