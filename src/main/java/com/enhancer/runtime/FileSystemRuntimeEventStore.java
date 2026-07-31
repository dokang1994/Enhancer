package com.enhancer.runtime;

import com.enhancer.io.BoundedFileOperations;
import com.enhancer.io.FileSizeLimitExceededException;
import com.enhancer.kernel.VerificationStatus;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One bounded integrity-checked append-only runtime-event artifact per Goal. Atomic publication
 * prevents partial visibility; cross-process serialization and parent-directory power-loss
 * durability are not claimed.
 */
public final class FileSystemRuntimeEventStore implements RuntimeEventStore {
    private static final int ENVELOPE_MAGIC = 0x52544531;
    private static final int DIGEST_BYTES = 32;
    static final int HEADER_BYTES =
            Integer.BYTES + Long.BYTES + Integer.BYTES + DIGEST_BYTES;
    static final int MAX_STATE_BYTES = 64 * 1024 * 1024;
    private static final int MAX_STRING_BYTES = 4 * 1024;
    private static final String PAYLOAD_KIND = "runtime-event-stream";
    private static final String FILE_SUFFIX = ".runtime-events";

    private final Path storageRoot;

    public FileSystemRuntimeEventStore(Path storageRoot) {
        Objects.requireNonNull(storageRoot, "storageRoot must not be null");
        this.storageRoot = storageRoot.toAbsolutePath().normalize();
    }

    @Override
    public synchronized RuntimeEventAppendResult append(RuntimeEvent event)
            throws IOException {
        Objects.requireNonNull(event, "event must not be null");
        prepareRoot();
        Path artifact = artifactPath(event.binding().goalId());
        if (!Files.exists(artifact, LinkOption.NOFOLLOW_LINKS)) {
            RuntimeEventStream initial =
                    RuntimeEventStream.initial(event.binding()).append(event);
            publish(initial, false);
            return RuntimeEventAppendResult.APPENDED;
        }
        RuntimeEventStream current = resolve(event.binding().goalId());
        RuntimeEventStream next;
        try {
            next = current.append(event);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new IOException(
                    "Runtime event append violates the stream contract",
                    exception);
        }
        if (next == current) {
            return RuntimeEventAppendResult.REPLAYED;
        }
        if (!next.isValidSuccessorOf(current)) {
            throw new IOException(
                    "Runtime event stream must preserve its exact prefix");
        }
        publish(next, true);
        return RuntimeEventAppendResult.APPENDED;
    }

