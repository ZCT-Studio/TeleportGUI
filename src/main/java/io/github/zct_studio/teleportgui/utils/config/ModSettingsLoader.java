package io.github.zct_studio.teleportgui.utils.config;

import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.platform.ConfigDir;
import io.github.zct_studio.teleportgui.storage.AtomicFiles;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ModSettingsLoader {

    public static ModSettings SETTINGS = new ModSettings();

    private static final String FILE_NAME = "settings.yml";

    private static final String DEFAULT_YAML = """
            # TeleportGUI 模组设置
            #
            # 本文件对所有存档生效，位于 config/teleportgui/settings.yml
            #
            # runtimePersistence: 是否把冷却与待处理的传送请求写入 runtime.json，
            # 使它们在服务器重启后依然保留。设为 false 则只保存在内存中。
            runtimePersistence: true
            """;

    private ModSettingsLoader() {
    }

    /**
     * 模组级配置目录：{@code <config>/teleportgui}。
     *
     * @return 配置目录路径
     */
    public static Path configFolder() {
        return ConfigDir.get().resolve(Constants.MOD_ID);
    }

    /**
     * 加载 settings.yml。
     * <p>
     * 文件位于模组级配置目录，对所有存档生效。若旧版本遗留的
     * {@code <世界目录>/teleportgui/settings.yml} 存在且新位置还没有文件，
     * 会先把它复制过来，避免升级后设置被重置。
     *
     * @param saveDir 世界根目录，仅用于查找旧位置的设置文件；传 null 表示跳过迁移
     */
    public static void load(Path saveDir) {
        Path folder = configFolder();
        Path settingsFile = folder.resolve(FILE_NAME);

        try {
            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
            }

            if (!Files.exists(settingsFile)) {
                Path legacy = legacyFile(saveDir);
                if (legacy != null) {
                    Files.copy(legacy, settingsFile);
                    Constants.LOGGER.info("已将设置文件从 {} 迁移到 {}", legacy, settingsFile);
                } else {
                    AtomicFiles.write(settingsFile, DEFAULT_YAML);
                    Constants.LOGGER.info("Settings file auto-generated at {}", settingsFile);
                    return;
                }
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

    /**
     * 查找旧版本存放在世界目录里的设置文件。
     *
     * @param saveDir 世界根目录
     * @return 旧文件路径；不存在时返回 null
     */
    private static Path legacyFile(Path saveDir) {
        if (saveDir == null) return null;
        Path legacy = saveDir.resolve(Constants.MOD_ID).resolve(FILE_NAME);
        return Files.isRegularFile(legacy) ? legacy : null;
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
