package io.github.zct_studio.teleportgui.storage;

import io.github.zct_studio.teleportgui.types.NamedLoc;
import io.github.zct_studio.teleportgui.types.PlayerData;
import io.github.zct_studio.teleportgui.Constants;
import net.minecraft.core.BlockPos;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Sqlite implements Provider {

    private Path dbFile;
    private Connection connection;

    @Override
    public void init(Path dataDir) throws Exception {
        this.dbFile = dataDir.resolve("storage.db");

        try {
            if (!java.nio.file.Files.exists(dataDir)) {
                java.nio.file.Files.createDirectories(dataDir);
            }

            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA busy_timeout = 5000");

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS warps (
                            name TEXT PRIMARY KEY,
                            x INT NOT NULL,
                            y INT NOT NULL,
                            z INT NOT NULL,
                            world_id TEXT NOT NULL
                        )
                        """);

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS players (
                            uuid TEXT PRIMARY KEY,
                            default_home TEXT NOT NULL DEFAULT ''
                        )
                        """);

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS player_homes (
                            player_uuid TEXT NOT NULL,
                            home_name TEXT NOT NULL,
                            x INT NOT NULL,
                            y INT NOT NULL,
                            z INT NOT NULL,
                            world_id TEXT NOT NULL,
                            PRIMARY KEY(player_uuid, home_name)
                        )
                        """);
            }

            Constants.LOGGER.info("SQLite storage initialized at {}", dbFile);
        } catch (Exception e) {
            throw e;
        } catch (java.lang.Exception e) {
            throw new Exception("Failed to initialize SQLite storage", e);
        }
    }

    @Override
    public List<NamedLoc> loadWarps() {
        List<NamedLoc> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name, x, y, z, world_id FROM warps")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    String worldId = rs.getString("world_id");
                    result.add(new NamedLoc(name, new BlockPos(x, y, z), worldId));
                }
            }
        } catch (SQLException e) {
            throw new Exception("Failed to load warps from SQLite", e);
        }
        return result;
    }

    @Override
    public void saveWarp(NamedLoc warp) {
        String sql = "INSERT OR REPLACE INTO warps (name, x, y, z, world_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, warp.getName());
            ps.setInt(2, warp.getX());
            ps.setInt(3, warp.getY());
            ps.setInt(4, warp.getZ());
            ps.setString(5, warp.getWorldId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new Exception("Failed to save warp to SQLite", e);
        }
    }

    @Override
    public boolean deleteWarp(String name) {
        String sql = "DELETE FROM warps WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new Exception("Failed to delete warp from SQLite", e);
        }
    }

    @Override
    public List<PlayerData> loadPlayers() {
        List<PlayerData> result = new ArrayList<>();
        try {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT uuid, default_home FROM players")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String uuid = rs.getString("uuid");
                        PlayerData tp = new PlayerData(uuid);
                        // PlayerData.setDefaultHome triggers StorageManager.save(),
                        // but we have no provider wired yet during loading.
                        // Use direct reflection-free workaround: homes are populated below,
                        // and defaultHome is set via the setter.
                        tp.setDefaultHome(rs.getString("default_home"));
                        result.add(tp);
                    }
                }
            }

            for (PlayerData tp : result) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT home_name, x, y, z, world_id FROM player_homes WHERE player_uuid = ?")) {
                    ps.setString(1, tp.getUuid());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String homeName = rs.getString("home_name");
                            int x = rs.getInt("x");
                            int y = rs.getInt("y");
                            int z = rs.getInt("z");
                            String worldId = rs.getString("world_id");
                            tp.addHome(new NamedLoc(homeName, new BlockPos(x, y, z), worldId));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new Exception("Failed to load players from SQLite", e);
        }
        return result;
    }

    @Override
    public void savePlayer(PlayerData player) {
        try {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT OR REPLACE INTO players (uuid, default_home) VALUES (?, ?)")) {
                    ps.setString(1, player.getUuid());
                    ps.setString(2, Optional.ofNullable(player.getDefaultHome()).orElse(""));
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM player_homes WHERE player_uuid = ?")) {
                    ps.setString(1, player.getUuid());
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT OR REPLACE INTO player_homes (player_uuid, home_name, x, y, z, world_id) VALUES (?, ?, ?, ?, ?, ?)")) {
                    for (NamedLoc home : player.getHomes()) {
                        ps.setString(1, player.getUuid());
                        ps.setString(2, home.getName());
                        ps.setInt(3, home.getX());
                        ps.setInt(4, home.getY());
                        ps.setInt(5, home.getZ());
                        ps.setString(6, home.getWorldId());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new Exception("Failed to save player to SQLite", e);
        }
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new Exception("Failed to close SQLite connection", e);
            }
        }
    }
}
