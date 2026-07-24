package com.enhancer.runtime;

import com.enhancer.io.BoundedFileOperations;
import com.enhancer.io.FileSizeLimitExceededException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Single-file filesystem adapter for the worker's cycle-intent checkpoint. Atomic publication
 * prevents partial visibility; parent-directory power-loss durability is not claimed. Bounded,
 * strict-UTF-8, digest-checked; fails closed on corrupt or oversized state.
 */
public final class FileSystemPendingFinalizationStore
        implements PendingFinalizationStore {
    private static final int ENVELOPE_MAGIC = 0x50464331;
    private static final int DIGEST_BYTES = 32;
    static final int HEADER_BYTES =
            Integer.BYTES + Long.BYTES + Integer.BYTES + DIGEST_BYTES;
    static final int MAX_STATE_BYTES = 16 * 1024;
    private static final int MAX_STRING_BYTES = 4 * 1024;
    public static final int PREVIOUS_SCHEMA_VERSION = 1;
    public static final int CURRENT_SCHEMA_VERSION = 2;
    private static final String PAYLOAD_KIND = "pending-finalization";
    private static final String FILE_NAME = "pending.finalization";

    private final Path storageRoot;
    private final PendingFinalizationMigrationHook migrationHook;

    public FileSystemPendingFinalizationStore(Path storageRoot) {
        this(storageRoot, ignored -> {
        });
    }

    FileSystemPendingFinalizationStore(
            Path storageRoot,
            PendingFinalizationMigrationHook migrationHook) {
        Objects.requireNonNull(storageRoot, "storageRoot must not be null");
        this.storageRoot = storageRoot.toAbsolutePath().normalize();
        this.migrationHook = Objects.requireNonNull(
                migrationHook, "migrationHook must not be null");
    }

    @Override
    public void record(PendingFinalization pending) throws IOException {
        Objects.requireNonNull(pending, "pending must not be null");
        prepareRoot();
        byte[] payload = encode(pending);
        if (payload.length > MAX_STATE_BYTES) {
            throw new IOException(
                    "Pending finalization exceeds the supported size limit");
        }
        long storedAtMillis = Instant.now().toEpochMilli();
        byte[] digest = envelopeDigest(
                storedAtMillis,
                payload.length,
                payload);
        ByteBuffer envelope = ByteBuffer.allocate(
                        HEADER_BYTES + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAtMillis)
                .putInt(payload.length)
                .put(digest)
                .put(payload);
        envelope.flip();

        Path pendingFile = Files.createTempFile(
                storageRoot, ".pending-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(
                    pendingFile,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                while (envelope.hasRemaining()) {
                    channel.write(envelope);
                }
                channel.force(true);
            }
            try {
                Files.move(
                        pendingFile,
                        artifactPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException(
                        "Pending finalization storage requires atomic move support",
                        exception);
            }
        } finally {
            Files.deleteIfExists(pendingFile);
        }
    }

    @Override
    public Optional<PendingFinalization> findPending() throws IOException {
        Path artifact = artifactPath();
        Optional<ValidatedEnvelope> validated =
                readValidatedEnvelope(artifact);
        if (validated.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(decodeCurrent(
                validated.orElseThrow().payload()));
    }

    /**
     * Explicitly migrates the single schema-v1 checkpoint to the current
     * schema. Callers must stop the owning Scheduler before invoking it.
     */
    public PendingFinalizationMigrationResult migrateSchemaV1ToCurrent()
            throws IOException {
        Path artifact = artifactPath();
        Optional<ValidatedEnvelope> existing =
                readValidatedEnvelope(artifact);
        if (existing.isEmpty()) {
            return PendingFinalizationMigrationResult.ABSENT;
        }
        ValidatedEnvelope original = existing.orElseThrow();
        int schemaVersion = schemaVersion(original.payload());
        if (schemaVersion == CURRENT_SCHEMA_VERSION) {
            decodeCurrent(original.payload());
            return PendingFinalizationMigrationResult.ALREADY_CURRENT;
        }
        if (schemaVersion != PREVIOUS_SCHEMA_VERSION) {
            throw corrupted("state schema version is unsupported");
        }
        PendingFinalization migrated = decodePrevious(original.payload());
        byte[] candidateEnvelope = encodeEnvelope(migrated);
        prepareRoot();
        Path candidate = Files.createTempFile(
                storageRoot, ".pending-migration-", ".tmp");
        try {
            writeCandidate(candidate, candidateEnvelope);
            ValidatedEnvelope validatedCandidate =
                    readValidatedEnvelope(candidate).orElseThrow(() ->
                            corrupted("migration candidate is missing"));
            PendingFinalization decodedCandidate =
                    decodeCurrent(validatedCandidate.payload());
            if (!migrated.equals(decodedCandidate)) {
                throw corrupted(
                        "migration candidate does not match converted state");
            }
            migrationHook.beforeSourceValidation(artifact);
            Optional<ValidatedEnvelope> current =
                    readValidatedEnvelope(artifact);
            if (current.isEmpty()
                    || !MessageDigest.isEqual(
                            original.bytes(),
                            current.orElseThrow().bytes())) {
                throw new ConcurrentPendingFinalizationMigrationException();
            }
            try {
                Files.move(
                        candidate,
                        artifact,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException(
                        "Pending finalization storage requires atomic move support",
                        exception);
            }
        } finally {
            Files.deleteIfExists(candidate);
        }
        return PendingFinalizationMigrationResult.MIGRATED;
    }

    private Optional<ValidatedEnvelope> readValidatedEnvelope(
            Path artifact) throws IOException {
        if (!Files.exists(artifact, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        if (!Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS)) {
            throw corrupted("artifact is not a regular file");
        }
        long artifactSize = Files.size(artifact);
        if (artifactSize < HEADER_BYTES
                || artifactSize > HEADER_BYTES + MAX_STATE_BYTES) {
            throw corrupted("artifact size is outside supported bounds");
        }

        byte[] envelope;
        try {
            envelope = BoundedFileOperations.readAllBytes(
                    artifact,
                    HEADER_BYTES + MAX_STATE_BYTES);
        } catch (FileSizeLimitExceededException exception) {
            throw corrupted(
                    "artifact grew outside supported bounds while reading");
        } catch (NoSuchFileException exception) {
            return Optional.empty();
        }
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        if (buffer.getInt() != ENVELOPE_MAGIC) {
            throw corrupted("envelope header is invalid");
        }
        long storedAtMillis = buffer.getLong();
        int declaredLength = buffer.getInt();
        if (declaredLength < 0
                || declaredLength > MAX_STATE_BYTES
                || declaredLength != buffer.remaining() - DIGEST_BYTES) {
            throw corrupted(
                    "declared state length does not match the artifact");
        }
        byte[] declaredDigest = new byte[DIGEST_BYTES];
        buffer.get(declaredDigest);
        byte[] payload = new byte[declaredLength];
        buffer.get(payload);
        byte[] actualDigest = envelopeDigest(
                storedAtMillis,
                declaredLength,
                payload);
        if (!MessageDigest.isEqual(declaredDigest, actualDigest)) {
            throw corrupted(
                    "envelope digest does not match stored metadata");
        }
        return Optional.of(new ValidatedEnvelope(payload, envelope));
    }

    @Override
    public void clear() throws IOException {
        Files.deleteIfExists(artifactPath());
    }

    private void prepareRoot() throws IOException {
        Files.createDirectories(storageRoot);
        if (!Files.isDirectory(storageRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "Pending finalization storage root must be a directory");
        }
    }

    private byte[] encode(PendingFinalization pending) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(CURRENT_SCHEMA_VERSION);
            writeString(output, PAYLOAD_KIND);
            writeString(output, pending.goalId());
            writeString(output, pending.agentRunId());
            output.writeBoolean(pending.runRecordReference().isPresent());
            if (pending.runRecordReference().isPresent()) {
                writeString(
                        output,
                        pending.runRecordReference().orElseThrow());
            }
            output.writeBoolean(pending.replacementAgentRunId().isPresent());
            if (pending.replacementAgentRunId().isPresent()) {
                writeString(
                        output,
                        pending.replacementAgentRunId().orElseThrow());
            }
        }
        return bytes.toByteArray();
    }

    private PendingFinalization decodeCurrent(byte[] payload)
            throws CorruptedPendingFinalizationException {
        try (DataInputStream input =
                new DataInputStream(new ByteArrayInputStream(payload))) {
            int schemaVersion = input.readInt();
            if (schemaVersion != CURRENT_SCHEMA_VERSION) {
                throw corrupted("state schema version is unsupported");
            }
            if (!PAYLOAD_KIND.equals(readString(input))) {
                throw corrupted("state payload kind is invalid");
            }
            String goalId = readString(input);
            String agentRunId = readString(input);
            Optional<String> reference = readPresence(input)
                    ? Optional.of(readString(input))
                    : Optional.empty();
            Optional<String> replacementAgentRunId = readPresence(input)
                    ? Optional.of(readString(input))
                    : Optional.empty();
            if (input.available() != 0) {
                throw corrupted("state contains trailing bytes");
            }
            return new PendingFinalization(
                    goalId,
                    agentRunId,
                    reference,
                    replacementAgentRunId);
        } catch (CorruptedPendingFinalizationException exception) {
            throw exception;
        } catch (EOFException exception) {
            throw corrupted(
                    "state ended before all fields were read",
                    exception);
        } catch (IOException | RuntimeException exception) {
            throw corrupted("state could not be decoded", exception);
        }
    }

    private PendingFinalization decodePrevious(byte[] payload)
            throws CorruptedPendingFinalizationException {
        try (DataInputStream input =
                new DataInputStream(new ByteArrayInputStream(payload))) {
            if (input.readInt() != PREVIOUS_SCHEMA_VERSION) {
                throw corrupted("state schema version is unsupported");
            }
            if (!PAYLOAD_KIND.equals(readString(input))) {
                throw corrupted("state payload kind is invalid");
            }
            String goalId = readString(input);
            String agentRunId = readString(input);
            Optional<String> reference = readPresence(input)
                    ? Optional.of(readString(input))
                    : Optional.empty();
            if (input.available() != 0) {
                throw corrupted("state contains trailing bytes");
            }
            return new PendingFinalization(
                    goalId,
                    agentRunId,
                    reference,
                    Optional.empty());
        } catch (CorruptedPendingFinalizationException exception) {
            throw exception;
        } catch (EOFException exception) {
            throw corrupted(
                    "state ended before all fields were read",
                    exception);
        } catch (IOException | RuntimeException exception) {
            throw corrupted("state could not be decoded", exception);
        }
    }

    private int schemaVersion(byte[] payload)
            throws CorruptedPendingFinalizationException {
        if (payload.length < Integer.BYTES) {
            throw corrupted("state ended before schema version");
        }
        return ByteBuffer.wrap(payload).getInt();
    }

    private byte[] encodeEnvelope(PendingFinalization pending)
            throws IOException {
        byte[] payload = encode(pending);
        if (payload.length > MAX_STATE_BYTES) {
            throw new IOException(
                    "Pending finalization exceeds the supported size limit");
        }
        long storedAtMillis = Instant.now().toEpochMilli();
        byte[] digest = envelopeDigest(
                storedAtMillis,
                payload.length,
                payload);
        return ByteBuffer.allocate(HEADER_BYTES + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAtMillis)
                .putInt(payload.length)
                .put(digest)
                .put(payload)
                .array();
    }

    private void writeCandidate(Path candidate, byte[] envelope)
            throws IOException {
        try (FileChannel channel = FileChannel.open(
                candidate,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer remaining = ByteBuffer.wrap(envelope);
            while (remaining.hasRemaining()) {
                channel.write(remaining);
            }
            channel.force(true);
        }
    }

    private boolean readPresence(DataInputStream input)
            throws IOException {
        int value = input.readUnsignedByte();
        if (value != 0 && value != 1) {
            throw new IOException(
                    "Pending finalization optional marker is invalid");
        }
        return value == 1;
    }

    private void writeString(
            DataOutputStream output,
            String value) throws IOException {
        byte[] encoded = encodeUtf8(
                Objects.requireNonNull(value, "value must not be null"));
        if (encoded.length > MAX_STRING_BYTES) {
            throw new IOException(
                    "Pending finalization string exceeds supported bounds");
        }
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private String readString(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0
                || length > MAX_STRING_BYTES
                || length > input.available()) {
            throw new IOException(
                    "Pending finalization string length is invalid");
        }
        byte[] encoded = new byte[length];
        input.readFully(encoded);
        try {
            CharBuffer decoded = StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(encoded));
            return decoded.toString();
        } catch (CharacterCodingException exception) {
            throw new IOException(
                    "Pending finalization string is not valid UTF-8",
                    exception);
        }
    }

    private byte[] encodeUtf8(String value) throws IOException {
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8
                    .newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(value));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException exception) {
            throw new IOException(
                    "Pending finalization string is not valid Unicode text",
                    exception);
        }
    }

    private Path artifactPath() {
        return storageRoot.resolve(FILE_NAME);
    }

    private byte[] envelopeDigest(
            long storedAtMillis,
            int payloadLength,
            byte[] payload) {
        ByteBuffer digestInput = ByteBuffer.allocate(
                        Integer.BYTES
                                + Long.BYTES
                                + Integer.BYTES
                                + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAtMillis)
                .putInt(payloadLength)
                .put(payload);
        return sha256(digestInput.array());
    }

    private byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception);
        }
    }

    private CorruptedPendingFinalizationException corrupted(String reason) {
        return new CorruptedPendingFinalizationException(reason);
    }

    private CorruptedPendingFinalizationException corrupted(
            String reason,
            Throwable cause) {
        return new CorruptedPendingFinalizationException(reason, cause);
    }

    private record ValidatedEnvelope(
            byte[] payload,
            byte[] bytes) {

        private ValidatedEnvelope {
            payload = payload.clone();
            bytes = bytes.clone();
        }

        @Override
        public byte[] payload() {
            return payload.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }
}
