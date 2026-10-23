package io.github.zct_studio.teleportgui.utils.config;

import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.storage.AtomicFiles;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ModSettingsLoader {

    public static ModSettings SETTINGS = new ModSettings();

    private static final String DEFAULT_YAML = """
            # TeleportGUI 模组设置
            #
            # runtimePersistence: 是否把冷却与待处理的传送请求写入 runtime.json，
            # 使它们在服务器重启后依然保留。设为 false 则只保存在内存中。
            runtimePersistence: true
            """;

    private ModSettingsLoader() {
    }

    public static void load(Path settingsFile) {
        try {
            Path parent = settingsFile.toAbsolutePath().getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(settingsFile)) {
                AtomicFiles.write(settingsFile, DEFAULT_YAML);
                Constants.LOGGER.info("Settings file auto-generated at {}", settingsFile);
                return;
            }

            String yamlText = Files.readString(settingsFile, StandardCharsets.UTF_8);
            SETTINGS = buildSettings(new Yaml().load(yamlText));
            Constants.LOGGER.info("Settings loaded from {} (runtimePersistence={})",
                    settingsFile, SETTINGS.runtimePersistence);
        } catch (IOException e) {
            Constants.LOGGER.error("Failed to read settings.yml — using defaults", e);
            SETTINGS = new ModSettings();
        } catch (RuntimeException e) {
            Constants.LOGGER.error("Failed to parse settings.yml — using defaults", e);
            SETTINGS = new ModSettings();
        }
    }

    private static ModSettings buildSettings(Object raw) {
        ModSettings settings = new ModSettings();
        if (!(raw instanceof Map<?, ?> root)) return settings;

        if (root.get("runtimePersistence") instanceof Boolean value) {
            settings.runtimePersistence = value;
        }
        return settings;
    }
}
