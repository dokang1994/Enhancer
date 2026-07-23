package com.enhancer.runtime;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Child-process fixture that retains one Scheduler queue writer lock until stdin closes. */
public final class SchedulerQueueLockHolderMain {
    private static final String LOCK_FILE_SUFFIX = ".scheduler-queue.lock";

    private SchedulerQueueLockHolderMain() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 2) {
            throw new IllegalArgumentException(
                    "storage root and queue identity are required");
        }
        Path storageRoot = Path.of(arguments[0]).toAbsolutePath().normalize();
        Files.createDirectories(storageRoot);
        Path lockPath = storageRoot.resolve(arguments[1] + LOCK_FILE_SUFFIX);
        try (FileChannel channel = FileChannel.open(
                        lockPath,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        LinkOption.NOFOLLOW_LINKS);
                FileLock lock = channel.lock()) {
            if (!lock.isValid()) {
                throw new IllegalStateException(
                        "Scheduler queue writer lock is not valid");
            }
            System.out.println("LOCKED");
            System.out.flush();
            System.in.read();
        }
    }
}
