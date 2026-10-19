package io.github.zct_studio.teleportgui.types;

import io.github.zct_studio.teleportgui.storage.Manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PlayerData {

    private final String uuid;
    private String defaultHome = "";
    private final List<NamedLoc> homes = new ArrayList<>();
    private Boolean notificationGui = Boolean.TRUE;

    public PlayerData(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public String getDefaultHome() {
        return defaultHome;
    }

    public boolean isNotificationGuiEnabled() {
        return notificationGui == null || notificationGui;
    }

    public void setNotificationGuiEnabled(boolean enabled) {
        this.notificationGui = enabled;
        Manager.save();
    }

    public List<NamedLoc> getHomes() {
        return List.copyOf(homes);
    }

    public Optional<NamedLoc> getHome(String name) {
        return homes.stream()
                .filter(home -> Objects.equals(home.getName(), name))
                .findFirst();
    }

    public void setDefaultHome(String defaultHome) {
        this.defaultHome = defaultHome;
        Manager.save();
    }

    public boolean addHome(NamedLoc home) {
        if (getHome(home.getName()).isPresent()) {
            return true;
        }
        homes.add(home);
        Manager.save();
        return false;
    }

    public void deleteHome(NamedLoc home) {
        homes.remove(home);
        Manager.save();
    }
}
