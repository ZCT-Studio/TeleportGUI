package io.github.zct_studio.teleportgui;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Tr_i18n {

    private static final Pattern ARG_PATTERN = Pattern.compile("%(\\d+)%");
    private static final ConcurrentHashMap<String, Map<String, String>> CACHE =
            new ConcurrentHashMap<>();

    private Tr_i18n() {
    }

    public static MutableComponent translate(String key, ServerPlayer player, MutableComponent... args) {
        String translation = lookup(key, player);
        if (translation == null) {
            Constants.LOGGER.error("Missing translation key: {}", key);
            return Component.literal(key);
        }
        return substitute(translation, args);
    }

    public static MutableComponent tr(String key, ServerPlayer player, MutableComponent... args) {
        return translate(key, player, args);
    }

    public static String raw(String key, ServerPlayer player) {
        String translation = lookup(key, player);
        if (translation == null) {
            Constants.LOGGER.error("Missing translation key: {}", key);
            return key;
        }
        return translation;
    }

    public static String text(String key, ServerPlayer player, String... args) {
        String translation = raw(key, player);
        for (int i = 0; i < args.length; i++) {
            translation = translation.replace("%" + i + "%", args[i] == null ? "" : args[i]);
        }
        return translation;
    }

    private static String lookup(String key, ServerPlayer player) {
        String language = player.clientInformation().language().toLowerCase(Locale.ROOT);
        String value = languageMap(language).get(key);
        if (value != null) return value;
        if (!"en_us".equals(language)) {
            return languageMap("en_us").get(key);
        }
        return null;
    }

    private static Map<String, String> languageMap(String language) {
        return CACHE.computeIfAbsent(language, Tr_i18n::readLanguage);
    }

    private static Map<String, String> readLanguage(String language) {
        String path = String.format("/assets/%s/lang/%s.json", Constants.MOD_ID, language);
        try (InputStream stream = Tr_i18n.class.getResourceAsStream(path)) {
            if (stream == null) return Map.of();

            JsonElement json = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (!json.isJsonObject()) return Map.of();

            Map<String, String> translations = new HashMap<>();
            for (var entry : json.getAsJsonObject().entrySet()) {
                translations.put(entry.getKey(), entry.getValue().getAsString());
            }
            return Map.copyOf(translations);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static MutableComponent substitute(String translation, MutableComponent[] args) {
        Matcher matcher = ARG_PATTERN.matcher(translation);
        MutableComponent component = Component.literal("");
        int lastIndex = 0;

        while (matcher.find()) {
            component.append(Component.literal(translation.substring(lastIndex, matcher.start())));
            int index = Integer.parseInt(matcher.group(1));
            if (index >= 0 && index < args.length && args[index] != null) {
                component.append(args[index]);
            }
            lastIndex = matcher.end();
        }
        component.append(translation.substring(lastIndex));

        return component;
    }
}
