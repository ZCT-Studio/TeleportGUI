package io.github.zct_studio.teleportgui.types;

import io.github.zct_studio.teleportgui.ServerMain;
import io.github.zct_studio.teleportgui.storage.Manager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

@SuppressWarnings("unused")
public class NamedLoc {

    private final int x;
    private final int y;
    private final int z;
    private final String worldId;
    private String name;

    public NamedLoc(String name, BlockPos pos, String worldId) {
        this.name = name;
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.worldId = worldId;
    }

    public String getName() {
        return name;
    }

    public void setName(String newName) {
        this.name = newName;
        Manager.save();
    }

    public BlockPos getBlockPos() {
        return new BlockPos(x, y, z);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
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
}
