package io.github.zct_studio.teleportgui.compat.brigadier;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.github.zct_studio.teleportgui.Constants;

import java.lang.reflect.Field;
import java.util.Map;

public final class ReflectiveBrigadierAccess implements BrigadierAccess {

    private static final String FIELD_CHILDREN = "children";
    private static final String FIELD_LITERALS = "literals";

    private static final Field CHILDREN = findField(FIELD_CHILDREN);
    private static final Field LITERALS = findField(FIELD_LITERALS);

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
        removed |= removeFrom(LITERALS, root, literal);
        return removed;
    }

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
