package io.github.zct_studio.teleportgui.compat.brigadier;

import com.mojang.brigadier.tree.RootCommandNode;
import io.github.zct_studio.teleportgui.Constants;

@SuppressWarnings({"SpellCheckingInspection", "RedundantSuppression"})
public interface BrigadierAccess {

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isAvailable();

    @SuppressWarnings("UnusedReturnValue")
    boolean removeChild(RootCommandNode<?> root, String literal);

    @SuppressWarnings("unused")
    static BrigadierAccess noop() {
        return new BrigadierAccess() {
            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public boolean removeChild(RootCommandNode<?> root, String literal) {
                return false;
            }
        };
    }

    static BrigadierAccess create() {
        BrigadierAccess access = new ReflectiveBrigadierAccess();
        if (!access.isAvailable()) {
            Constants.LOGGER.error(
                    "Unable to access Brigadier command tree internals; runtime command "
                            + "enable/disable will not work, server restart required after config changes. "
                            + "Please verify field names in {} still match the current Brigadier version.",
                    ReflectiveBrigadierAccess.class.getSimpleName());
        }
        return access;
    }
}