    @Override
    public RuntimeEventStream resolve(String goalId) throws IOException {
        String canonicalGoalId = RuntimeIdentity.canonicalUuid(goalId, "goalId");
        if (Files.exists(storageRoot, LinkOption.NOFOLLOW_LINKS)
                && !Files.isDirectory(
                        storageRoot,
                        LinkOption.NOFOLLOW_LINKS)) {
            throw corrupted(
                    canonicalGoalId,
                    "storage root must be a directory without symbolic links");
        }
        Path artifact = artifactPath(canonicalGoalId);
        if (!Files.exists(artifact, LinkOption.NOFOLLOW_LINKS)) {
            throw new MissingRuntimeEventStreamException(canonicalGoalId);
        }
        if (!Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS)) {
            throw corrupted(canonicalGoalId, "artifact is not a regular file");
        }
        long size = Files.size(artifact);
        if (size < HEADER_BYTES || size > HEADER_BYTES + MAX_STATE_BYTES) {
            throw corrupted(
                    canonicalGoalId,
                    "artifact size is outside supported bounds");
        }
        byte[] envelope;
        try {
            envelope = BoundedFileOperations.readAllBytes(
                    artifact,
                    HEADER_BYTES + MAX_STATE_BYTES);
        } catch (FileSizeLimitExceededException exception) {
            throw corrupted(
                    canonicalGoalId,
                    "artifact grew outside supported bounds while reading",
                    exception);
        } catch (NoSuchFileException exception) {
            throw new MissingRuntimeEventStreamException(canonicalGoalId);
        }
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        if (buffer.getInt() != ENVELOPE_MAGIC) {
            throw corrupted(canonicalGoalId, "envelope header is invalid");
        }
        long storedAtMillis = buffer.getLong();
        int declaredLength = buffer.getInt();
        if (declaredLength < 0
                || declaredLength > MAX_STATE_BYTES
                || declaredLength != buffer.remaining() - DIGEST_BYTES) {
            throw corrupted(
                    canonicalGoalId,
                    "declared state length does not match the artifact");
        }
        byte[] declaredDigest = new byte[DIGEST_BYTES];
        buffer.get(declaredDigest);
        byte[] payload = new byte[declaredLength];
        buffer.get(payload);
        if (!MessageDigest.isEqual(
                declaredDigest,
                envelopeDigest(storedAtMillis, declaredLength, payload))) {
            throw corrupted(
                    canonicalGoalId,
                    "envelope digest does not match stored metadata");
        }
        return decode(canonicalGoalId, payload);
    }

    private void prepareRoot() throws IOException {
        Files.createDirectories(storageRoot);
        if (!Files.isDirectory(storageRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "Runtime event storage root must be a directory without symbolic links");
        }
    }

    private void publish(RuntimeEventStream state, boolean replaceExisting)
            throws IOException {
        byte[] payload = encode(state);
        if (payload.length > MAX_STATE_BYTES) {
            throw new IOException(
                    "Runtime event stream exceeds the supported size limit");
        }
        long storedAtMillis = Instant.now().toEpochMilli();
        byte[] digest = envelopeDigest(
                storedAtMillis,
                payload.length,
                payload);
        ByteBuffer envelope = ByteBuffer.allocate(HEADER_BYTES + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAtMillis)
                .putInt(payload.length)
                .put(digest)
                .put(payload);
        envelope.flip();
        Path pending = Files.createTempFile(storageRoot, ".pending-", ".tmp");
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
                if (replaceExisting) {
                    Files.move(
                            pending,
                            artifactPath(state.binding().goalId()),
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.move(
                            pending,
                            artifactPath(state.binding().goalId()),
                            StandardCopyOption.ATOMIC_MOVE);
                }
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException(
                        "Runtime event storage requires atomic move support",
                        exception);
            } catch (FileAlreadyExistsException exception) {
                throw new IOException(
                        "Runtime event stream already exists: "
                                + state.binding().goalId(),
                        exception);
            }
        } finally {
            Files.deleteIfExists(pending);
        }
    }

    private byte[] encode(RuntimeEventStream stream) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(stream.schemaVersion());
            writeString(output, PAYLOAD_KIND);
            writeBinding(output, stream.binding());
            output.writeLong(stream.revision());
            output.writeInt(stream.events().size());
            for (RuntimeEvent event : stream.events()) {
                writeString(output, event.schemaVersion());
                writeString(output, event.eventId());
                writeString(output, event.kind().name());
                output.writeLong(event.occurredAt().getEpochSecond());
                output.writeInt(event.occurredAt().getNano());
                writeBinding(output, event.binding());
                writeString(output, event.agentRunId());
                writeOptionalString(output, event.causationId());
                writeString(output, event.producerId());
                writeDetail(output, event.detail());
                output.writeInt(event.authoritativeReferences().size());
                for (RuntimeEventReference reference
                        : event.authoritativeReferences()) {
                    writeString(output, reference.kind().name());
                    writeString(output, reference.reference());
                    writeOptionalString(output, reference.sha256());
                }
            }
        }
        return bytes.toByteArray();
    }

    private RuntimeEventStream decode(String expectedGoalId, byte[] payload)
            throws CorruptedRuntimeEventStreamException {
        try (DataInputStream input =
                new DataInputStream(new ByteArrayInputStream(payload))) {
            int schemaVersion = input.readInt();
            if (schemaVersion != RuntimeEventStream.CURRENT_SCHEMA_VERSION) {
                throw corrupted(
                        expectedGoalId,
                        "state schema version is unsupported");
            }
            if (!PAYLOAD_KIND.equals(readString(input))) {
                throw corrupted(
                        expectedGoalId,
                        "state payload kind is invalid");
            }
            RuntimeEventBinding binding = readBinding(input);
            if (!expectedGoalId.equals(binding.goalId())) {
                throw corrupted(
                        expectedGoalId,
                        "state Goal identity does not match");
            }
            long revision = input.readLong();
            int count = input.readInt();
            if (count < 0 || count > RuntimeEventStream.MAX_EVENTS) {
                throw corrupted(
                        expectedGoalId,
                        "state event count is invalid");
            }
            List<RuntimeEvent> events = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                String eventSchema = readString(input);
                String eventId = readString(input);
                RuntimeEventKind kind = RuntimeEventKind.valueOf(
                        readString(input));
                Instant occurredAt = Instant.ofEpochSecond(
                        input.readLong(),
                        input.readInt());
                RuntimeEventBinding eventBinding = readBinding(input);
                String agentRunId = readString(input);
                Optional<String> causationId = readOptionalString(input);
                String producerId = readString(input);
                RuntimeEventDetail detail = readDetail(input, kind);
                int referenceCount = input.readInt();
                if (referenceCount < RuntimeEvent.MIN_REFERENCES
                        || referenceCount > RuntimeEvent.MAX_REFERENCES) {
                    throw corrupted(
                            expectedGoalId,
                            "event authoritative-reference count is invalid");
                }
                List<RuntimeEventReference> references = new ArrayList<>();
                for (int referenceIndex = 0;
                        referenceIndex < referenceCount;
                        referenceIndex++) {
                    references.add(new RuntimeEventReference(
                            RuntimeEventReferenceKind.valueOf(
                                    readString(input)),
                            readString(input),
                            readOptionalString(input)));
                }
                events.add(new RuntimeEvent(
                        eventSchema,
                        eventId,
                        kind,
                        occurredAt,
                        eventBinding,
                        agentRunId,
                        causationId,
                        producerId,
                        detail,
                        references));
            }
            if (input.available() != 0) {
                throw corrupted(
                        expectedGoalId,
                        "state contains trailing bytes");
            }
            return new RuntimeEventStream(
                    schemaVersion,
                    binding,
                    revision,
                    events);
        } catch (CorruptedRuntimeEventStreamException exception) {
            throw exception;
        } catch (EOFException exception) {
            throw corrupted(
                    expectedGoalId,
                    "state ended before all fields were read",
                    exception);
        } catch (IOException | RuntimeException exception) {
            throw corrupted(
                    expectedGoalId,
                    "state could not be decoded",
                    exception);
        }
    }

    private void writeBinding(
            DataOutputStream output,
            RuntimeEventBinding binding) throws IOException {
        writeString(output, binding.goalId());
        writeString(output, binding.workItemId());
        writeString(output, binding.taskRevision().taskId());
        writeString(output, binding.taskRevision().sourceDocument());
        writeString(output, binding.taskRevision().sourceSha256());
        writeString(output, binding.snapshotId());
        writeString(output, binding.logicalRunId());
        writeString(output, binding.correlationId());
    }

    private RuntimeEventBinding readBinding(DataInputStream input)
            throws IOException {
        return new RuntimeEventBinding(
                readString(input),
                readString(input),
                new ApprovedTaskRevision(
                        readString(input),
                        readString(input),
                        readString(input)),
                readString(input),
                readString(input),
                readString(input));
    }

    private void writeDetail(
            DataOutputStream output,
            RuntimeEventDetail detail) throws IOException {
        if (detail instanceof RuntimeEventDetail.RetryDecisionRecorded value) {
            output.writeBoolean(value.admitted());
            writeOptionalString(
                    output,
                    value.refusalReason().map(Enum::name));
        } else if (detail instanceof RuntimeEventDetail.RetryStarted value) {
            writeString(output, value.previousAgentRunId());
        } else if (detail
                instanceof RuntimeEventDetail.StagnationDetected value) {
            output.writeInt(value.iterations());
            output.writeInt(value.threshold());
        } else if (detail
                instanceof RuntimeEventDetail.TimeoutDetected value) {
            writeString(output, value.timeoutKind().name());
        } else if (detail
                instanceof RuntimeEventDetail.CancellationRequestRecorded value) {
            writeString(output, value.controlMessageId());
        } else if (detail
                instanceof RuntimeEventDetail.CancellationApplied value) {
            writeString(output, value.controlMessageId());
        } else if (detail
                instanceof RuntimeEventDetail.VerificationRecorded value) {
            writeString(output, value.status().name());
        } else if (detail
                instanceof RuntimeEventDetail.WorkItemTerminated value) {
            writeString(output, value.disposition().name());
        } else {
            throw new IOException("Runtime event detail kind is unsupported");
        }
    }

    private RuntimeEventDetail readDetail(
            DataInputStream input,
            RuntimeEventKind kind) throws IOException {
        return switch (kind) {
            case RETRY_DECISION_RECORDED ->
                    new RuntimeEventDetail.RetryDecisionRecorded(
                            input.readBoolean(),
                            readOptionalString(input).map(
                                    AgentRunRetryRefusalReason::valueOf));
            case RETRY_STARTED ->
                    new RuntimeEventDetail.RetryStarted(readString(input));
            case STAGNATION_DETECTED ->
                    new RuntimeEventDetail.StagnationDetected(
                            input.readInt(),
                            input.readInt());
            case TIMEOUT_DETECTED ->
                    new RuntimeEventDetail.TimeoutDetected(
                            RuntimeTimeoutKind.valueOf(readString(input)));
            case CANCELLATION_REQUEST_RECORDED ->
                    new RuntimeEventDetail.CancellationRequestRecorded(
                            readString(input));
            case CANCELLATION_APPLIED ->
                    new RuntimeEventDetail.CancellationApplied(
                            readString(input));
            case VERIFICATION_RECORDED ->
                    new RuntimeEventDetail.VerificationRecorded(
                            VerificationStatus.valueOf(readString(input)));
            case WORK_ITEM_TERMINATED ->
                    new RuntimeEventDetail.WorkItemTerminated(
                            WorkItemDisposition.valueOf(readString(input)));
        };
    }

    private void writeOptionalString(
            DataOutputStream output,
            Optional<String> value) throws IOException {
        Objects.requireNonNull(value, "optional value must not be null");
        output.writeBoolean(value.isPresent());
        if (value.isPresent()) {
            writeString(output, value.orElseThrow());
        }
    }

    private Optional<String> readOptionalString(DataInputStream input)
            throws IOException {
        return input.readBoolean()
                ? Optional.of(readString(input))
                : Optional.empty();
    }

    private void writeString(DataOutputStream output, String value)
            throws IOException {
        byte[] encoded = encodeUtf8(
                Objects.requireNonNull(value, "value must not be null"));
        if (encoded.length > MAX_STRING_BYTES) {
            throw new IOException(
                    "Runtime event string exceeds supported bounds");
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
                    "Runtime event string length is invalid");
        }
        byte[] encoded = new byte[length];
        input.readFully(encoded);
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(encoded))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IOException(
                    "Runtime event string is not valid UTF-8",
                    exception);
        }
    }

    private byte[] encodeUtf8(String value) throws IOException {
        try {
            CharBuffer characters = CharBuffer.wrap(value);
            ByteBuffer encoded = StandardCharsets.UTF_8
                    .newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(characters);
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException exception) {
            throw new IOException(
                    "Runtime event string is not valid Unicode text",
                    exception);
        }
    }

    private Path artifactPath(String goalId) {
        Path artifact = storageRoot.resolve(goalId + FILE_SUFFIX).normalize();
        if (!artifact.startsWith(storageRoot)) {
            throw new IllegalArgumentException(
                    "Runtime event Goal identity resolves outside storage");
        }
        return artifact;
    }

    private byte[] envelopeDigest(
            long storedAtMillis,
            int payloadLength,
            byte[] payload) {
        return sha256(ByteBuffer.allocate(
                        Integer.BYTES
                                + Long.BYTES
                                + Integer.BYTES
                                + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAtMillis)
                .putInt(payloadLength)
                .put(payload)
                .array());
    }

    private byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception);
        }
    }

    private CorruptedRuntimeEventStreamException corrupted(
            String goalId,
            String reason) {
        return new CorruptedRuntimeEventStreamException(
                "corrupted runtime event stream " + goalId + ": " + reason);
    }

    private CorruptedRuntimeEventStreamException corrupted(
            String goalId,
            String reason,
            Throwable cause) {
        return new CorruptedRuntimeEventStreamException(
                "corrupted runtime event stream " + goalId + ": " + reason,
                cause);
    }
}
