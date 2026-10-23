package io.github.zct_studio.teleportgui.storage;

import io.github.zct_studio.teleportgui.Constants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 原子写入工具。
 * <p>
 * 直接把内容 TRUNCATE 后写回目标文件，一旦中途崩溃或断电，存档会变成半截内容，
 * 下次启动就读不回来了。这里统一改成「先写同目录临时文件，再原子替换」：
 * 要么完全成功，要么保留旧文件。
 */
public final class AtomicFiles {

    private static final String TEMP_SUFFIX = ".tmp";
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private AtomicFiles() {
    }

    /**
     * 原子地把 {@code content} 写入 {@code target}。
     *
     * @throws IOException 写入或替换失败（此时目标文件保持原样）
     */
    public static void write(Path target, String content) throws IOException {
        write(target, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 原子地把 {@code content} 写入 {@code target}。
     *
     * @throws IOException 写入或替换失败（此时目标文件保持原样）
     */
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

    /**
     * 生成当前线程唯一的一段临时文件后缀。
     * <p>
     * 临时文件名不能固定成 {@code storage.json.tmp}：如果同时有两处写入（例如将来有人
     * 在异步线程调用 save），它们会抢同一个临时文件，可能出现一方把另一方写了一半的内容
     * 挪到正式位置上。加上线程 id 与自增序列即可保证同进程内不会撞名。
     */
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
            // 部分文件系统（如某些网络盘）不支持原子替换，退化成普通替换，
            // 至少还能保证顺序写入而不是原地截断。
            Constants.LOGGER.warn("Atomic move is not supported for {}; falling back to a plain move",
                    target, e);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 临时文件清理失败不影响主流程，下次写入会覆盖它。
        }
    }
}
