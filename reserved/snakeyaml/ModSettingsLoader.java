// 预留实现，不参与编译。
// 切换步骤见同目录 README.md。
package io.github.zct_studio.teleportgui.utils.config;

import io.github.zct_studio.teleportgui.Constants;
import org.yaml.snakeyaml.Yaml;

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
            Yaml yamlParser = new Yaml();
            Object raw = yamlParser.load(yamlText);
            SETTINGS = buildSettings(raw);
            Constants.LOGGER.info("Settings loaded from {}", settingsFile);
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

        Object runtimePersistence = root.get("runtimePersistence");
        if (runtimePersistence instanceof Boolean b) s.runtimePersistence = b;

        return s;
    }

    private static void writeDefaultYaml(Path file) throws Exception {
        String defaultYaml = """
                # TeleportGUI mod settings
                runtimePersistence: true
                """;
        Files.writeString(file, defaultYaml);
    }
}
