package io.github.zct_studio.teleportgui.utils.config;

import java.util.*;

/**
 * 一个<b>极简</b>的 YAML 子集解析器，只为 {@code settings.yml} 服务。
 *
 * <h2>支持什么</h2>
 * <ul>
 *   <li>嵌套映射（靠缩进层级）</li>
 *   <li>标量：{@code true}/{@code false}/{@code null}/{@code ~}、整数、长整数、字符串</li>
 *   <li>单双引号字符串、行尾 {@code #} 注释（引号内的 {@code #} 不算注释）</li>
 * </ul>
 *
 * <h2>不支持什么</h2>
 * <ul>
 *   <li>列表（{@code - item}）：这类行没有冒号，会被直接忽略</li>
 *   <li>多行块（{@code |} / {@code >}）、锚点与别名、流式写法（{@code {a: 1}}）</li>
 * </ul>
 * 之所以不引入完整 YAML 库，是为了避免为了一个只含两三个布尔值的配置文件
 * 拖进整个 SnakeYAML。若将来设置项需要列表，请直接换成正式库，
 * 不要在本类上继续堆特性。
 */
public final class SimpleYaml {

    private SimpleYaml() {
    }

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

            while (Objects.requireNonNull(stack.peek()).indent >= indent) {
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

    private record Frame(int indent, Map<String, Object> map) {
    }
}
