package com.enhancer.runtime;

import com.enhancer.io.BoundedFileOperations;
import com.enhancer.io.FileSizeLimitExceededException;
import com.enhancer.workspace.ApprovedTaskRevision;
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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/** Bounded integrity-checked filesystem adapter for deterministic process-timeout points. */
public final class FileSystemProcessTimeoutFactStore implements ProcessTimeoutFactStore {
    private static final int ENVELOPE_MAGIC = 0x50544631;
    private static final int DIGEST_BYTES = 32;
    static final int HEADER_BYTES = Integer.BYTES + Integer.BYTES + DIGEST_BYTES;
    static final int MAX_STATE_BYTES = 16 * 1024;
    private static final int MAX_STRING_BYTES = 4 * 1024;
    private static final int SCHEMA_VERSION = 1;
    private static final String PAYLOAD_KIND = "process-timeout-fact";
    private static final String FILE_SUFFIX = ".process-timeout";

    private final Path storageRoot;

    public FileSystemProcessTimeoutFactStore(Path storageRoot) {
        this.storageRoot = Objects.requireNonNull(
                storageRoot, "storageRoot must not be null")
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public ResolvedProcessTimeoutFact persist(ProcessTimeoutFact fact) throws IOException {
        ProcessTimeoutFact checked = Objects.requireNonNull(fact, "fact must not be null");
        Path artifact = artifactPath(checked.binding().goalId(), checked.agentRunId());
        Optional<ResolvedProcessTimeoutFact> existing = readIfPresent(artifact);
        if (existing.isPresent()) {
            ResolvedProcessTimeoutFact resolved = existing.orElseThrow();
            if (!resolved.fact().equals(checked)) {
                throw new IOException(
                        "process timeout identity is already bound to different content");
            }
            return resolved;
        }

        Path goalRoot = prepareGoalRoot(checked.binding().goalId());
        byte[] payload = encode(checked);
        if (payload.length > MAX_STATE_BYTES) {
            throw new IOException("process timeout fact exceeds the supported size limit");
        }
        byte[] digest = sha256(payload);
        ByteBuffer envelope = ByteBuffer.allocate(HEADER_BYTES + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putInt(payload.length)
                .put(digest)
                .put(payload);
        envelope.flip();
        Path candidate = Files.createTempFile(goalRoot, ".process-timeout-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(
                    candidate,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                while (envelope.hasRemaining()) {
                    channel.write(envelope);
                }
                channel.force(true);
            }
            try {
                Files.move(candidate, artifact, StandardCopyOption.ATOMIC_MOVE);
            } catch (FileAlreadyExistsException raced) {
                ResolvedProcessTimeoutFact resolved = resolve(checked.reference());
                if (!resolved.fact().equals(checked)) {
                    throw new IOException(
                            "process timeout identity was concurrently reused", raced);
                }
                return resolved;
            } catch (AtomicMoveNotSupportedException unsupported) {
                throw new IOException(
                        "process timeout storage requires atomic move support", unsupported);
            }
        } finally {
            Files.deleteIfExists(candidate);
        }
        return resolve(checked.reference());
    }

    @Override
    public Optional<ResolvedProcessTimeoutFact> find(String goalId, String agentRunId)
            throws IOException {
        String canonicalGoalId = RuntimeIdentity.canonicalUuid(goalId, "goalId");
        String canonicalAgentRunId = RuntimeIdentity.canonicalUuid(
                agentRunId, "agentRunId");
        Optional<ResolvedProcessTimeoutFact> found = readIfPresent(
                artifactPath(canonicalGoalId, canonicalAgentRunId));
        if (found.isPresent()
                && !found.orElseThrow().reference().equals(
                        ProcessTimeoutFact.reference(canonicalGoalId, canonicalAgentRunId))) {
            throw corrupted("artifact binding does not match its point");
        }
        return found;
    }

    @Override
    public ResolvedProcessTimeoutFact resolve(String reference) throws IOException {
        Identity identity = parseReference(reference);
        return find(identity.goalId(), identity.agentRunId())
                .orElseThrow(() -> new MissingProcessTimeoutFactException(reference));
    }

    private Optional<ResolvedProcessTimeoutFact> readIfPresent(Path artifact)
            throws IOException {
        rejectSymbolicRoot();
        if (Files.isSymbolicLink(artifact.getParent())) {
            throw corrupted("Goal storage root must not be symbolic");
        }
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
                    artifact, HEADER_BYTES + MAX_STATE_BYTES);
        } catch (FileSizeLimitExceededException exception) {
            throw corrupted("artifact grew outside supported bounds", exception);
        } catch (NoSuchFileException missing) {
            return Optional.empty();
        }
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        if (buffer.getInt() != ENVELOPE_MAGIC) {
            throw corrupted("envelope header is invalid");
        }
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
        byte[] actualDigest = sha256(payload);
        if (!MessageDigest.isEqual(declaredDigest, actualDigest)) {
            throw corrupted("envelope digest does not match its payload");
        }
        ProcessTimeoutFact fact = decode(payload);
        if (!artifact.equals(artifactPath(fact.binding().goalId(), fact.agentRunId()))) {
            throw corrupted("artifact path does not match its fact binding");
        }
        return Optional.of(resolved(fact, actualDigest));
    }

    private Path prepareGoalRoot(String goalId) throws IOException {
        rejectSymbolicRoot();
        Files.createDirectories(storageRoot);
        if (!Files.isDirectory(storageRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("process timeout storage root must be a directory");
        }
        Path goalRoot = storageRoot.resolve(goalId);
        if (Files.isSymbolicLink(goalRoot)) {
            throw new IOException("process timeout Goal root must not be symbolic");
        }
        Files.createDirectories(goalRoot);
        if (!Files.isDirectory(goalRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("process timeout Goal root must be a directory");
        }
        return goalRoot;
    }

    private void rejectSymbolicRoot() throws IOException {
        if (Files.isSymbolicLink(storageRoot)) {
            throw new IOException("process timeout storage root must not be symbolic");
        }
    }

    private Path artifactPath(String goalId, String agentRunId) {
        return storageRoot.resolve(goalId).resolve(agentRunId + FILE_SUFFIX);
    }

    private byte[] encode(ProcessTimeoutFact fact) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(SCHEMA_VERSION);
            writeString(output, PAYLOAD_KIND);
            writeString(output, fact.schemaVersion());
            output.writeLong(fact.occurredAt().getEpochSecond());
            output.writeInt(fact.occurredAt().getNano());
            writeBinding(output, fact.binding());
            writeString(output, fact.agentRunId());
            output.writeLong(fact.timeout().getSeconds());
            output.writeInt(fact.timeout().getNano());
            writeString(output, fact.reason());
        }
        return bytes.toByteArray();
    }

    private ProcessTimeoutFact decode(byte[] payload) throws IOException {
        try (DataInputStream input =
                new DataInputStream(new ByteArrayInputStream(payload))) {
            if (input.readInt() != SCHEMA_VERSION) {
                throw corrupted("state schema version is unsupported");
            }
            if (!PAYLOAD_KIND.equals(readString(input))) {
                throw corrupted("state payload kind is invalid");
            }
            String factSchema = readString(input);
            Instant occurredAt = Instant.ofEpochSecond(input.readLong(), input.readInt());
            RuntimeEventBinding binding = readBinding(input);
            String agentRunId = readString(input);
            Duration timeout = Duration.ofSeconds(input.readLong(), input.readInt());
            String reason = readString(input);
            if (input.available() != 0) {
                throw corrupted("state contains trailing bytes");
            }
            return new ProcessTimeoutFact(
                    factSchema, occurredAt, binding, agentRunId, timeout, reason);
        } catch (CorruptedProcessTimeoutFactException exception) {
            throw exception;
        } catch (EOFException exception) {
            throw corrupted("state ended before all fields were read", exception);
        } catch (IOException | RuntimeException exception) {
            throw corrupted("state could not be decoded", exception);
        }
    }

    private void writeBinding(DataOutputStream output, RuntimeEventBinding binding)
            throws IOException {
        writeString(output, binding.goalId());
        writeString(output, binding.workItemId());
        writeString(output, binding.taskRevision().taskId());
        writeString(output, binding.taskRevision().sourceDocument());
        writeString(output, binding.taskRevision().sourceSha256());
        writeString(output, binding.snapshotId());
        writeString(output, binding.logicalRunId());
        writeString(output, binding.correlationId());
    }

    private RuntimeEventBinding readBinding(DataInputStream input) throws IOException {
        return new RuntimeEventBinding(
                readString(input),
                readString(input),
                new ApprovedTaskRevision(
                        readString(input), readString(input), readString(input)),
                readString(input),
                readString(input),
                readString(input));
    }

    private void writeString(DataOutputStream output, String value) throws IOException {
        byte[] encoded = encodeUtf8(Objects.requireNonNull(value, "value must not be null"));
        if (encoded.length > MAX_STRING_BYTES) {
            throw new IOException("process timeout string exceeds supported bounds");
        }
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private String readString(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_STRING_BYTES || length > input.available()) {
            throw new IOException("process timeout string length is invalid");
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
            throw new IOException("process timeout string is not valid UTF-8", exception);
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
            throw new IOException("process timeout string is not valid Unicode text", exception);
        }
    }

    private ResolvedProcessTimeoutFact resolved(ProcessTimeoutFact fact, byte[] digest) {
        return new ResolvedProcessTimeoutFact(
                fact, fact.reference(), HexFormat.of().formatHex(digest));
    }

    private Identity parseReference(String reference) throws IOException {
        String checked = Objects.requireNonNull(reference, "reference must not be null");
        String[] parts = checked.split("/", -1);
        if (parts.length != 3 || !"process-timeout".equals(parts[0])) {
            throw new IOException("process timeout reference is invalid");
        }
        try {
            return new Identity(
                    RuntimeIdentity.canonicalUuid(parts[1], "goalId"),
                    RuntimeIdentity.canonicalUuid(parts[2], "agentRunId"));
        } catch (IllegalArgumentException exception) {
            throw new IOException("process timeout reference is invalid", exception);
        }
    }

    private byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private CorruptedProcessTimeoutFactException corrupted(String reason) {
        return new CorruptedProcessTimeoutFactException(reason);
    }

    private CorruptedProcessTimeoutFactException corrupted(
            String reason, Throwable cause) {
        return new CorruptedProcessTimeoutFactException(reason, cause);
    }

    private record Identity(String goalId, String agentRunId) {
    }
}
