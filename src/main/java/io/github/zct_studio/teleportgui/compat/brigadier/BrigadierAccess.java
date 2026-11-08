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
                    "无法访问 Brigadier 命令树内部结构；运行期启用/禁用命令将不会生效，"
                            + "修改配置后需要重启服务器。请检查 {} 中的字段名是否仍与当前 "
                            + "Brigadier 版本匹配。",
                    ReflectiveBrigadierAccess.class.getSimpleName());
        }
        return access;
    }
}
