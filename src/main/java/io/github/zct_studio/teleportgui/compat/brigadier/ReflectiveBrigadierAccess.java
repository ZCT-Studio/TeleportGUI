package io.github.zct_studio.teleportgui.compat.brigadier;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.github.zct_studio.teleportgui.Constants;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 基于反射的 {@link BrigadierAccess} 实现。
 *
 * <h2>依赖的内部字段</h2>
 * Brigadier 的 {@code CommandNode} 内部维护两个 map（均在 1.3.10 / 1.3.11 上验证存在）：
 * <ul>
 *   <li>{@code children}：{@code Map<String, CommandNode<S>>}，命令分发与补全的实际来源</li>
 *   <li>{@code literals}：{@code Map<String, LiteralCommandNode<S>>}，补全加速索引</li>
 * </ul>
 * 这两个字段是 private 的，字段名一旦变化，{@link #isAvailable()} 会返回 false，
 * 由调用方降级处理。
 *
 * <h2>为什么不直接用 Unsafe</h2>
 * 用 {@code setAccessible(true)} 就够了，Unsafe 在模块化之后反而更容易被拦。
 */
public final class ReflectiveBrigadierAccess implements BrigadierAccess {

    private static final String FIELD_CHILDREN = "children";
    private static final String FIELD_LITERALS = "literals";

    /** 命令分发与补全使用的子节点索引。null 表示该字段不可访问。 */
    private static final Field CHILDREN = findField(FIELD_CHILDREN);
    /** 补全加速使用的字面量索引。null 表示该字段不可访问。 */
    private static final Field LITERALS = findField(FIELD_LITERALS);

    /**
     * 缓存查找结果，避免 {@link CommandNode#getDeclaredField} 被反复调用。
     */
    private static Field findField(String name) {
        try {
            Field field = CommandNode.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException | RuntimeException e) {
            Constants.LOGGER.warn(
                    "Brigadier 的 CommandNode 中找不到字段 '{}'，运行期启用/禁用命令将不可用", name, e);
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        return CHILDREN != null && LITERALS != null;
    }

    @Override
    public boolean removeChild(RootCommandNode<?> root, String literal) {
        if (root == null || literal == null || !isAvailable()) return false;

        boolean removed = removeFrom(CHILDREN, root, literal);
        // literals 属于加速索引，删失败不算致命，但两个都删才是一致的状态。
        removed |= removeFrom(LITERALS, root, literal);
        return removed;
    }

    /**
     * 从指定字段对应的 map 里移除一个键。
     *
     * @return 是否真的移除了内容
     */
    private static boolean removeFrom(Field field, Object root, String literal) {
        try {
            Object value = field.get(root);
            if (value instanceof Map<?, ?> map) {
                return map.remove(literal) != null;
            }
        } catch (IllegalAccessException e) {
            Constants.LOGGER.error("移除命令 '{}' 失败", literal, e);
        }
        return false;
    }
}
