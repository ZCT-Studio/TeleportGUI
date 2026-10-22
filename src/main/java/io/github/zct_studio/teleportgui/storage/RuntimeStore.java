package io.github.zct_studio.teleportgui.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.zct_studio.teleportgui.Constants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RuntimeStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path file;
    private static Data data = new Data();
    private static boolean dirty;
    private static boolean enabled = true;
    private RuntimeStore() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!enabled) {
            data = new Data();
            dirty = false;
        }
    }

    public static void load(Path path) {
        file = path;
        data = new Data();
        if (!enabled || path == null || !Files.exists(path)) return;

        try {
            String json = Files.readString(path);
            Data loaded = GSON.fromJson(json, Data.class);
            if (loaded != null) {
                data = loaded;
                if (data.cooldowns == null) data.cooldowns = new ArrayList<>();
                if (data.tpa == null) data.tpa = new ArrayList<>();
            }
        } catch (java.lang.Exception e) {
            Constants.LOGGER.error("Failed to load runtime data; starting fresh", e);
            data = new Data();
        }
    }

    public static Data data() {
        return data;
    }

    public static void markDirty() {
        if (enabled) dirty = true;
    }

    public static boolean isDirty() {
        return enabled && dirty;
    }

    public static void save(List<CooldownEntry> cooldowns, List<TpaEntry> tpa) {
        if (!enabled) return;

        Data snapshot = new Data();
        snapshot.cooldowns = cooldowns;
        snapshot.tpa = tpa;
        data = snapshot;
        dirty = false;
        if (file == null) return;

        try {
            Path parent = file.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, GSON.toJson(snapshot));
        } catch (java.lang.Exception e) {
            dirty = true;
            Constants.LOGGER.error("Failed to save runtime data", e);
        }
    }

    public static final class CooldownEntry {
        public String uuid;
        public String module;
        public String node;
        public long remainingTicks;
    }

    public static final class TpaEntry {
        public String from;
        public String to;
        public boolean here;
        public long ageTicks;
        public long remainingTicks;
    }

    public static final class Data {
        public List<CooldownEntry> cooldowns = new ArrayList<>();
        public List<TpaEntry> tpa = new ArrayList<>();
    }
}
