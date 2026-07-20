package com.enhancer.bus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * First concrete {@link MessageTransport}: it carries one route and envelope to a local spool
 * directory that a peer process reads.
 *
 * <p>Per the accepted transport decision, {@code ACCEPTED} means only that this adapter took
 * responsibility for the hop by durably spooling the message. It is never evidence that a
 * receiving Message Bus admitted, journaled, dispatched, or delivered it.
 *
 * <p>A refused message spools nothing. Capacity exhaustion is {@code BACKPRESSURED} and an
 * unusable spool root is {@code UNAVAILABLE}; neither consumes bus delivery, idempotency,
 * cancellation, dead-letter, or journal state, and retry timing stays higher-level policy.
 *
 * <p>Wire format and integrity belong to {@link MessageEnvelopeCodec}; this adapter owns only
 * publication. Each hop is written through a temporary file and an atomic move into its own
 * freshly named file, so a reader never observes a partial message and resending an envelope
 * never overwrites an earlier hop. Because the codec frame is deterministic and this adapter
 * adds no header of its own, one message always spools identical bytes.
 */
public final class FileSpoolMessageTransport implements MessageTransport {
    public static final String FILE_SUFFIX = ".transport";

    private static final MessageEnvelopeCodec CODEC = new MessageEnvelopeCodec();

    private final Path spoolRoot;
    private final BackpressurePolicy backpressurePolicy;

    public FileSpoolMessageTransport(Path spoolRoot, BackpressurePolicy backpressurePolicy) {
        Objects.requireNonNull(spoolRoot, "spoolRoot must not be null");
        this.backpressurePolicy = Objects.requireNonNull(
                backpressurePolicy, "backpressurePolicy must not be null");
        this.spoolRoot = spoolRoot.toAbsolutePath().normalize();
    }

    @Override
    public TransportOutcome send(TransportMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        byte[] frame = CODEC.encode(message);
        try {
            prepareRoot();
            if (pendingMessages() >= backpressurePolicy.maxPendingPublications()) {
                return refuse(
                        TransportStatus.BACKPRESSURED,
                        "spool holds the configured maximum of "
                                + backpressurePolicy.maxPendingPublications() + " messages");
            }
            spool(frame);
            return new TransportOutcome(TransportStatus.ACCEPTED, Optional.empty());
        } catch (IOException unusableSpool) {
            return refuse(TransportStatus.UNAVAILABLE, reason(unusableSpool));
        }
    }

    /**
     * Decodes one spooled message.
     *
     * <p>A {@link CorruptedSpooledMessageException} means the message itself is unusable and will
     * stay unusable, so a caller should dead-letter rather than retry it. Any other
     * {@link IOException} is a filesystem condition and may be transient.
     */
    public static TransportMessage read(Path spooledMessage) throws IOException {
        Objects.requireNonNull(spooledMessage, "spooledMessage must not be null");
        return CODEC.decode(Files.readAllBytes(spooledMessage));
    }

    private void prepareRoot() throws IOException {
        Files.createDirectories(spoolRoot);
        if (!Files.isDirectory(spoolRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("transport spool root must be a directory");
        }
    }

    private long pendingMessages() throws IOException {
        try (var paths = Files.list(spoolRoot)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(FILE_SUFFIX))
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .count();
        }
    }

    private void spool(byte[] frame) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(frame);
        Path pending = Files.createTempFile(spoolRoot, ".pending-", ".tmp");
        Path destination = spoolRoot.resolve(UUID.randomUUID() + FILE_SUFFIX);
        try {
            try (FileChannel channel = FileChannel.open(
                    pending, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            Files.move(pending, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException failedPublication) {
            Files.deleteIfExists(pending);
            throw failedPublication;
        }
    }

    private static TransportOutcome refuse(TransportStatus status, String reason) {
        return new TransportOutcome(status, Optional.of(reason));
    }

    private static String reason(IOException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
