package io.github.zct_studio.teleportgui.storage;

import io.github.zct_studio.teleportgui.Constants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicLong;

public final class AtomicFiles {

    private static final String TEMP_SUFFIX = ".tmp";
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private AtomicFiles() {
    }

    public static void write(Path target, String content) throws IOException {
        write(target, content.getBytes(StandardCharsets.UTF_8));
    }

    public static void write(Path target, byte[] content) throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        Path temp = target.resolveSibling(target.getFileName() + "." + uniqueSuffix() + TEMP_SUFFIX);
        try {
            Files.write(temp, content);
            moveIntoPlace(temp, target);
        } catch (IOException e) {
            deleteQuietly(temp);
            throw e;
        }
    }

    private static String uniqueSuffix() {
        return Long.toHexString(Thread.currentThread().threadId())
                + "-" + SEQUENCE.getAndIncrement();
    }

    private static void moveIntoPlace(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Constants.LOGGER.warn("Atomic move is not supported for {}; falling back to a plain move",
                    target, e);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
