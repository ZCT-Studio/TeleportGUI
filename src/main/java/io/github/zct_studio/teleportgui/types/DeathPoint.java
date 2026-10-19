package io.github.zct_studio.teleportgui.types;

import io.github.zct_studio.teleportgui.ServerMain;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class DeathPoint {

    private BlockPos pos;
    private String worldId;

    public DeathPoint(BlockPos pos, String worldId) {
        this.pos = pos;
        this.worldId = worldId;
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    public String getWorldId() {
        return worldId;
    }

    public Optional<ServerLevel> getWorld() {
        var server = ServerMain.SERVER;
        if (server == null) return Optional.empty();
        return StreamSupport.stream(server.getAllLevels().spliterator(), false)
                .filter(level -> Objects.equals(level.dimension().identifier().toString(), worldId))
                .findFirst();
    }

    public void setBlockPos(BlockPos pos) {
        this.pos = pos;
    }

    public void setWorldId(String worldId) {
        this.worldId = worldId;
    }
}
