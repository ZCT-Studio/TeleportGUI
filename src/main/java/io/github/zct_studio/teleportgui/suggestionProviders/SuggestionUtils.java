package io.github.zct_studio.teleportgui.suggestionProviders;

import java.util.Locale;

public final class SuggestionUtils {

    private SuggestionUtils() {
    }

    public static String filter(String remaining) {
        String value = remaining == null ? "" : remaining.trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value.toLowerCase(Locale.ROOT);
    }

    public static boolean matches(String candidate, String filter) {
        if (candidate == null) return false;
        if (filter.isEmpty()) return true;
        return candidate.toLowerCase(Locale.ROOT).contains(filter);
    }
}
