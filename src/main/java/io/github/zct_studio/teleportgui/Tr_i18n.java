/*
 * Copyright (c) 2026 ZCT Studio
 * SPDX-License-Identifier: MIT
 */

package io.github.zct_studio.teleportgui;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Tr_i18n {

    private static final Pattern ARG_PATTERN = Pattern.compile("%(\\d+)%");
    private static final int MAX_CACHED_LANGUAGES = 64;
    private static final int MAX_MISSING_KEYS = 1024;
    private static final Map<String, Map<String, String>> CACHE = boundedMap(MAX_CACHED_LANGUAGES);
    private static final Map<String, Boolean> MISSING_KEYS = boundedMap(MAX_MISSING_KEYS);

    private Tr_i18n() {
    }

    public static MutableComponent translate(String key, ServerPlayer player, MutableComponent... args) {
        String translation = lookup(key, player);
        if (translation == null) {
            logMissingKey(key);
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
            logMissingKey(key);
            return key;
        }
        return translation;
    }

    public static String text(String key, ServerPlayer player, String... args) {
        Matcher matcher = ARG_PATTERN.matcher(raw(key, player));
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            int index = argumentIndex(matcher, args.length);
            String replacement = index >= 0 && args[index] != null ? args[index] : matcher.group();
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        return matcher.appendTail(result).toString();
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
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(language, Tr_i18n::readLanguage);
        }
    }

    private static void logMissingKey(String key) {
        synchronized (MISSING_KEYS) {
            if (MISSING_KEYS.put(key, Boolean.TRUE) == null) {
                Constants.LOGGER.error("Missing translation key: {}", key);
            }
        }
    }

    private static <K, V> Map<K, V> boundedMap(int limit) {
        return Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > limit;
            }
        });
    }

    private static Map<String, String> readLanguage(String language) {
        String path = String.format("/assets/%s/lang/%s.json", Constants.MOD_ID, language);
        try (InputStream stream = Tr_i18n.class.getResourceAsStream(path)) {
            if (stream == null) return Map.of();

            JsonElement json = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (!json.isJsonObject()) {
                throw new IllegalArgumentException("Expected a JSON object");
            }

            Map<String, String> translations = new HashMap<>();
            for (var entry : json.getAsJsonObject().entrySet()) {
                if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("Translation value is not a string: " + entry.getKey());
                }
                translations.put(entry.getKey(), entry.getValue().getAsString());
            }
            return Map.copyOf(translations);
        } catch (IOException | RuntimeException e) {
            Constants.LOGGER.error("Failed to load language file {} for {}", path, language, e);
            return Map.of();
        }
    }

    private static MutableComponent substitute(String translation, MutableComponent[] args) {
        Matcher matcher = ARG_PATTERN.matcher(translation);
        MutableComponent component = Component.literal("");
        int lastIndex = 0;

        while (matcher.find()) {
            component.append(Component.literal(translation.substring(lastIndex, matcher.start())));
            int index = argumentIndex(matcher, args.length);
            if (index >= 0 && args[index] != null) {
                component.append(args[index]);
            } else {
                component.append(Component.literal(matcher.group()));
            }
            lastIndex = matcher.end();
        }
        component.append(translation.substring(lastIndex));

        return component;
    }

    private static int argumentIndex(Matcher matcher, int argumentCount) {
        try {
            int index = Integer.parseInt(matcher.group(1));
            return index < argumentCount ? index : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
