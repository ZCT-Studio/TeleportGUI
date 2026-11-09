/*
 * Copyright (c) 2026 ZCT Studio
 * SPDX-License-Identifier: MIT
 */

package io.github.zct_studio.teleportgui.utils.config;

import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.platform.ConfigDir;
import io.github.zct_studio.teleportgui.storage.AtomicFiles;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ModSettingsLoader {

    private static final String FILE_NAME = "settings.yml";
    private static final String DEFAULT_YAML = """
            # TeleportGUI 模组设置
            #
            # runtimePersistence: 是否把冷却与待处理的传送请求写入 runtime.json，
            # 使它们在服务器重启后依然保留。设为 false 则只保存在内存中。
            runtimePersistence: true
            #
            # emoteTriggerUuids: 基岩版表情触发设置的 UUID 列表（Geyser 专用）。
            # 配置的表情将同时：
            #   1. 触发快捷菜单表单（TPA/请求/Home/Warp）
            #   2. 取消 Geyser 默认行为（如副手切换）
            # 设为空列表 [] 则禁用此功能。
            # 默认："Over There!" 表情 UUID
            emoteTriggerUuids:
              - "ce5c0300-7f03-455d-aaf1-352e4927b54d"
            """;
    public static ModSettings SETTINGS = new ModSettings();

    private ModSettingsLoader() {
    }

    public static Path configFolder() {
        return ConfigDir.get().resolve(Constants.MOD_ID);
    }

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

        Object emoteUuids = root.get("emoteTriggerUuids");
        if (emoteUuids instanceof List<?> list) {
            settings.emoteTriggerUuids = list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return settings;
    }
}
