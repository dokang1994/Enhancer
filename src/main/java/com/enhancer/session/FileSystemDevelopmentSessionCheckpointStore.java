package com.enhancer.session;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Single-checkpoint filesystem store with bounded integrity validation and atomic publication.
 * Parent-directory power-loss durability is not claimed.
 */
public final class FileSystemDevelopmentSessionCheckpointStore {
    private static final int ENVELOPE_MAGIC = 0x44534331;
    private static final int DIGEST_BYTES = 32;
    private static final int HEADER_BYTES =
            Integer.BYTES + Long.BYTES + Integer.BYTES + DIGEST_BYTES;
    private static final int MAX_STATE_BYTES = 512 * 1024;
    private static final int MAX_STRING_BYTES = 16 * 1024;
    private static final int CURRENT_SCHEMA_VERSION = 1;
    private static final String PAYLOAD_KIND = "development-session-checkpoint";
    static final String FILE_NAME = "session.checkpoint";

    private final Path storageRoot;

    public FileSystemDevelopmentSessionCheckpointStore(Path storageRoot) {
        Objects.requireNonNull(storageRoot, "storageRoot must not be null");
        this.storageRoot = storageRoot.toAbsolutePath().normalize();
    }

    public Optional<DevelopmentSessionCheckpoint> find() throws IOException {
        if (!hasReadableStorageRoot()) {
            return Optional.empty();
        }
        Path artifact = artifactPath();
        if (!Files.exists(artifact, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        if (!Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS)) {
            throw corrupted("artifact is not a regular file");
        }
        long size = Files.size(artifact);
        if (size < HEADER_BYTES || size > HEADER_BYTES + MAX_STATE_BYTES) {
            throw corrupted("artifact size is outside supported bounds");
        }
        byte[] envelope;
        try {
            envelope = BoundedFileOperations.readAllBytes(
                    artifact,
                    HEADER_BYTES + MAX_STATE_BYTES);
        } catch (FileSizeLimitExceededException exception) {
            throw corrupted("artifact grew outside supported bounds while reading", exception);
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
            throw corrupted("declared state length does not match the artifact");
        }
        byte[] declaredDigest = new byte[DIGEST_BYTES];
        buffer.get(declaredDigest);
        byte[] payload = new byte[declaredLength];
        buffer.get(payload);
        byte[] actualDigest = envelopeDigest(storedAtMillis, declaredLength, payload);
        if (!MessageDigest.isEqual(declaredDigest, actualDigest)) {
            throw corrupted("envelope digest does not match stored metadata");
        }
        return Optional.of(decode(payload));
    }

    public void create(DevelopmentSessionCheckpoint checkpoint) throws IOException {
        Objects.requireNonNull(checkpoint, "checkpoint must not be null");
        if (checkpoint.revision() != 1) {
            throw new IllegalArgumentException("a new checkpoint must start at revision 1");
        }
        prepareRoot();
        if (Files.exists(artifactPath(), LinkOption.NOFOLLOW_LINKS)) {
            throw new DevelopmentSessionCheckpointConflictException(
                    "an active development-session checkpoint already exists");
        }
        publish(checkpoint, false);
    }

    public void update(
            String runId,
            long expectedRevision,
            DevelopmentSessionCheckpoint checkpoint) throws IOException {
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(checkpoint, "checkpoint must not be null");
        DevelopmentSessionCheckpoint current = find().orElseThrow(() ->
                new DevelopmentSessionCheckpointConflictException(
                        "no active development-session checkpoint exists"));
        requireWriter(current, runId, expectedRevision);
        if (!checkpoint.runId().equals(current.runId())
                || !checkpoint.taskId().equals(current.taskId())
                || !checkpoint.taskContractSha256().equals(current.taskContractSha256())
                || checkpoint.revision() != current.revision() + 1) {
            throw new DevelopmentSessionCheckpointConflictException(
                    "checkpoint update does not extend the current run by one revision");
        }
        prepareRoot();
        publish(checkpoint, true);
    }

    public void clear(String runId, long expectedRevision) throws IOException {
        DevelopmentSessionCheckpoint current = find().orElseThrow(() ->
                new DevelopmentSessionCheckpointConflictException(
                        "no active development-session checkpoint exists"));
        requireWriter(current, runId, expectedRevision);
        Files.delete(artifactPath());
    }

    private void requireWriter(
            DevelopmentSessionCheckpoint current,
            String runId,
            long expectedRevision)
            throws DevelopmentSessionCheckpointConflictException {
        if (!current.runId().equals(runId)) {
            throw new DevelopmentSessionCheckpointConflictException(
                    "checkpoint belongs to a different run");
        }
        if (current.revision() != expectedRevision) {
            throw new DevelopmentSessionCheckpointConflictException(
                    "checkpoint revision is stale");
        }
    }

    private void prepareRoot() throws IOException {
        Path parent = storageRoot.getParent();
        if (parent == null) {
            throw new IOException("checkpoint storage root requires a parent directory");
        }
        Files.createDirectories(parent);
        if (!isExactRealDirectory(parent)) {
            throw new IOException("checkpoint parent must be a non-symbolic-link directory");
        }
        if (Files.exists(storageRoot, LinkOption.NOFOLLOW_LINKS)) {
            if (!isExactRealDirectory(storageRoot)) {
                throw new IOException("checkpoint storage root must be a directory");
            }
        } else {
            Files.createDirectory(storageRoot);
        }
    }

    private boolean hasReadableStorageRoot() throws IOException {
        Path parent = storageRoot.getParent();
        if (parent == null) {
            throw corrupted("checkpoint storage root requires a parent directory");
        }
        if (!Files.exists(parent, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        if (!isExactRealDirectory(parent)) {
            throw corrupted("checkpoint parent must be a non-symbolic-link directory");
        }
        if (!Files.exists(storageRoot, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        if (!isExactRealDirectory(storageRoot)) {
            throw corrupted("checkpoint storage root must be a directory");
        }
        return true;
    }

    private boolean isExactRealDirectory(Path path) throws IOException {
        return Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
                && path.toRealPath().equals(path.toAbsolutePath().normalize());
    }

    private void publish(
            DevelopmentSessionCheckpoint checkpoint,
            boolean replace) throws IOException {
        byte[] payload = encode(checkpoint);
        if (payload.length > MAX_STATE_BYTES) {
            throw new IOException("development-session checkpoint exceeds size limit");
        }
        long storedAtMillis = Instant.now().toEpochMilli();
        byte[] digest = envelopeDigest(storedAtMillis, payload.length, payload);
        ByteBuffer envelope = ByteBuffer.allocate(HEADER_BYTES + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAtMillis)
                .putInt(payload.length)
                .put(digest)
                .put(payload);
        envelope.flip();

        Path pending = Files.createTempFile(storageRoot, ".session-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(
                    pending,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                while (envelope.hasRemaining()) {
                    channel.write(envelope);
                }
                channel.force(true);
            }
            try {
                if (replace) {
                    Files.move(
                            pending,
                            artifactPath(),
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.move(
                            pending,
                            artifactPath(),
                            StandardCopyOption.ATOMIC_MOVE);
                }
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException(
                        "development-session checkpoint requires atomic move support",
                        exception);
            }
        } finally {
            Files.deleteIfExists(pending);
        }
    }

    private byte[] encode(DevelopmentSessionCheckpoint checkpoint) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(CURRENT_SCHEMA_VERSION);
            writeString(output, PAYLOAD_KIND);
            writeString(output, checkpoint.runId());
            writeString(output, checkpoint.taskId());
            writeString(output, checkpoint.taskContractSha256());
            output.writeLong(checkpoint.revision());
            writeString(output, checkpoint.state().name());
            writeString(output, checkpoint.currentStep());
            output.writeBoolean(checkpoint.lastSuccessfulStep().isPresent());
            if (checkpoint.lastSuccessfulStep().isPresent()) {
                writeString(output, checkpoint.lastSuccessfulStep().orElseThrow());
            }
            writeString(output, checkpoint.nextAction());
            output.writeInt(checkpoint.evidenceReferences().size());
            for (String reference : checkpoint.evidenceReferences()) {
                writeString(output, reference);
            }
            output.writeInt(checkpoint.artifacts().size());
            for (DevelopmentSessionArtifact artifact : checkpoint.artifacts()) {
                writeString(output, artifact.path());
                output.writeBoolean(artifact.present());
                if (artifact.present()) {
                    writeString(output, artifact.contentSha256().orElseThrow());
                }
            }
        }
        return bytes.toByteArray();
    }

    private DevelopmentSessionCheckpoint decode(byte[] payload)
            throws CorruptedDevelopmentSessionCheckpointException {
        try (DataInputStream input =
                new DataInputStream(new ByteArrayInputStream(payload))) {
            if (input.readInt() != CURRENT_SCHEMA_VERSION) {
                throw corrupted("state schema version is unsupported");
            }
            if (!PAYLOAD_KIND.equals(readString(input))) {
                throw corrupted("state payload kind is invalid");
            }
            String runId = readString(input);
            String taskId = readString(input);
            String taskDigest = readString(input);
            long revision = input.readLong();
            DevelopmentSessionCheckpointState state =
                    DevelopmentSessionCheckpointState.valueOf(readString(input));
            String currentStep = readString(input);
            Optional<String> lastSuccessful = readPresence(input)
                    ? Optional.of(readString(input))
                    : Optional.empty();
            String nextAction = readString(input);
            int evidenceCount = boundedCount(
                    input.readInt(),
                    DevelopmentSessionCheckpoint.MAX_EVIDENCE_REFERENCES,
                    "evidence reference");
            List<String> evidence = new ArrayList<>();
            for (int index = 0; index < evidenceCount; index++) {
                evidence.add(readString(input));
            }
            int artifactCount = boundedCount(
                    input.readInt(),
                    DevelopmentSessionCheckpoint.MAX_ARTIFACTS,
                    "artifact");
            List<DevelopmentSessionArtifact> artifacts = new ArrayList<>();
            for (int index = 0; index < artifactCount; index++) {
                String path = readString(input);
                boolean present = readPresence(input);
                artifacts.add(new DevelopmentSessionArtifact(
                        path,
                        present,
                        present ? Optional.of(readString(input)) : Optional.empty()));
            }
            if (input.available() != 0) {
                throw corrupted("state contains trailing bytes");
            }
            return new DevelopmentSessionCheckpoint(
                    runId,
                    taskId,
                    taskDigest,
                    revision,
                    state,
                    currentStep,
                    lastSuccessful,
                    nextAction,
                    evidence,
                    artifacts);
        } catch (CorruptedDevelopmentSessionCheckpointException exception) {
            throw exception;
        } catch (EOFException exception) {
            throw corrupted("state ended before all fields were read", exception);
        } catch (IOException | RuntimeException exception) {
            throw corrupted("state could not be decoded", exception);
        }
    }

    private int boundedCount(int count, int maximum, String name) throws IOException {
        if (count < 0 || count > maximum) {
            throw new IOException(name + " count is invalid");
        }
        return count;
    }

    private boolean readPresence(DataInputStream input) throws IOException {
        int value = input.readUnsignedByte();
        if (value != 0 && value != 1) {
            throw new IOException("checkpoint boolean marker is invalid");
        }
        return value == 1;
    }

    private void writeString(DataOutputStream output, String value) throws IOException {
        byte[] encoded = encodeUtf8(Objects.requireNonNull(value, "value must not be null"));
        if (encoded.length > MAX_STRING_BYTES) {
            throw new IOException("checkpoint string exceeds supported bounds");
        }
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private String readString(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_STRING_BYTES || length > input.available()) {
            throw new IOException("checkpoint string length is invalid");
        }
        byte[] encoded = new byte[length];
        input.readFully(encoded);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(encoded))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IOException("checkpoint string is not valid UTF-8", exception);
        }
    }

    private byte[] encodeUtf8(String value) throws IOException {
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(value));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException exception) {
            throw new IOException("checkpoint string is not valid Unicode text", exception);
        }
    }

    private byte[] envelopeDigest(long storedAtMillis, int length, byte[] payload) {
        ByteBuffer input = ByteBuffer.allocate(
                        Integer.BYTES + Long.BYTES + Integer.BYTES + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAtMillis)
                .putInt(length)
                .put(payload);
        return sha256(input.array());
    }

    private byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private Path artifactPath() {
        return storageRoot.resolve(FILE_NAME);
    }

    private CorruptedDevelopmentSessionCheckpointException corrupted(String reason) {
        return new CorruptedDevelopmentSessionCheckpointException(reason);
    }

    private CorruptedDevelopmentSessionCheckpointException corrupted(
            String reason,
            Throwable cause) {
        return new CorruptedDevelopmentSessionCheckpointException(reason, cause);
    }
}
