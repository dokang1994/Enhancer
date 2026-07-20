package com.enhancer.runtime;

import com.enhancer.bus.CorruptedSpooledMessageException;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.TransportMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Child-process entry point for {@link IsolatedWorkerLauncher}.
 *
 * <p>It reads one message from a spool directory through the Gate 7 file spool adapter, so the
 * process boundary is proven by a real message crossing it rather than by a stub. The exit code
 * is the only channel back: this process prints nothing a parent will read, and the launcher
 * discards its output by construction.
 *
 * <p>Executing the approved work is deliberately not done here. Running the Gate 1-4 pipeline in
 * the child is the named follow-on and needs its own decision about how a result returns.
 */
public final class IsolatedWorkerMain {
    /** One message was read and decoded. */
    public static final int EXIT_MESSAGE_READ = 0;

    /** The spool directory was not named, or too many arguments were given. */
    public static final int EXIT_USAGE = 2;

    /** The spool directory holds no message. */
    public static final int EXIT_SPOOL_EMPTY = 3;

    /** A spooled message exists but cannot be decoded and will stay undecodable. */
    public static final int EXIT_MESSAGE_CORRUPT = 10;

    /** The spool could not be read for a reason that may be transient. */
    public static final int EXIT_SPOOL_UNREADABLE = 20;

    private IsolatedWorkerMain() {
    }

    public static void main(String[] arguments) {
        System.exit(run(arguments));
    }

    static int run(String[] arguments) {
        if (arguments == null || arguments.length != 1) {
            return EXIT_USAGE;
        }
        Path spoolRoot;
        try {
            spoolRoot = Path.of(arguments[0]).toAbsolutePath().normalize();
        } catch (RuntimeException invalidPath) {
            return EXIT_USAGE;
        }

        try {
            Optional<Path> spooled = firstSpooledMessage(spoolRoot);
            if (spooled.isEmpty()) {
                return EXIT_SPOOL_EMPTY;
            }
            TransportMessage message = FileSpoolMessageTransport.read(spooled.orElseThrow());
            return message.envelope().payload() == null ? EXIT_SPOOL_UNREADABLE : EXIT_MESSAGE_READ;
        } catch (CorruptedSpooledMessageException corrupt) {
            return EXIT_MESSAGE_CORRUPT;
        } catch (IOException unreadable) {
            return EXIT_SPOOL_UNREADABLE;
        }
    }

    private static Optional<Path> firstSpooledMessage(Path spoolRoot) throws IOException {
        if (!Files.isDirectory(spoolRoot, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        try (var paths = Files.list(spoolRoot)) {
            List<Path> spooled = paths
                    .filter(path -> path.getFileName().toString()
                            .endsWith(FileSpoolMessageTransport.FILE_SUFFIX))
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList();
            return spooled.isEmpty() ? Optional.empty() : Optional.of(spooled.get(0));
        }
    }
}
