package io.github.zct_studio.teleportgui.utils.config;

import java.util.LinkedHashMap;

public class Command {
    public int configVersion = 1;
    public CommandNode defaults = new CommandNode();

    public LinkedHashMap<String, CommandNode> warp = new LinkedHashMap<>();
    public LinkedHashMap<String, CommandNode> home = new LinkedHashMap<>();
    public LinkedHashMap<String, CommandNode> tpa = new LinkedHashMap<>();
    public LinkedHashMap<String, CommandNode> teleport = new LinkedHashMap<>();

    public void fillDefaults() {
        if (defaults == null) defaults = new CommandNode();
        if (warp == null) warp = new LinkedHashMap<>();
        if (home == null) home = new LinkedHashMap<>();
        if (tpa == null) tpa = new LinkedHashMap<>();
        if (teleport == null) teleport = new LinkedHashMap<>();

        ensureNode(warp, ConfigKeys.WARP_CREATE, 0, 0, false, 20);
        ensureNode(warp, ConfigKeys.WARP_GO, 5, 0);
        ensureNode(warp, ConfigKeys.WARP_DEL, 0, 2);
        ensureNode(warp, ConfigKeys.WARP_RENAME, 0, 2);
        ensureNode(warp, ConfigKeys.WARP_LIST, 0, 0);
        ensureNode(warp, ConfigKeys.WARP_GUI, 0, 0);

        ensureNode(home, ConfigKeys.HOME_CREATE, 0, 0, false, 10);
        ensureNode(home, ConfigKeys.HOME_GO, 5, 0);
        ensureNode(home, ConfigKeys.HOME_DEL, 0, 0);
        ensureNode(home, ConfigKeys.HOME_RENAME, 0, 0);
        ensureNode(home, ConfigKeys.HOME_LIST, 0, 0);
        ensureNode(home, ConfigKeys.HOME_DEFAULT, 0, 0);
        ensureNode(home, ConfigKeys.HOME_GUI, 0, 0);

        ensureNode(tpa, ConfigKeys.TPA_REQUEST, 30, 0);
        ensureNode(tpa, ConfigKeys.TPA_REQUEST_HERE, 30, 0);
        ensureNode(tpa, ConfigKeys.TPA_ACCEPT, 0, 0);
        ensureNode(tpa, ConfigKeys.TPA_DENY, 0, 0);

        ensureNode(teleport, ConfigKeys.TP_DEATH_POINT, 0, 0, true);
        ensureNode(teleport, ConfigKeys.TP_WORLD_SPAWN, 0, 0, true);
        ensureNode(teleport, ConfigKeys.TP_BACK, 0, 0, true);
        ensureNode(teleport, ConfigKeys.TP_SPAWN_POINT, 0, 0, false);
    }

    private void ensureNode(LinkedHashMap<String, CommandNode> map, String key,
                            int defaultCooldown, int defaultPermLevel) {
        ensureNode(map, key, defaultCooldown, defaultPermLevel, false, -1);
    }

    private void ensureNode(LinkedHashMap<String, CommandNode> map, String key,
                            int defaultCooldown, int defaultPermLevel, boolean defaultSafety) {
        ensureNode(map, key, defaultCooldown, defaultPermLevel, defaultSafety, -1);
    }

    private void ensureNode(LinkedHashMap<String, CommandNode> map, String key,
                            int defaultCooldown, int defaultPermLevel, boolean defaultSafety,
                            int defaultMaxCount) {
        if (!map.containsKey(key) || map.get(key) == null) {
            CommandNode node = new CommandNode();
            node.cooldownSeconds = defaultCooldown;
            node.permission = new PermissionNode(defaultPermLevel);
            node.maxDistance = -1;
            node.expireSeconds = 0;
            node.safetyCheck = defaultSafety;
            node.maxCount = defaultMaxCount;
            map.put(key, node);
        } else {
            CommandNode n = map.get(key);
            if (n.cooldownSeconds == null) n.cooldownSeconds = defaultCooldown;
            if (n.permission == null) n.permission = new PermissionNode(defaultPermLevel);
            if (n.maxDistance == null) n.maxDistance = -1;
            if (n.expireSeconds == null) n.expireSeconds = 0;
            if (n.safetyCheck == null) n.safetyCheck = defaultSafety;
            if (n.maxCount == null) n.maxCount = defaultMaxCount;
        }
    }
}
