package io.github.zct_studio.teleportgui.utils;

import io.github.zct_studio.teleportgui.Styles;
import io.github.zct_studio.teleportgui.Tr_i18n;
import net.minecraft.server.level.ServerPlayer;

public class CommandUtils {
    private CommandUtils() {
    }

    public static boolean isInvalidName(ServerPlayer player, String name) {
        if (name.isEmpty() || name.indexOf('"') >= 0 || name.indexOf('\'') >= 0) {
            player.sendSystemMessage(
                    Tr_i18n.tr("commands.teleportgui.common.invalidName", player)
                            .withStyle(Styles.ERROR),
                    true);
            return true;
        }
        return false;
    }

    public static String quote(String name) {
        return "\"" + name.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
