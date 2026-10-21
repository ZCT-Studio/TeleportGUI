package io.github.zct_studio.teleportgui.utils.config;

public class CommandNode {
    public Integer cooldownSeconds;    // null = inherit, 0 = no cooldown
    public Integer minDistance;        // random teleport: minimum horizontal distance
    public Integer maxDistance;        // null = inherit, -1 = no limit
    public PermissionNode permission;  // null = inherit
    public Integer expireSeconds;      // null = inherit, 0 = no expire
    public Integer requestExpireSeconds;   // null = inherit, 0 = no expire
    public Integer requestHereExpireSeconds;   // null = inherit, 0 = no expire
    public Boolean safetyCheck;        // null = inherit, false = off, true = on
    public Integer maxCount;
}
