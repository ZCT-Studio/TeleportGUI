package io.github.zct_studio.teleportgui.utils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.zct_studio.teleportgui.Constants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;

@SuppressWarnings("SameParameterValue")
public final class CommandLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Command CONFIG = new Command();
    public static Path CONFIG_FILE;

    private CommandLoader() {
    }

    public static void load(Path configFile) {
        CONFIG_FILE = configFile;
        try {
            if (!Files.exists(configFile.getParent())) {
                Files.createDirectories(configFile.getParent());
            }
            if (!Files.exists(configFile)) {
                CONFIG = new Command();
                CONFIG.fillDefaults();
                save(configFile);
                Constants.LOGGER.info("Config file auto-generated at {}", configFile);
                return;
            }
            String json = Files.readString(configFile);
            Command parsed = GSON.fromJson(json, Command.class);
            CONFIG = parsed == null ? new Command() : parsed;
            if (CONFIG.fillDefaults()) {
                save(configFile);
                Constants.LOGGER.info("Config migrated and saved to {}", configFile);
            }
            Constants.LOGGER.info("Config loaded from {}", configFile);
        } catch (Exception e) {
            Constants.LOGGER.error("Config file corrupted! Backing up and falling back to defaults.", e);
            try {
                if (Files.exists(configFile)) {
                    Files.copy(configFile, Path.of(configFile + ".bak"), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception ignored) {
            }
            CONFIG = new Command();
            CONFIG.fillDefaults();
        }
    }

    public static void save(Path configFile) {
        try {
            Files.writeString(configFile, GSON.toJson(CONFIG));
        } catch (Exception e) {
            Constants.LOGGER.error("Failed to save config", e);
        }
    }

    public static void save() {
        if (CONFIG_FILE != null) save(CONFIG_FILE);
    }

    public static int getCooldown(String module, String node) {
        Integer v = resolveInt(module, node, ConfigKeys.FIELD_COOLDOWN);
        return v == null ? 0 : v;
    }

    public static int getPermissionLevel(String module, String node) {
        int level = -1;
        CommandNode n = getNodeIfPresent(module, node);
        if (n != null && n.permission != null && n.permission.level >= 0) {
            level = n.permission.level;
        } else if (CONFIG.defaults != null
                && CONFIG.defaults.permission != null
                && CONFIG.defaults.permission.level >= 0) {
            level = CONFIG.defaults.permission.level;
        }
        return level;
    }

    public static int getRequestExpireSeconds(String module, String node) {
        Integer v = resolveInt(module, node, ConfigKeys.FIELD_EXPIRE_REQUEST);
        return v == null ? 0 : v;
    }

    public static boolean isSafetyCheckEnabled(String module, String node) {
        Boolean v = resolveBool(module, node, ConfigKeys.FIELD_SAFETY);
        return v != null && v;
    }

    public static int getMaxCount(String module, String node) {
        Integer v = resolveInt(module, node, ConfigKeys.FIELD_MAX_COUNT);
        return v == null ? -1 : v;
    }

    public static int getMinDistance(String module, String node) {
        Integer v = resolveInt(module, node, ConfigKeys.FIELD_MIN_DISTANCE);
        return v == null ? 0 : v;
    }

    public static int getMaxDistance(String module, String node) {
        Integer v = resolveInt(module, node, ConfigKeys.FIELD_MAX_DISTANCE);
        return v == null ? -1 : v;
    }

    public static boolean isCommandEnabled(String command) {
        if (CONFIG.commands == null) return true;
        Boolean enabled = CONFIG.commands.get(command);
        return enabled == null || enabled;
    }


    @SuppressWarnings("UnusedReturnValue")
    public static boolean setCommandEnabled(String command, boolean enabled) {
        if (!ConfigKeys.ALL_COMMANDS.contains(command)) return false;
        if (CONFIG.commands == null) CONFIG.commands = new LinkedHashMap<>();
        CONFIG.commands.put(command, enabled);
        save();
        return true;
    }


    private static Boolean resolveBool(String module, String node, String field) {
        Boolean v = extractBool(getNodeIfPresent(module, node), field);
        if (v != null) return v;
        return CONFIG.defaults == null ? null : extractBool(CONFIG.defaults, field);
    }

    private static Boolean extractBool(CommandNode n, String field) {
        if (n == null) return null;
        if (ConfigKeys.FIELD_SAFETY.equals(field)) return n.safetyCheck;
        return null;
    }

    private static Integer resolveInt(String module, String node, String field) {
        CommandNode n = getNodeIfPresent(module, node);
        Integer v = extractField(n, field);
        if (v != null) return v;
        if (CONFIG.defaults != null) {
            v = extractField(CONFIG.defaults, field);
        }
        return v;
    }

    private static Integer extractField(CommandNode n, String field) {
        if (n == null) return null;
        return switch (field) {
            case ConfigKeys.FIELD_COOLDOWN -> n.cooldownSeconds;
            case ConfigKeys.FIELD_MIN_DISTANCE -> n.minDistance;
            case ConfigKeys.FIELD_MAX_DISTANCE -> n.maxDistance;
            case ConfigKeys.FIELD_EXPIRE -> n.expireSeconds;
            case ConfigKeys.FIELD_EXPIRE_REQUEST -> n.requestExpireSeconds;
            case ConfigKeys.FIELD_MAX_COUNT -> n.maxCount;
            default -> null;
        };
    }

    private static CommandNode getNodeIfPresent(String module, String node) {
        LinkedHashMap<String, CommandNode> map = getModuleMap(module);
        return map == null ? null : map.get(node);
    }

    private static LinkedHashMap<String, CommandNode> getModuleMap(String module) {
        return switch (module) {
            case ConfigKeys.MODULE_WARP -> CONFIG.warp;
            case ConfigKeys.MODULE_HOME -> CONFIG.home;
            case ConfigKeys.MODULE_TPA -> CONFIG.tpa;
            case ConfigKeys.MODULE_TELEPORT -> CONFIG.teleport;
            default -> null;
        };
    }
}
