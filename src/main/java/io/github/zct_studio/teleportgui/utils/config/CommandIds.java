package io.github.zct_studio.teleportgui.utils.config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class CommandIds {

    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> IDS =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, String[]> NAMES =
            new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT = new AtomicInteger();

    private CommandIds() {
    }

    public static int of(String module, String node) {
        ConcurrentHashMap<String, Integer> nodes =
                IDS.computeIfAbsent(module, key -> new ConcurrentHashMap<>());
        Integer existing = nodes.get(node);
        if (existing != null) return existing;

        int id = NEXT.incrementAndGet();
        Integer previous = nodes.putIfAbsent(node, id);
        if (previous != null) return previous;

        NAMES.put(id, new String[]{module, node});
        return id;
    }

    public static String module(int id) {
        String[] name = NAMES.get(id);
        return name == null ? null : name[0];
    }

    public static String node(int id) {
        String[] name = NAMES.get(id);
        return name == null ? null : name[1];
    }
}
