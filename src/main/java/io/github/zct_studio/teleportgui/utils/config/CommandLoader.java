package io.github.zct_studio.teleportgui.utils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.zct_studio.teleportgui.Constants;
import io.github.zct_studio.teleportgui.storage.AtomicFiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;

/**
 * 命令配置（{@code config.json}）的读写与查询入口。
 *
 * <h2>配置层级与「null = 继承」约定</h2>
 * 配置分两层：功能模块里的具体节点（如 {@code tpa.request}）和全局兜底
 * {@link Command#defaults}。{@link CommandNode} 的每个字段都用包装类型，
 * <b>null 表示「本层没有设置，向上继承」</b>：
 * <ul>
 *   <li>{@code cooldownSeconds}：null 继承 defaults；0 表示明确「无冷却」。</li>
 *   <li>{@code maxDistance}：null 继承；-1 表示「无限制」。</li>
 *   <li>{@code expireSeconds}：null 继承；0 表示「永不过期」。</li>
 *   <li>{@code permission.level}：null 或小于 0 表示继承；-1 的最终含义是「无需权限」。</li>
 * </ul>
 * 因此查询方法一律返回「已经解析过继承关系」的最终值，调用方不必再关心层级。
 *
 * <h2>为什么用包装类型而不是原始类型</h2>
 * 若字段是 {@code int}，Gson 反序列化时缺失字段会被填成 0，与「明确设置为 0」
 * 无法区分，继承机制就失效了。
 *
 * <h2>配置损坏的处理</h2>
 * 解析失败时不让服务器崩溃：先把坏文件备份成 {@code config.json.bak}，
 * 再用默认值重建，尽量保住上一次能用的配置以便人工恢复。
 */
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
            AtomicFiles.write(configFile, GSON.toJson(CONFIG));
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

    public static int getExpireSeconds(String module, String node) {
        Integer v = resolveInt(module, node, ConfigKeys.FIELD_EXPIRE);
        if (v != null) return v;
        boolean isRequest = ConfigKeys.MODULE_TPA.equals(module)
                && (ConfigKeys.TPA_REQUEST.equals(node) || ConfigKeys.TPA_REQUEST_HERE.equals(node));
        return isRequest ? Command.DEFAULT_REQUEST_EXPIRE_SECONDS : 0;
    }

    public static boolean isNotifGuiDefaultEnabled() {
        return CONFIG.notifGuiDefault == null || CONFIG.notifGuiDefault;
    }

    public static void setNotifGuiDefault(boolean enabled) {
        CONFIG.notifGuiDefault = enabled;
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
            case ConfigKeys.FIELD_MAX_COUNT -> n.maxCount;
            default -> null;
        };
    }

    public static CommandNode nodeIfPresent(String module, String node) {
        return getNodeIfPresent(module, node);
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
