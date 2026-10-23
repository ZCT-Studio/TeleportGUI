package io.github.zct_studio.teleportgui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;

public final class Styles {

    public static final Style EMPHASIS = of(ChatFormatting.BOLD);
    public static final Style ERROR = of(ChatFormatting.RED);
    public static final Style ERROR_STRONG = of(ChatFormatting.RED, ChatFormatting.BOLD);
    public static final Style SUCCESS = of(ChatFormatting.GREEN);
    public static final Style SUCCESS_STRONG = of(ChatFormatting.GREEN, ChatFormatting.BOLD);
    public static final Style INFO = of(ChatFormatting.AQUA);
    public static final Style INFO_STRONG = of(ChatFormatting.AQUA, ChatFormatting.BOLD);
    public static final Style WARNING = of(ChatFormatting.YELLOW);
    public static final Style HEADER = of(ChatFormatting.YELLOW, ChatFormatting.BOLD);
    public static final Style MUTED = of(ChatFormatting.GRAY);
    public static final Style MUTED_STRONG = of(ChatFormatting.GRAY, ChatFormatting.BOLD);
    public static final Style SUBTLE = of(ChatFormatting.DARK_GRAY);
    public static final Style BODY = of(ChatFormatting.WHITE);
    public static final Style ACTION = of(ChatFormatting.LIGHT_PURPLE);
    public static final Style ACTION_SECONDARY = of(ChatFormatting.DARK_PURPLE);
    public static final Style ACTION_SOFT = of(ChatFormatting.DARK_AQUA);
    public static final Style ACTION_FORCE = of(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD);

    private Styles() {
    }

    private static Style of(ChatFormatting... formats) {
        return Style.EMPTY.applyFormats(formats);
    }
}
