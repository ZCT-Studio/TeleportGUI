package io.github.zct_studio.teleportgui.utils.config;

import java.util.LinkedHashMap;

@SuppressWarnings("SameParameterValue")
public class Command {
    public static final int CURRENT_CONFIG_VERSION = 2;
    public static final int DEFAULT_REQUEST_EXPIRE_SECONDS = 120;

    public int configVersion = CURRENT_CONFIG_VERSION;
    public CommandNode defaults = new CommandNode();
    public Boolean notifGuiDefault = Boolean.TRUE;

    public LinkedHashMap<String, Boolean> commands = new LinkedHashMap<>();

    public LinkedHashMap<String, CommandNode> warp = new LinkedHashMap<>();
    public LinkedHashMap<String, CommandNode> home = new LinkedHashMap<>();
    public LinkedHashMap<String, CommandNode> tpa = new LinkedHashMap<>();
    public LinkedHashMap<String, CommandNode> teleport = new LinkedHashMap<>();

    public boolean fillDefaults() {
        boolean changed = configVersion < CURRENT_CONFIG_VERSION;
        configVersion = Math.max(configVersion, CURRENT_CONFIG_VERSION);

        if (defaults == null) {
            defaults = new CommandNode();
            changed = true;
        }
        if (warp == null) {
            warp = new LinkedHashMap<>();
            changed = true;
        }
        if (home == null) {
            home = new LinkedHashMap<>();
            changed = true;
        }
        if (tpa == null) {
            tpa = new LinkedHashMap<>();
            changed = true;
        }
        if (teleport == null) {
            teleport = new LinkedHashMap<>();
            changed = true;
        }
        if (commands == null) {
            commands = new LinkedHashMap<>();
            changed = true;
        }
        if (notifGuiDefault == null) {
            notifGuiDefault = Boolean.TRUE;
            changed = true;
        }
        for (String command : ConfigKeys.ALL_COMMANDS) {
            if (commands.putIfAbsent(command, true) == null) {
                changed = true;
            }
        }

        changed |= ensureNode(warp, ConfigKeys.WARP_CREATE, 0, 2, false, 20);
        changed |= ensureNode(warp, ConfigKeys.WARP_GO, 5, 0);
        changed |= ensureNode(warp, ConfigKeys.WARP_DEL, 0, 2);
        changed |= ensureNode(warp, ConfigKeys.WARP_RENAME, 0, 2);
        changed |= ensureNode(warp, ConfigKeys.WARP_LIST, 0, 0);
        changed |= ensureNode(warp, ConfigKeys.WARP_GUI, 0, 0);

        changed |= ensureNode(home, ConfigKeys.HOME_CREATE, 0, 0, false, 10);
        changed |= ensureNode(home, ConfigKeys.HOME_GO, 5, 0);
        changed |= ensureNode(home, ConfigKeys.HOME_DEL, 0, 0);
        changed |= ensureNode(home, ConfigKeys.HOME_RENAME, 0, 0);
        changed |= ensureNode(home, ConfigKeys.HOME_LIST, 0, 0);
        changed |= ensureNode(home, ConfigKeys.HOME_DEFAULT, 0, 0);
        changed |= ensureNode(home, ConfigKeys.HOME_GUI, 0, 0);

        // 带默认过期秒数的节点需同步登记到 GUI.EXPIRE_SECONDS_NODES，否则无法通过配置命令修改。
        changed |= ensureNode(tpa, ConfigKeys.TPA_REQUEST, 30, 0, false, -1,
                DEFAULT_REQUEST_EXPIRE_SECONDS);
        changed |= ensureNode(tpa, ConfigKeys.TPA_REQUEST_HERE, 30, 0, false, -1,
                DEFAULT_REQUEST_EXPIRE_SECONDS);
        changed |= ensureNode(tpa, ConfigKeys.TPA_ACCEPT, 0, 0);
        changed |= ensureNode(tpa, ConfigKeys.TPA_DENY, 0, 0);

        changed |= ensureNode(teleport, ConfigKeys.TP_DEATH_POINT, 0, 0, true);
        changed |= ensureNode(teleport, ConfigKeys.TP_WORLD_SPAWN, 0, 0, true);
        changed |= ensureNode(teleport, ConfigKeys.TP_BACK, 0, 0, true);
        changed |= ensureNode(teleport, ConfigKeys.TP_SPAWN_POINT, 0, 0, false);
        changed |= ensureNode(teleport, ConfigKeys.TP_RANDOM, 60, 0, true);

        CommandNode random = teleport.get(ConfigKeys.TP_RANDOM);
        if (random.minDistance == null) {
            random.minDistance = 100;
            changed = true;
        }

        if (random.maxDistance == null || random.maxDistance < 0) {
            random.maxDistance = 1000;
            changed = true;
        }
        return changed;
    }

    private boolean ensureNode(LinkedHashMap<String, CommandNode> map, String key,
                               int defaultCooldown, int defaultPermLevel) {
        return ensureNode(map, key, defaultCooldown, defaultPermLevel, false, -1);
    }

    private boolean ensureNode(LinkedHashMap<String, CommandNode> map, String key,
                               int defaultCooldown, int defaultPermLevel, boolean defaultSafety) {
        return ensureNode(map, key, defaultCooldown, defaultPermLevel, defaultSafety, -1);
    }

    private boolean ensureNode(LinkedHashMap<String, CommandNode> map, String key,
                               int defaultCooldown, int defaultPermLevel, boolean defaultSafety,
                               int defaultMaxCount) {
        return ensureNode(map, key, defaultCooldown, defaultPermLevel, defaultSafety,
                defaultMaxCount, 0);
    }

    private boolean ensureNode(LinkedHashMap<String, CommandNode> map, String key,
                               int defaultCooldown, int defaultPermLevel, boolean defaultSafety,
                               int defaultMaxCount, int defaultExpireSeconds) {
        if (!map.containsKey(key) || map.get(key) == null) {
            CommandNode node = new CommandNode();
            node.cooldownSeconds = defaultCooldown;
            node.permission = new PermissionNode(defaultPermLevel);
            node.maxDistance = -1;
            node.expireSeconds = defaultExpireSeconds;
            node.safetyCheck = defaultSafety;
            node.maxCount = defaultMaxCount;
            map.put(key, node);
            return true;
        }

        CommandNode n = map.get(key);
        boolean changed = false;
        if (n.cooldownSeconds == null) {
            n.cooldownSeconds = defaultCooldown;
            changed = true;
        }
        if (n.permission == null) {
            n.permission = new PermissionNode(defaultPermLevel);
            changed = true;
        }
        if (n.maxDistance == null) {
            n.maxDistance = -1;
            changed = true;
        }
        if (n.expireSeconds == null) {
            n.expireSeconds = defaultExpireSeconds;
            changed = true;
        }
        if (n.safetyCheck == null) {
            n.safetyCheck = defaultSafety;
            changed = true;
        }
        if (n.maxCount == null) {
            n.maxCount = defaultMaxCount;
            changed = true;
        }
        return changed;
    }
}
