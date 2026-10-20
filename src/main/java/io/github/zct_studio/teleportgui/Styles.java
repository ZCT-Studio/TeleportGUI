package io.github.zct_studio.teleportgui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;

public final class Styles {

    private Styles() {
    }

    // 仅加粗，不带颜色
    public static final Style EMPHASIS = of(ChatFormatting.BOLD);

    // 错误、拒绝、危险操作
    public static final Style ERROR = of(ChatFormatting.RED);
    public static final Style ERROR_STRONG = of(ChatFormatting.RED, ChatFormatting.BOLD);

    // 成功、允许、完成
    public static final Style SUCCESS = of(ChatFormatting.GREEN);
    public static final Style SUCCESS_STRONG = of(ChatFormatting.GREEN, ChatFormatting.BOLD);

    // 普通信息、玩家名、列表分隔符
    public static final Style INFO = of(ChatFormatting.AQUA);
    public static final Style INFO_STRONG = of(ChatFormatting.AQUA, ChatFormatting.BOLD);

    // 提示、警告、返回/关闭等弱操作
    public static final Style WARNING = of(ChatFormatting.YELLOW);

    // 列表标题、章节名
    public static final Style HEADER = of(ChatFormatting.YELLOW, ChatFormatting.BOLD);

    // 次要说明（Lore、补充信息）
    public static final Style MUTED = of(ChatFormatting.GRAY);
    public static final Style MUTED_STRONG = of(ChatFormatting.GRAY, ChatFormatting.BOLD);

    // 更次要的说明（如世界 ID）
    public static final Style SUBTLE = of(ChatFormatting.DARK_GRAY);

    // 普通正文
    public static final Style BODY = of(ChatFormatting.WHITE);

    // 可点击操作：复制坐标等
    public static final Style ACTION = of(ChatFormatting.LIGHT_PURPLE);

    // 可点击操作：次级信息（如维度）
    public static final Style ACTION_SECONDARY = of(ChatFormatting.DARK_PURPLE);

    // 可点击操作：链接式操作（如重命名）
    public static final Style ACTION_LINK = of(ChatFormatting.BLUE);

    // 可点击操作：弱强调（如设为默认）
    public static final Style ACTION_SOFT = of(ChatFormatting.DARK_AQUA);

    // 可点击操作：强制类操作（如强制传送）
    public static final Style ACTION_FORCE = of(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD);

    private static Style of(ChatFormatting... formats) {
        return Style.EMPTY.applyFormats(formats);
    }
}
