package io.github.zct_studio.teleportgui.storage;

@SuppressWarnings("unused")
public class Exception extends RuntimeException {
    public Exception(String msg) {
        super(msg);
    }

    public Exception(String msg, Throwable cause) {
        super(msg, cause);
    }
}
