package io.github.zct_studio.teleportgui.utils.config;

import java.util.List;

public final class ConfigKeys {
    @SuppressWarnings("unused")
    public static final String MODULE_TELEPORTGUI = "teleportgui";
    public static final String MODULE_WARP = "warp";
    public static final String MODULE_HOME = "home";
    public static final String MODULE_TPA = "tpa";
    public static final String MODULE_TELEPORT = "teleport";
    public static final String TG_PLAYER = "teleportgui";
    public static final String TG_ADMIN = "teleportgui_admin";
    public static final String TG_BE_ROOT = "teleportgui_be";
    public static final String TG_ALLOW_REQUEST = "allow_request";
    public static final String TG_NOTIF_GUI = "notif_gui";
    public static final String KEY_NOTIF_GUI_DEFAULT = "notifGuiDefault";
    public static final String TG_CONFIG = "config";
    public static final String TG_COMMAND = "command";
    public static final String TG_RELOAD = "reload";
    public static final String WARP_CREATE = "create";
    public static final String WARP_GO = "go";
    public static final String WARP_DEL = "del";
    public static final String WARP_RENAME = "rename";
    public static final String WARP_LIST = "list";
    public static final String WARP_GUI = "gui";
    public static final String HOME_CREATE = "create";
    public static final String HOME_GO = "go";
    public static final String HOME_DEL = "del";
    public static final String HOME_RENAME = "rename";
    public static final String HOME_LIST = "list";
    public static final String HOME_DEFAULT = "default";
    public static final String HOME_GUI = "gui";
    public static final String TPA_REQUEST = "request";
    public static final String TPA_REQUEST_HERE = "requestHere";
    public static final String TPA_ACCEPT = "accept";
    public static final String TPA_DENY = "deny";
    public static final String TP_DEATH_POINT = "deathPoint";
    public static final String TP_SPAWN_POINT = "spawnPoint";
    public static final String TP_WORLD_SPAWN = "worldSpawn";
    public static final String TP_BACK = "back";
    public static final String TP_RANDOM = "random";
    public static final String FIELD_COOLDOWN = "cooldownSeconds";
    public static final String FIELD_MIN_DISTANCE = "minDistance";
    public static final String FIELD_MAX_DISTANCE = "maxDistance";
    public static final String FIELD_PERMISSION = "permission";
    public static final String FIELD_EXPIRE = "expireSeconds";
    public static final String FIELD_SAFETY = "safetyCheck";
    public static final String FIELD_MAX_COUNT = "maxCount";
    public static final String COMMAND_HOME = "home";
    public static final String COMMAND_WARP = "warp";
    public static final String COMMAND_TPA = "tpa";
    public static final String COMMAND_BACK = "back";
    public static final String COMMAND_DEATH_POINT = "tpdeathpoint";
    public static final String COMMAND_WORLD_SPAWN = "tpworldspawn";
    public static final String COMMAND_SPAWN_POINT = "tpspawnpoint";
    public static final String COMMAND_FORCE = "tpforce";
    public static final String COMMAND_RANDOM_TP = "randomtp";

    public static final List<String> ALL_COMMANDS = List.of(
            COMMAND_HOME,
            COMMAND_WARP,
            COMMAND_TPA,
            COMMAND_BACK,
            COMMAND_DEATH_POINT,
            COMMAND_WORLD_SPAWN,
            COMMAND_SPAWN_POINT,
            COMMAND_FORCE,
            COMMAND_RANDOM_TP
    );

    public static String Build(String Module, String Sub) {
        return Module + "." + Sub;
    }

    private ConfigKeys() {
    }
}
