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

/**
 * 极简的多语言查找与占位符替换。
 *
 * <h2>为什么不直接用 Minecraft 的 {@code Component.translatable}</h2>
 * 本模组完全跑在服务端，而 {@code Component.translatable} 依赖客户端语言文件，
 * 服务端发出的提示需要按「每个玩家自己的客户端语言」渲染，否则所有人看到的都是英文。
 * 这里直接在服务端读取 lang JSON，按玩家客户端语言挑选文本，再生成已定型的 {@link Component}。
 *
 * <h2>占位符语法</h2>
 * 用 {@code %0%}、{@code %1%} 这样的<b>下标占位符</b>，而不是 {@code %s} 或 {@code {}}。
 * 这样替换多个参数时可以任意调整顺序，重复引用同一个参数也不需要额外处理。
 * <pre>
 *   "commands.teleportgui.tpa.sent": "已向 %0% 发送传送请求"
 * </pre>
 *
 * <h2>语言回退</h2>
 * 先查玩家语言，查不到再回退到 {@code en_us}；两者都没有时记录错误日志并原样返回 key，
 * 便于发现漏翻。
 */
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

    /**
     * 按 {@code %N%} 把参数拼进文本。
     * <p>
     * 这里拼的是 {@link Component} 而不是字符串，这样占位符里传入的组件可以保留自己的样式
     * （比如玩家名加粗、坐标可点击），而不会在替换过程中被降级成纯文本。
     *
     * @param translation 带占位符的原始文本
     * @param args        按下标取用的组件参数，多余或缺失都不会报错
     * @return 拼接后的组件
     */
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
