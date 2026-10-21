package io.github.zct_studio.teleportgui.utils.config;

import io.github.zct_studio.teleportgui.Constants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ModSettingsLoader {
    public static ModSettings SETTINGS = new ModSettings();

    private ModSettingsLoader() {}

    public static void load(Path settingsFile) {
        try {
            if (!Files.exists(settingsFile.getParent())) {
                Files.createDirectories(settingsFile.getParent());
            }
            if (!Files.exists(settingsFile)) {
                writeDefaultYaml(settingsFile);
                Constants.LOGGER.info("Settings file auto-generated at {}", settingsFile);
                return;
            }
            String yamlText = Files.readString(settingsFile);
            Map<String, Object> raw = SimpleYaml.parse(yamlText);
            SETTINGS = buildSettings(raw);
            Constants.LOGGER.info("Settings loaded from {}, storage.type={}",
                    settingsFile, SETTINGS.storage.type);
        } catch (Exception e) {
            Constants.LOGGER.error("Failed to load settings.yml — using defaults", e);
            SETTINGS = new ModSettings();
        }
    }

    @SuppressWarnings("unchecked")
    private static ModSettings buildSettings(Object raw) {
        ModSettings s = new ModSettings();
        if (!(raw instanceof Map)) return s;
        Map<String, Object> root = (Map<String, Object>) raw;

        Object logLevel = root.get("logLevel");
        if (logLevel instanceof String str) s.logLevel = str;

        Object autoMigrate = root.get("autoMigrate");
        if (autoMigrate instanceof Boolean b) s.autoMigrate = b;

        Object storageRaw = root.get("storage");
        if (storageRaw instanceof Map) {
            Map<String, Object> storage = (Map<String, Object>) storageRaw;
            Object stype = storage.get("type");
            if (stype instanceof String str) s.storage.type = str;

            Object sqliteRaw = storage.get("sqlite");
            if (sqliteRaw instanceof Map) {
                Map<String, Object> sqlite = (Map<String, Object>) sqliteRaw;
                Object path = sqlite.get("path");
                if (path instanceof String str) s.storage.sqlite.path = str;
                Object timeout = sqlite.get("busyTimeoutMs");
                if (timeout instanceof Number n) s.storage.sqlite.busyTimeoutMs = n.intValue();
            }
        }
        return s;
    }

    private static void writeDefaultYaml(Path file) throws Exception {
        String defaultYaml = """
                # TeleportGUI mod settings
                logLevel: INFO
                storage:
                  type: json
                  sqlite:
                    path: data/teleport.db
                    busyTimeoutMs: 5000
                autoMigrate: true
                """;
        Files.writeString(file, defaultYaml);
    }
}
