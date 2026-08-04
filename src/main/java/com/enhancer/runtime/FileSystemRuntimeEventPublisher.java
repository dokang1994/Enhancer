package com.enhancer.runtime;

import com.enhancer.io.BoundedFileOperations;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Publishes only an opaque durable runtime-event reference to one bounded local point.
 * Success is local adapter acceptance, never consumer delivery or runtime authority.
 */
public final class FileSystemRuntimeEventPublisher
        implements RuntimeEventPublisher {
    public static final String FILE_SUFFIX = ".runtime-event-reference";
    public static final int MIN_PENDING_PUBLICATIONS = 1;
    public static final int MAX_PENDING_PUBLICATIONS = 4096;

    private static final int ENVELOPE_MAGIC = 0x52545031;
    private static final int SCHEMA_VERSION = 1;
    private static final int DIGEST_BYTES = 32;
    private static final int MAX_REFERENCE_BYTES =
            RuntimeEventPublicationReference.MAX_REFERENCE_CHARACTERS * 4;
    private static final int HEADER_BYTES =
            Integer.BYTES * 3 + DIGEST_BYTES;
    private static final int MAX_ENVELOPE_BYTES =
            HEADER_BYTES + MAX_REFERENCE_BYTES;

    private final Path publicationRoot;
    private final int maxPendingPublications;

    public FileSystemRuntimeEventPublisher(
            Path publicationRoot,
            int maxPendingPublications) {
        Objects.requireNonNull(
                publicationRoot, "publicationRoot must not be null");
        if (maxPendingPublications < MIN_PENDING_PUBLICATIONS
                || maxPendingPublications > MAX_PENDING_PUBLICATIONS) {
            throw new IllegalArgumentException(
                    "maxPendingPublications must be between "
                            + MIN_PENDING_PUBLICATIONS
                            + " and "
                            + MAX_PENDING_PUBLICATIONS);
        }
        this.publicationRoot = publicationRoot.toAbsolutePath().normalize();
        this.maxPendingPublications = maxPendingPublications;
    }

    @Override
    public synchronized void publish(RuntimeEventPublicationReference reference)
            throws IOException {
        RuntimeEventPublicationReference checked = Objects.requireNonNull(
                reference, "reference must not be null");
        prepareRoot();
        Path point = pointPath(checked);
        if (Files.exists(point, LinkOption.NOFOLLOW_LINKS)) {
            requireExact(point, checked);
            return;
        }
        if (pendingPublications() >= maxPendingPublications) {
            throw new IOException(
                    "runtime event publication root holds the configured maximum of "
                            + maxPendingPublications
                            + " points");
        }
        publishNew(point, checked);
    }

    static String pointName(RuntimeEventPublicationReference reference) {
        RuntimeEventPublicationReference checked = Objects.requireNonNull(
                reference, "reference must not be null");
        return HexFormat.of().formatHex(sha256(
                        checked.reference().getBytes(StandardCharsets.UTF_8)))
                + FILE_SUFFIX;
    }

    RuntimeEventPublicationReference resolveAcceptedPoint(
            RuntimeEventPublicationReference reference) throws IOException {
        RuntimeEventPublicationReference checked = Objects.requireNonNull(
                reference, "reference must not be null");
        prepareRoot();
        Path point = pointPath(checked);
        if (!Files.exists(point, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "runtime event publication point is missing: "
                            + point.getFileName());
        }
        return requireExact(point, checked);
    }

    private void prepareRoot() throws IOException {
        Files.createDirectories(publicationRoot);
        if (!Files.isDirectory(publicationRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "runtime event publication root must be a directory without symbolic links");
        }
    }

    private long pendingPublications() throws IOException {
        try (var points = Files.list(publicationRoot)) {
            return points.filter(path -> path.getFileName().toString()
                            .endsWith(FILE_SUFFIX))
                    .count();
        }
    }

    private void publishNew(
            Path point,
            RuntimeEventPublicationReference reference) throws IOException {
        byte[] envelope = encode(reference);
        Path pending = Files.createTempFile(
                publicationRoot, ".pending-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(
                    pending,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(envelope);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            try {
                Files.move(
                        pending,
                        point,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (FileAlreadyExistsException race) {
                requireExact(point, reference);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException(
                        "runtime event publication requires atomic move support",
                        exception);
            }
        } finally {
            Files.deleteIfExists(pending);
        }
    }

    private RuntimeEventPublicationReference requireExact(
            Path point,
            RuntimeEventPublicationReference expected) throws IOException {
        if (!Files.isRegularFile(point, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "runtime event publication point must be a regular file without symbolic links");
        }
        byte[] envelope = BoundedFileOperations.readAllBytes(
                point, MAX_ENVELOPE_BYTES);
        if (envelope.length < HEADER_BYTES) {
            throw new IOException(
                    "runtime event publication envelope is truncated");
        }
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        if (buffer.getInt() != ENVELOPE_MAGIC) {
            throw new IOException(
                    "runtime event publication envelope magic is invalid");
        }
        int schemaVersion = buffer.getInt();
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IOException(
                    "runtime event publication schema version is unsupported");
        }
        int referenceLength = buffer.getInt();
        if (referenceLength <= 0
                || referenceLength > MAX_REFERENCE_BYTES
                || referenceLength != buffer.remaining() - DIGEST_BYTES) {
            throw new IOException(
                    "runtime event publication reference length is invalid");
        }
        byte[] declaredDigest = new byte[DIGEST_BYTES];
        buffer.get(declaredDigest);
        byte[] encodedReference = new byte[referenceLength];
        buffer.get(encodedReference);
        if (!MessageDigest.isEqual(
                declaredDigest,
                envelopeDigest(schemaVersion, encodedReference))) {
            throw new IOException(
                    "runtime event publication digest does not match");
        }
        RuntimeEventPublicationReference resolved =
                new RuntimeEventPublicationReference(
                        decodeUtf8(encodedReference));
        if (!resolved.equals(expected)) {
            throw new IOException(
                    "runtime event publication point does not match its deterministic reference");
        }
        return resolved;
    }

    private byte[] encode(RuntimeEventPublicationReference reference) {
        byte[] encodedReference =
                reference.reference().getBytes(StandardCharsets.UTF_8);
        byte[] digest = envelopeDigest(SCHEMA_VERSION, encodedReference);
        return ByteBuffer.allocate(HEADER_BYTES + encodedReference.length)
                .putInt(ENVELOPE_MAGIC)
                .putInt(SCHEMA_VERSION)
                .putInt(encodedReference.length)
                .put(digest)
                .put(encodedReference)
                .array();
    }

    private String decodeUtf8(byte[] encoded) throws IOException {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(encoded))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IOException(
                    "runtime event publication reference is not strict UTF-8",
                    exception);
        }
    }

    private Path pointPath(RuntimeEventPublicationReference reference) {
        Path point = publicationRoot.resolve(pointName(reference)).normalize();
        if (!point.startsWith(publicationRoot)) {
            throw new IllegalArgumentException(
                    "runtime event publication point resolves outside its root");
        }
        return point;
    }

    private static byte[] envelopeDigest(
            int schemaVersion,
            byte[] encodedReference) {
        return sha256(ByteBuffer.allocate(
                        Integer.BYTES * 3 + encodedReference.length)
                .putInt(ENVELOPE_MAGIC)
                .putInt(schemaVersion)
                .putInt(encodedReference.length)
                .put(encodedReference)
                .array());
    }

    private static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception);
        }
    }
}
