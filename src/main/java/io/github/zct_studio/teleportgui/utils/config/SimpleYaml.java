package io.github.zct_studio.teleportgui.utils.config;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tiny parser for the block-style YAML subset used by {@code settings.yml}.
 * <p>
 * The mod used to depend on SnakeYAML, but that library is not bundled into the
 * released jar, causing a {@code NoClassDefFoundError} on server startup. The
 * settings file is intentionally simple (nested maps and scalar values), so a
 * small self-contained parser removes the runtime dependency entirely.
 * <p>
 * Supported: comments, nested maps, quoted strings, booleans, integers and
 * {@code null}/{@code ~}. Not supported: lists, flow maps, anchors or
 * multi-line scalars.
 */
public final class SimpleYaml {

    private SimpleYaml() {}

    public static Map<String, Object> parse(String text) {
        Map<String, Object> root = new LinkedHashMap<>();
        if (text == null) return root;
        if (text.startsWith("\uFEFF")) text = text.substring(1);

        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(-1, root));

        for (String rawLine : text.split("\\R")) {
            String line = stripComment(rawLine);
            if (line.isBlank()) continue;

            int indent = countIndent(line);
            String content = line.trim();
            int colon = findColon(content);
            if (colon < 0) continue;

            String key = unquote(content.substring(0, colon).trim());
            if (key.isEmpty()) continue;
            String value = content.substring(colon + 1).trim();

            while (stack.peek().indent >= indent) {
                stack.pop();
            }
            Map<String, Object> parent = stack.peek().map;
            if (value.isEmpty()) {
                Map<String, Object> child = new LinkedHashMap<>();
                parent.put(key, child);
                stack.push(new Frame(indent, child));
            } else {
                parent.put(key, scalar(value));
            }
        }
        return root;
    }

    private static Object scalar(String value) {
        String v = unquote(value.trim());
        if (v.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (v.equalsIgnoreCase("false")) return Boolean.FALSE;
        if (v.equals("null") || v.equals("~")) return null;
        try {
            return Integer.valueOf(v);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Long.valueOf(v);
        } catch (NumberFormatException ignored) {
        }
        return v;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static int countIndent(String line) {
        int i = 0;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }
        return i;
    }

    private static int findColon(String content) {
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\'' && !doubleQuoted) singleQuoted = !singleQuoted;
            else if (c == '"' && !singleQuoted) doubleQuoted = !doubleQuoted;
            else if (c == ':' && !singleQuoted && !doubleQuoted) return i;
        }
        return -1;
    }

    private static String stripComment(String line) {
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\'' && !doubleQuoted) {
                singleQuoted = !singleQuoted;
            } else if (c == '"' && !singleQuoted) {
                doubleQuoted = !doubleQuoted;
            } else if (c == '#' && !singleQuoted && !doubleQuoted) {
                if (i == 0 || Character.isWhitespace(line.charAt(i - 1))) {
                    return line.substring(0, i);
                }
            }
        }
        return line;
    }

    private record Frame(int indent, Map<String, Object> map) {}
}
