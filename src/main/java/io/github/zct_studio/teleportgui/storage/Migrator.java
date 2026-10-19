package io.github.zct_studio.teleportgui.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.zct_studio.teleportgui.types.NamedLoc;
import io.github.zct_studio.teleportgui.types.PlayerData;
import io.github.zct_studio.teleportgui.Constants;
import net.minecraft.core.BlockPos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class Migrator {

    private Migrator() {
    }

    public static void migrateJsonToSqlite(Path jsonFile, Path dbFile) {
        Path backup = jsonFile.resolveSibling(jsonFile.getFileName() + ".bak");
        try {
            if (!Files.exists(jsonFile)) {
                throw new Exception("JSON source not found: " + jsonFile);
            }
            Files.copy(jsonFile, backup, StandardCopyOption.REPLACE_EXISTING);

            String json = Files.readString(jsonFile);
            Gson gson = new Gson();
            JsonFormat data = gson.fromJson(json, JsonFormat.class);
            if (data == null) {
                data = new JsonFormat();
            }

            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath())) {
                SqliteMigrationHelper.createTables(conn);

                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT OR REPLACE INTO warps (name, x, y, z, world_id) VALUES (?, ?, ?, ?, ?)")) {
                        for (NamedLoc w : data.warps) {
                            ps.setString(1, w.getName());
                            ps.setInt(2, w.getX());
                            ps.setInt(3, w.getY());
                            ps.setInt(4, w.getZ());
                            ps.setString(5, w.getWorldId());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }

                    try (PreparedStatement psPlayer = conn.prepareStatement(
                            "INSERT OR REPLACE INTO players (uuid, default_home) VALUES (?, ?)");
                         PreparedStatement psHome = conn.prepareStatement(
                                 "INSERT OR REPLACE INTO player_homes (player_uuid, home_name, x, y, z, world_id) VALUES (?, ?, ?, ?, ?, ?)")) {
                        for (PlayerData tp : data.players) {
                            psPlayer.setString(1, tp.getUuid());
                            psPlayer.setString(2, tp.getDefaultHome() == null ? "" : tp.getDefaultHome());
                            psPlayer.addBatch();

                            for (NamedLoc home : tp.getHomes()) {
                                psHome.setString(1, tp.getUuid());
                                psHome.setString(2, home.getName());
                                psHome.setInt(3, home.getX());
                                psHome.setInt(4, home.getY());
                                psHome.setInt(5, home.getZ());
                                psHome.setString(6, home.getWorldId());
                                psHome.addBatch();
                            }
                        }
                        psPlayer.executeBatch();
                        psHome.executeBatch();
                    }
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }

            Constants.LOGGER.info("JSON → SQLite migration succeeded ({} warps, {} players)",
                    data.warps.size(), data.players.size());
        } catch (java.lang.Exception e) {
            Constants.LOGGER.error("JSON → SQLite migration failed, restoring backup", e);
            restoreBackup(jsonFile, backup);
            throw new Exception("JSON → SQLite migration failed", e);
        }
    }

    public static void migrateSqliteToJson(Path dbFile, Path jsonFile) {
        Path backup = Files.exists(jsonFile)
                ? jsonFile.resolveSibling(jsonFile.getFileName() + ".bak")
                : null;
        try {
            if (backup != null) {
                Files.copy(jsonFile, backup, StandardCopyOption.REPLACE_EXISTING);
            }

            Class.forName("org.sqlite.JDBC");
            JsonFormat data = new JsonFormat();
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath())) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT name, x, y, z, world_id FROM warps");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        data.warps.add(new NamedLoc(rs.getString("name"),
                                new BlockPos(rs.getInt("x"), rs.getInt("y"), rs.getInt("z")),
                                rs.getString("world_id")));
                    }
                }

                List<PlayerData> loaded = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement("SELECT uuid, default_home FROM players");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PlayerData tp = new PlayerData(rs.getString("uuid"));
                        tp.setDefaultHome(rs.getString("default_home"));
                        loaded.add(tp);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT home_name, x, y, z, world_id FROM player_homes WHERE player_uuid = ?")) {
                    for (PlayerData tp : loaded) {
                        ps.setString(1, tp.getUuid());
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                tp.addHome(new NamedLoc(rs.getString("home_name"),
                                        new BlockPos(rs.getInt("x"), rs.getInt("y"), rs.getInt("z")),
                                        rs.getString("world_id")));
                            }
                        }
                    }
                }
                data.players = new ArrayList<>(loaded);
            }

            Gson pretty = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(jsonFile, pretty.toJson(data));

            Constants.LOGGER.info("SQLite → JSON migration succeeded ({} warps, {} players)",
                    data.warps.size(), data.players.size());
        } catch (java.lang.Exception e) {
            Constants.LOGGER.error("SQLite → JSON migration failed, restoring backup", e);
            if (backup != null) {
                restoreBackup(jsonFile, backup);
            }
            throw new Exception("SQLite → JSON migration failed", e);
        }
    }

    private static void restoreBackup(Path target, Path backup) {
        if (backup != null && Files.exists(backup)) {
            try {
                Files.copy(backup, target, StandardCopyOption.REPLACE_EXISTING);
                Constants.LOGGER.warn("Restored backup from {}", backup);
            } catch (java.lang.Exception restoreEx) {
                Constants.LOGGER.error("Failed to restore backup — manual recovery needed at {}", backup, restoreEx);
            }
        }
    }

    private static class JsonFormat {
        List<NamedLoc> warps = new ArrayList<>();
        List<PlayerData> players = new ArrayList<>();
    }

    private static final class SqliteMigrationHelper {
        static void createTables(Connection conn) throws SQLException {
            try (Statement stmt = conn.createStatement()) {
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
        }
    }
}
