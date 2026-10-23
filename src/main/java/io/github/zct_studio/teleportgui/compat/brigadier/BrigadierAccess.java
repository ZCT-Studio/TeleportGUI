package io.github.zct_studio.teleportgui.compat.brigadier;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.github.zct_studio.teleportgui.Constants;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Brigadier 命令树的可变访问适配器（反射实现）。
 *
 * <h2>这个包是做什么的</h2>
 * Brigadier 的 {@link CommandNode} 只提供 {@code addChild}，<b>没有</b>任何公开的
 * 「移除子节点」API。但本模组需要支持 {@code /teleportgui_admin command home false}
 * 之后立刻让 {@code /home} 从补全和命令分发里消失，因此必须摸到内部的 map。
 * <p>
 * 所有「与 Brigadier 内部实现强相关」的代码都集中在这个包：
 * <ul>
 *   <li>{@link BrigadierAccess} —— 策略接口，描述需要的能力</li>
 *   <li>{@link ReflectiveBrigadierAccess} —— 基于反射的通用实现</li>
 * </ul>
 * 升级 Minecraft / Brigadier 之后如果字段名变了，只需要在这个包里加一个新的实现类，
 * 业务代码（{@code commands.Registry}）完全不用改。
 *
 * <h2>为什么用反射而不是编译期分支</h2>
 * 本项目只针对一个 Minecraft 版本（见 {@code settings.gradle.kts} 的 {@code mc(...)}），
 * Brigadier 的字段名在该版本内是稳定的。用反射可以少一份 stonecutter 预处理代码，
 * 代价是字段名变化时会在启动日志里看到明确的告警（见 {@link #isAvailable()}），
 * 而不是编译失败。若将来同时维护多个 MC 版本且字段名不同，再在这里加 stonecutter 分支。
 */
public interface BrigadierAccess {

    /**
     * 当前实现是否可用。
     * <p>
     * 返回 {@code false} 时表示无法访问命令树内部结构。调用方应当退化为
     * 「只在启动时注册命令，不做运行期增删」，而不是抛异常让服务器起不来。
     *
     * @return 是否可以安全调用 {@link #removeChild}
     */
    boolean isAvailable();

    /**
     * 从命令树根节点移除一个字面量子命令。
     * <p>
     * 需要同时清理 Brigadier 的两个索引：
     * <ul>
     *   <li>{@code children}：真正参与命令分发与补全的 {@code String -> CommandNode}</li>
     *   <li>{@code literals}：供补全加速使用的 {@code String -> LiteralCommandNode}</li>
     * </ul>
     * 只清一个会留下「命令不能用但补全还列着」这类不一致状态。
     *
     * @param root    命令树根节点
     * @param literal 要移除的字面量（不带命名空间前缀）
     * @return 是否真的移除了（原本不存在或实现不可用时返回 false）
     */
    boolean removeChild(RootCommandNode<?> root, String literal);

    /**
     * 创建一个「什么都不做」的实现，用于反射不可用时的降级。
     *
     * @return 空实现
     */
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

    /**
     * 创建当前平台默认的实现。
     * <p>
     * 目前只有反射实现；将来需要按版本区分时，在这里根据版本选择不同的类即可。
     *
     * @return Brigadier 访问器
     */
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
