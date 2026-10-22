package io.github.zct_studio.teleportgui.storage;

import io.github.zct_studio.teleportgui.types.NamedLoc;
import io.github.zct_studio.teleportgui.types.PlayerData;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("unused")
public interface Provider {

    void init(Path dataDir) throws IOException;

    List<NamedLoc> loadWarps();

    void saveWarp(NamedLoc warp);

    @SuppressWarnings("UnusedReturnValue")
    boolean deleteWarp(String name);

    List<PlayerData> loadPlayers();

    void savePlayer(PlayerData player);

    /**
     * 用给定快照整体覆盖存储内容，只落盘一次。
     * 批量保存必须走这里：逐条调用 {@link #saveWarp}/{@link #savePlayer} 会让每次调用都写一遍
     * 整个存储文件。
     */
    void saveAll(List<NamedLoc> warps, List<PlayerData> players);

    void close();
}
