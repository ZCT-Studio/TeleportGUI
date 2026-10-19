package io.github.zct_studio.teleportgui.utils.config;

public class ModSettings {
    public String logLevel = "INFO";    // INFO, DEBUG, WARN, ERROR
    public StorageSection storage = new StorageSection();
    public boolean autoMigrate = true;

    public static class StorageSection {
        public String type = "json";      // json | sqlite
        public SqliteSection sqlite = new SqliteSection();
    }

    public static class SqliteSection {
        public String path = "data/teleport.db";
        public int busyTimeoutMs = 5000;
    }
}
