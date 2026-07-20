package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ResultPayload;
import com.enhancer.bus.WorkPayload;
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
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * One-current-state filesystem adapter for a Goal and its schema-v1 AgentRun. Atomic
 * publication prevents partial visibility; parent-directory power-loss durability is not
 * claimed.
 */
public final class FileSystemAgentRuntimeStateStore
        implements AgentRuntimeStateStore {
    private static final int ENVELOPE_MAGIC = 0x45415231;
    private static final int DIGEST_BYTES = 32;
    static final int HEADER_BYTES =
            Integer.BYTES + Long.BYTES + Integer.BYTES + DIGEST_BYTES;
    static final int MAX_STATE_BYTES = 4 * 1024 * 1024;
    private static final int MAX_STRING_BYTES = 1024 * 1024;
    private static final int MAX_COLLECTION_ITEMS = 256;
    private static final String FILE_SUFFIX = ".agent-runtime";
    private static final String PAYLOAD_KIND = "agent-runtime-state";
    private static final String WORK_PAYLOAD_KIND = "work";
    private static final String RESULT_PAYLOAD_KIND = "result";

    private final Path storageRoot;

    public FileSystemAgentRuntimeStateStore(Path storageRoot) {
        Objects.requireNonNull(storageRoot, "storageRoot must not be null");
        this.storageRoot = storageRoot.toAbsolutePath().normalize();
    }

    @Override
    public void create(AgentRuntimeState initialState) throws IOException {
        Objects.requireNonNull(
                initialState, "initialState must not be null");
        if (initialState.revision() != 0) {
            throw new IllegalArgumentException(
                    "initialState revision must be zero");
        }
        prepareRoot();
        if (Files.exists(
                artifactPath(initialState.goal().goalId()),
                LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "Agent runtime state already exists: "
                            + initialState.goal().goalId());
        }
        publish(initialState, false);
    }

    @Override
    public void update(AgentRuntimeState nextState) throws IOException {
        Objects.requireNonNull(nextState, "nextState must not be null");
        AgentRuntimeState current =
                resolve(nextState.goal().goalId());
        if (nextState.revision() != current.revision() + 1) {
            throw new IOException(
                    "Agent runtime revision must advance exactly one");
        }
        if (!nextState.goal().workItem().equals(
                current.goal().workItem())) {
            throw new IOException(
                    "Agent runtime WorkItem must not change");
        }
        long currentFence = current.lastIssuedFenceToken();
        long nextFence = nextState.lastIssuedFenceToken();
        if (nextFence < currentFence
                || nextFence - currentFence > 1) {
            throw new IOException(
                    "Agent runtime fence token must stay current or advance exactly one");
        }
        publish(nextState, true);
    }

    @Override
    public AgentRuntimeState resolve(String goalId) throws IOException {
        String canonicalGoalId =
                AgentRuntimeState.requireCanonicalGoalId(goalId);
        Path artifact = artifactPath(canonicalGoalId);
        if (!Files.exists(artifact, LinkOption.NOFOLLOW_LINKS)) {
            throw new MissingAgentRuntimeStateException(canonicalGoalId);
        }
        if (!Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS)) {
            throw corrupted(
                    canonicalGoalId,
                    "artifact is not a regular file");
        }
        long artifactSize = Files.size(artifact);
        if (artifactSize < HEADER_BYTES
                || artifactSize > HEADER_BYTES + MAX_STATE_BYTES) {
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
                    "artifact grew outside supported bounds while reading");
        } catch (NoSuchFileException exception) {
            throw new MissingAgentRuntimeStateException(canonicalGoalId);
        }
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        if (buffer.getInt() != ENVELOPE_MAGIC) {
            throw corrupted(
                    canonicalGoalId,
                    "envelope header is invalid");
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
        byte[] actualDigest = envelopeDigest(
                storedAtMillis,
                declaredLength,
                payload);
        if (!MessageDigest.isEqual(declaredDigest, actualDigest)) {
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
                    "Agent runtime storage root must be a directory");
        }
    }

    private void publish(
            AgentRuntimeState state,
            boolean replaceExisting) throws IOException {
        byte[] payload = encode(state);
        if (payload.length > MAX_STATE_BYTES) {
            throw new IOException(
                    "Agent runtime state exceeds the supported size limit");
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

        Path pending = Files.createTempFile(
                storageRoot, ".pending-", ".tmp");
        Path destination = artifactPath(state.goal().goalId());
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
                            destination,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.move(
                            pending,
                            destination,
                            StandardCopyOption.ATOMIC_MOVE);
                }
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException(
                        "Agent runtime storage requires atomic move support",
                        exception);
            } catch (FileAlreadyExistsException exception) {
                throw new IOException(
                        "Agent runtime state already exists: "
                                + state.goal().goalId(),
                        exception);
            }
        } finally {
            Files.deleteIfExists(pending);
        }
    }

    private byte[] encode(AgentRuntimeState state) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(state.schemaVersion());
            writeString(output, PAYLOAD_KIND);
            writeString(output, state.goal().goalId());
            output.writeLong(state.revision());
            output.writeLong(state.lastIssuedFenceToken());
            writeString(output, state.goal().status().name());
            writeWorkItem(output, state.goal().workItem());
            output.writeBoolean(state.agentRun().isPresent());
            if (state.agentRun().isPresent()) {
                writeAgentRun(output, state.agentRun().orElseThrow());
            }
        }
        return bytes.toByteArray();
    }

    private AgentRuntimeState decode(
            String expectedGoalId,
            byte[] payload) throws CorruptedAgentRuntimeStateException {
        try (DataInputStream input =
                new DataInputStream(new ByteArrayInputStream(payload))) {
            int schemaVersion = input.readInt();
            if (schemaVersion
                    != AgentRuntimeState.CURRENT_SCHEMA_VERSION) {
                throw corrupted(
                        expectedGoalId,
                        "state schema version is unsupported");
            }
            if (!PAYLOAD_KIND.equals(readString(input))) {
                throw corrupted(
                        expectedGoalId,
                        "state payload kind is invalid");
            }
            String goalId = readString(input);
            if (!expectedGoalId.equals(goalId)) {
                throw corrupted(
                        expectedGoalId,
                        "state Goal identity does not match artifact");
            }
            long revision = input.readLong();
            long lastIssuedFenceToken = input.readLong();
            RuntimeGoalStatus goalStatus =
                    readEnum(input, RuntimeGoalStatus.class, "Goal status");
            WorkItem workItem = readWorkItem(input);
            Optional<RuntimeAgentRun> agentRun = readPresence(input)
                    ? Optional.of(readAgentRun(input))
                    : Optional.empty();
            if (input.available() != 0) {
                throw corrupted(
                        expectedGoalId,
                        "state contains trailing bytes");
            }
            return new AgentRuntimeState(
                    schemaVersion,
                    revision,
                    lastIssuedFenceToken,
                    new RuntimeGoal(goalId, workItem, goalStatus),
                    agentRun);
        } catch (CorruptedAgentRuntimeStateException exception) {
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

    private void writeAgentRun(
            DataOutputStream output,
            RuntimeAgentRun agentRun) throws IOException {
        writeString(output, agentRun.agentRunId());
        writeString(output, agentRun.goalId());
        writeString(output, agentRun.workItemId());
        writeString(output, agentRun.status().name());
        output.writeBoolean(agentRun.lease().isPresent());
        if (agentRun.lease().isPresent()) {
            writeLease(output, agentRun.lease().orElseThrow());
        }
        output.writeBoolean(agentRun.resultMessage().isPresent());
        if (agentRun.resultMessage().isPresent()) {
            writeMessageEnvelope(
                    output,
                    agentRun.resultMessage().orElseThrow());
        }
    }

    private RuntimeAgentRun readAgentRun(DataInputStream input)
            throws IOException {
        String agentRunId = readString(input);
        String goalId = readString(input);
        String workItemId = readString(input);
        RuntimeAgentRunStatus status = readEnum(
                input,
                RuntimeAgentRunStatus.class,
                "AgentRun status");
        Optional<AgentRunLease> lease = readPresence(input)
                ? Optional.of(readLease(input))
                : Optional.empty();
        Optional<MessageEnvelope> resultMessage = readPresence(input)
                ? Optional.of(readMessageEnvelope(input))
                : Optional.empty();
        return new RuntimeAgentRun(
                agentRunId,
                goalId,
                workItemId,
                status,
                lease,
                resultMessage);
    }

    private void writeLease(
            DataOutputStream output,
            AgentRunLease lease) throws IOException {
        writeString(output, lease.ownerId());
        output.writeLong(lease.fenceToken());
        writeInstant(output, lease.issuedAt());
        writeInstant(output, lease.expiresAt());
    }

    private AgentRunLease readLease(DataInputStream input)
            throws IOException {
        return new AgentRunLease(
                readString(input),
                input.readLong(),
                readInstant(input),
                readInstant(input));
    }

    private void writeWorkItem(
            DataOutputStream output,
            WorkItem workItem) throws IOException {
        writeString(output, workItem.workItemId());
        writeString(output, workItem.requiredCapability());
        writeMessageEnvelope(output, workItem.workMessage());
    }

    private WorkItem readWorkItem(DataInputStream input)
            throws IOException {
        return new WorkItem(
                readString(input),
                readString(input),
                readMessageEnvelope(input));
    }

    private void writeMessageEnvelope(
            DataOutputStream output,
            MessageEnvelope envelope) throws IOException {
        writeString(output, MessageEnvelope.ENVELOPE_VERSION);
        writeString(output, envelope.messageId());
        writeString(output, envelope.correlationId());
        writeOptionalString(output, envelope.causationId());
        writeString(output, envelope.logicalRunId());
        writeString(output, envelope.producer());
        writeInstant(output, envelope.occurredAt());
        if (envelope.payload() instanceof WorkPayload payload) {
            writeString(output, WORK_PAYLOAD_KIND);
            writeApprovedTaskRevision(output, payload.taskRevision());
            writeString(output, payload.snapshotId());
            writeStringSet(output, payload.allowedTools());
            writeExecutionInput(output, payload.executionInput());
            return;
        }
        if (envelope.payload() instanceof ResultPayload payload) {
            writeString(output, RESULT_PAYLOAD_KIND);
            writeString(output, payload.taskId());
            writeString(output, payload.runRecordReference());
            writeString(output, payload.verificationStatus().name());
            return;
        }
        throw new IOException(
                "Agent runtime message payload kind is unsupported");
    }

    private MessageEnvelope readMessageEnvelope(DataInputStream input)
            throws IOException {
        if (!MessageEnvelope.ENVELOPE_VERSION.equals(readString(input))) {
            throw new IOException(
                    "Agent runtime message envelope version is unsupported");
        }
        String messageId = readString(input);
        String correlationId = readString(input);
        Optional<String> causationId = readOptionalString(input);
        String logicalRunId = readString(input);
        String producer = readString(input);
        Instant occurredAt = readInstant(input);
        String payloadKind = readString(input);
        if (WORK_PAYLOAD_KIND.equals(payloadKind)) {
            return new MessageEnvelope(
                    messageId,
                    correlationId,
                    causationId,
                    logicalRunId,
                    producer,
                    occurredAt,
                    new WorkPayload(
                            readApprovedTaskRevision(input),
                            readString(input),
                            readStringSet(input),
                            readExecutionInput(input)));
        }
        if (RESULT_PAYLOAD_KIND.equals(payloadKind)) {
            return new MessageEnvelope(
                    messageId,
                    correlationId,
                    causationId,
                    logicalRunId,
                    producer,
                    occurredAt,
                    new ResultPayload(
                            readString(input),
                            readString(input),
                            readEnum(
                                    input,
                                    VerificationStatus.class,
                                    "verification status")));
        }
        throw new IOException(
                "Agent runtime message payload kind is invalid");
    }

    private void writeApprovedTaskRevision(
            DataOutputStream output,
            ApprovedTaskRevision revision) throws IOException {
        writeString(output, revision.taskId());
        writeString(output, revision.sourceDocument());
        writeString(output, revision.sourceSha256());
    }

    private ApprovedTaskRevision readApprovedTaskRevision(
            DataInputStream input) throws IOException {
        return new ApprovedTaskRevision(
                readString(input),
                readString(input),
                readString(input));
    }

    private void writeExecutionInput(
            DataOutputStream output,
            Optional<WorkPayload.ExecutionInput> executionInput)
            throws IOException {
        output.writeBoolean(executionInput.isPresent());
        if (executionInput.isPresent()) {
            WorkPayload.ExecutionInput input = executionInput.orElseThrow();
            writeString(output, input.targetPath());
            writeString(output, input.expectedContentSha256());
        }
    }

    private Optional<WorkPayload.ExecutionInput> readExecutionInput(
            DataInputStream input) throws IOException {
        if (!readPresence(input)) {
            return Optional.empty();
        }
        return Optional.of(new WorkPayload.ExecutionInput(
                readString(input),
                readString(input)));
    }

    private void writeStringSet(
            DataOutputStream output,
            Set<String> values) throws IOException {
        if (values.size() > MAX_COLLECTION_ITEMS) {
            throw new IOException(
                    "Agent runtime collection exceeds supported bounds");
        }
        output.writeInt(values.size());
        for (String value : values) {
            writeString(output, value);
        }
    }

    private Set<String> readStringSet(DataInputStream input)
            throws IOException {
        int size = input.readInt();
        if (size < 0 || size > MAX_COLLECTION_ITEMS) {
            throw new IOException(
                    "Agent runtime collection size is invalid");
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (int index = 0; index < size; index++) {
            if (!values.add(readString(input))) {
                throw new IOException(
                        "Agent runtime set contains a duplicate value");
            }
        }
        return Set.copyOf(values);
    }

    private void writeOptionalString(
            DataOutputStream output,
            Optional<String> value) throws IOException {
        output.writeBoolean(value.isPresent());
        if (value.isPresent()) {
            writeString(output, value.orElseThrow());
        }
    }

    private Optional<String> readOptionalString(DataInputStream input)
            throws IOException {
        return readPresence(input)
                ? Optional.of(readString(input))
                : Optional.empty();
    }

    private boolean readPresence(DataInputStream input)
            throws IOException {
        int value = input.readUnsignedByte();
        if (value != 0 && value != 1) {
            throw new IOException(
                    "Agent runtime optional marker is invalid");
        }
        return value == 1;
    }

    private void writeInstant(
            DataOutputStream output,
            Instant value) throws IOException {
        Objects.requireNonNull(value, "value must not be null");
        output.writeLong(value.getEpochSecond());
        output.writeInt(value.getNano());
    }

    private Instant readInstant(DataInputStream input)
            throws IOException {
        try {
            return Instant.ofEpochSecond(
                    input.readLong(),
                    input.readInt());
        } catch (RuntimeException exception) {
            throw new IOException(
                    "Agent runtime instant is invalid",
                    exception);
        }
    }

    private <E extends Enum<E>> E readEnum(
            DataInputStream input,
            Class<E> type,
            String field) throws IOException {
        String value = readString(input);
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw new IOException(field + " is invalid", exception);
        }
    }

    private void writeString(
            DataOutputStream output,
            String value) throws IOException {
        byte[] encoded = encodeUtf8(
                Objects.requireNonNull(value, "value must not be null"));
        if (encoded.length > MAX_STRING_BYTES) {
            throw new IOException(
                    "Agent runtime string exceeds supported bounds");
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
                    "Agent runtime string length is invalid");
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
                    "Agent runtime string is not valid UTF-8",
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
                    "Agent runtime string is not valid Unicode text",
                    exception);
        }
    }

    private Path artifactPath(String goalId) {
        Path artifact = storageRoot
                .resolve(goalId + FILE_SUFFIX)
                .normalize();
        if (!artifact.startsWith(storageRoot)) {
            throw new IllegalArgumentException(
                    "Agent runtime identity resolves outside storage");
        }
        return artifact;
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

    private CorruptedAgentRuntimeStateException corrupted(
            String goalId,
            String reason) {
        return new CorruptedAgentRuntimeStateException(goalId, reason);
    }

    private CorruptedAgentRuntimeStateException corrupted(
            String goalId,
            String reason,
            Throwable cause) {
        return new CorruptedAgentRuntimeStateException(
                goalId,
                reason,
                cause);
    }
}
