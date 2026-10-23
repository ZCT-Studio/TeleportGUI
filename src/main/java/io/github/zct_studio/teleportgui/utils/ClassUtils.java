package io.github.zct_studio.teleportgui.utils;

public class ClassUtils {
    public static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
