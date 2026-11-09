package io.github.zct_studio.teleportgui.compat.brigadier;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.github.zct_studio.teleportgui.Constants;

import java.lang.reflect.Field;
import java.util.Map;

@SuppressWarnings({"SpellCheckingInspection", "RedundantSuppression"})
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
                    "Field '{}' not found in Brigadier's CommandNode; runtime command enable/disable unavailable", name, e);
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

        boolean removed = false;
        if (CHILDREN != null) {
            removed = removeFrom(CHILDREN, root, literal);
        }
        if (LITERALS != null) {
            removed |= removeFrom(LITERALS, root, literal);
        }
        return removed;
    }

    private static boolean removeFrom(Field field, Object root, String literal) {
        try {
            Object value = field.get(root);
            if (value instanceof Map<?, ?> map) {
                return map.remove(literal) != null;
            }
        } catch (IllegalAccessException e) {
            Constants.LOGGER.error("Failed to remove command '{}'", literal, e);
        }
        return false;
    }
}
