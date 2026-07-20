package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * One-current-snapshot filesystem adapter for Scheduler queue state. Atomic publication prevents
 * partial state visibility; it does not claim power-loss durability of parent-directory metadata.
 */
public final class FileSystemSchedulerQueueStore implements SchedulerQueueStore {
    private static final int ENVELOPE_MAGIC = 0x45535131;
    private static final int DIGEST_BYTES = 32;
    static final int HEADER_BYTES =
            Integer.BYTES + Long.BYTES + Integer.BYTES + DIGEST_BYTES;
    static final int MAX_STATE_BYTES = 64 * 1024 * 1024;
    private static final int MAX_STRING_BYTES = 1024 * 1024;
    private static final int MAX_COLLECTION_ITEMS =
            SingleWorkerSchedulerQueue.MAX_WORK_ITEMS;
    private static final String FILE_SUFFIX = ".scheduler-queue";
    private static final String PAYLOAD_KIND = "scheduler-queue-state";

    private final Path storageRoot;

    public FileSystemSchedulerQueueStore(Path storageRoot) {
        Objects.requireNonNull(storageRoot, "storageRoot must not be null");
        this.storageRoot = storageRoot.toAbsolutePath().normalize();
    }

    @Override
    public void create(SchedulerQueueState initialState) throws IOException {
        Objects.requireNonNull(
                initialState, "initialState must not be null");
        if (initialState.revision() != 0) {
            throw new IllegalArgumentException(
                    "initialState revision must be zero");
        }
        prepareRoot();
        if (Files.exists(
                artifactPath(initialState.queueId()),
                LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "Scheduler queue state already exists: "
                            + initialState.queueId());
        }
        publish(initialState, false);
    }

    @Override
    public void update(SchedulerQueueState nextState) throws IOException {
        Objects.requireNonNull(nextState, "nextState must not be null");
        SchedulerQueueState current = resolve(nextState.queueId());
        if (nextState.revision() != current.revision() + 1) {
            throw new IOException(
                    "Scheduler queue revision must advance exactly one");
        }
        if (nextState.maxWorkItems() != current.maxWorkItems()) {
            throw new IOException(
                    "Scheduler queue capacity must not change");
        }
        if (current.logicalRunId().isPresent()
                && !current.logicalRunId().equals(nextState.logicalRunId())) {
            throw new IOException(
                    "Scheduler queue logical run must not change");
        }
        publish(nextState, true);
    }

    @Override
    public SchedulerQueueState resolve(String queueId) throws IOException {
        String canonicalQueueId =
                SchedulerQueueState.requireCanonicalQueueId(queueId);
        Path artifact = artifactPath(canonicalQueueId);
        if (!Files.exists(artifact, LinkOption.NOFOLLOW_LINKS)) {
            throw new MissingSchedulerQueueStateException(
                    canonicalQueueId);
        }
        if (!Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS)) {
            throw corrupted(
                    canonicalQueueId,
                    "artifact is not a regular file");
        }
        long artifactSize = Files.size(artifact);
        if (artifactSize < HEADER_BYTES
                || artifactSize > HEADER_BYTES + MAX_STATE_BYTES) {
            throw corrupted(
                    canonicalQueueId,
                    "artifact size is outside supported bounds");
        }

        byte[] envelope;
        try {
            envelope = BoundedFileOperations.readAllBytes(
                    artifact,
                    HEADER_BYTES + MAX_STATE_BYTES);
        } catch (FileSizeLimitExceededException exception) {
            throw corrupted(
                    canonicalQueueId,
                    "artifact grew outside supported bounds while reading");
        } catch (NoSuchFileException exception) {
            throw new MissingSchedulerQueueStateException(
                    canonicalQueueId);
        }
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        if (buffer.getInt() != ENVELOPE_MAGIC) {
            throw corrupted(
                    canonicalQueueId,
                    "envelope header is invalid");
        }
        long storedAtMillis = buffer.getLong();
        int declaredLength = buffer.getInt();
        if (declaredLength < 0
                || declaredLength > MAX_STATE_BYTES
                || declaredLength != buffer.remaining() - DIGEST_BYTES) {
            throw corrupted(
                    canonicalQueueId,
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
                    canonicalQueueId,
                    "envelope digest does not match stored metadata");
        }
        return decode(canonicalQueueId, payload);
    }

    private void prepareRoot() throws IOException {
        Files.createDirectories(storageRoot);
        if (!Files.isDirectory(storageRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "Scheduler queue storage root must be a directory");
        }
    }

    private void publish(
            SchedulerQueueState state,
            boolean replaceExisting) throws IOException {
        byte[] payload = encode(state);
        if (payload.length > MAX_STATE_BYTES) {
            throw new IOException(
                    "Scheduler queue state exceeds the supported size limit");
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
        Path destination = artifactPath(state.queueId());
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
                        "atomic Scheduler queue persistence is not supported",
                        exception);
            } catch (FileAlreadyExistsException exception) {
                throw new IOException(
                        "Scheduler queue state already exists: "
                                + state.queueId(),
                        exception);
            }
        } finally {
            Files.deleteIfExists(pending);
        }
    }

    private byte[] encode(SchedulerQueueState state) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(state.schemaVersion());
            writeString(output, PAYLOAD_KIND);
            writeString(output, state.queueId());
            output.writeLong(state.revision());
            output.writeInt(state.maxWorkItems());
            writeOptionalString(output, state.logicalRunId());
            writeStringList(output, state.admissionOrder());
            writeQueuedWorkList(output, state.pendingWork());
            writeOptionalQueuedWork(output, state.activeWork());
            writeStringSet(output, state.completedWorkItemIds());
            writeStringSet(output, state.failedWorkItemIds());
        }
        return bytes.toByteArray();
    }

    private SchedulerQueueState decode(
            String expectedQueueId,
            byte[] payload) throws CorruptedSchedulerQueueStateException {
        try (DataInputStream input =
                new DataInputStream(new ByteArrayInputStream(payload))) {
            int schemaVersion = input.readInt();
            if (schemaVersion
                    != SchedulerQueueState.CURRENT_SCHEMA_VERSION) {
                throw corrupted(
                        expectedQueueId,
                        "state schema version is unsupported");
            }
            if (!PAYLOAD_KIND.equals(readString(input))) {
                throw corrupted(
                        expectedQueueId,
                        "state payload kind is invalid");
            }
            String queueId = readString(input);
            if (!expectedQueueId.equals(queueId)) {
                throw corrupted(
                        expectedQueueId,
                        "state queue identity does not match artifact");
            }
            long revision = input.readLong();
            int maxWorkItems = input.readInt();
            Optional<String> logicalRunId = readOptionalString(input);
            List<String> admissionOrder = readStringList(input);
            List<QueuedWork> pendingWork = readQueuedWorkList(input);
            Optional<QueuedWork> activeWork =
                    readOptionalQueuedWork(input);
            Set<String> completedWorkItemIds = readStringSet(input);
            Set<String> failedWorkItemIds = readStringSet(input);
            if (input.available() != 0) {
                throw corrupted(
                        expectedQueueId,
                        "state contains trailing bytes");
            }
            return new SchedulerQueueState(
                    schemaVersion,
                    queueId,
                    revision,
                    maxWorkItems,
                    logicalRunId,
                    admissionOrder,
                    pendingWork,
                    activeWork,
                    completedWorkItemIds,
                    failedWorkItemIds);
        } catch (CorruptedSchedulerQueueStateException exception) {
            throw exception;
        } catch (EOFException exception) {
            throw corrupted(
                    expectedQueueId,
                    "state ended before all fields were read",
                    exception);
        } catch (IOException | RuntimeException exception) {
            throw corrupted(
                    expectedQueueId,
                    "state could not be decoded",
                    exception);
        }
    }

    private void writeQueuedWorkList(
            DataOutputStream output,
            List<QueuedWork> values) throws IOException {
        writeCollectionSize(output, values.size());
        for (QueuedWork value : values) {
            writeQueuedWork(output, value);
        }
    }

    private List<QueuedWork> readQueuedWorkList(
            DataInputStream input) throws IOException {
        int size = readCollectionSize(input);
        List<QueuedWork> values = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            values.add(readQueuedWork(input));
        }
        return List.copyOf(values);
    }

    private void writeOptionalQueuedWork(
            DataOutputStream output,
            Optional<QueuedWork> value) throws IOException {
        output.writeBoolean(value.isPresent());
        if (value.isPresent()) {
            writeQueuedWork(output, value.orElseThrow());
        }
    }

    private Optional<QueuedWork> readOptionalQueuedWork(
            DataInputStream input) throws IOException {
        return readPresence(input)
                ? Optional.of(readQueuedWork(input))
                : Optional.empty();
    }

    private void writeQueuedWork(
            DataOutputStream output,
            QueuedWork queuedWork) throws IOException {
        WorkItem workItem = queuedWork.workItem();
        writeString(output, workItem.workItemId());
        writeString(output, workItem.requiredCapability());
        writeMessageEnvelope(output, workItem.workMessage());
        writeStringSet(output, queuedWork.dependencyWorkItemIds());
    }

    private QueuedWork readQueuedWork(DataInputStream input)
            throws IOException {
        return new QueuedWork(
                new WorkItem(
                        readString(input),
                        readString(input),
                        readMessageEnvelope(input)),
                readStringSet(input));
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
        output.writeLong(envelope.occurredAt().getEpochSecond());
        output.writeInt(envelope.occurredAt().getNano());
        if (!(envelope.payload() instanceof WorkPayload payload)) {
            throw new IOException(
                    "Scheduler queue work message must carry WorkPayload");
        }
        writeApprovedTaskRevision(output, payload.taskRevision());
        writeString(output, payload.snapshotId());
        writeStringSet(output, payload.allowedTools());
        writeExecutionInput(output, payload.executionInput());
    }

    private MessageEnvelope readMessageEnvelope(DataInputStream input)
            throws IOException {
        if (!MessageEnvelope.ENVELOPE_VERSION.equals(readString(input))) {
            throw new IOException(
                    "Scheduler queue message envelope version is unsupported");
        }
        String messageId = readString(input);
        String correlationId = readString(input);
        Optional<String> causationId = readOptionalString(input);
        String logicalRunId = readString(input);
        String producer = readString(input);
        Instant occurredAt = Instant.ofEpochSecond(
                input.readLong(),
                input.readInt());
        ApprovedTaskRevision taskRevision =
                readApprovedTaskRevision(input);
        String snapshotId = readString(input);
        Set<String> allowedTools = readStringSet(input);
        Optional<WorkPayload.ExecutionInput> executionInput =
                readExecutionInput(input);
        return new MessageEnvelope(
                messageId,
                correlationId,
                causationId,
                logicalRunId,
                producer,
                occurredAt,
                new WorkPayload(
                        taskRevision,
                        snapshotId,
                        allowedTools,
                        executionInput));
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

    private void writeStringList(
            DataOutputStream output,
            List<String> values) throws IOException {
        writeCollectionSize(output, values.size());
        for (String value : values) {
            writeString(output, value);
        }
    }

    private List<String> readStringList(DataInputStream input)
            throws IOException {
        int size = readCollectionSize(input);
        List<String> values = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            values.add(readString(input));
        }
        return List.copyOf(values);
    }

    private void writeStringSet(
            DataOutputStream output,
            Set<String> values) throws IOException {
        writeCollectionSize(output, values.size());
        for (String value : values) {
            writeString(output, value);
        }
    }

    private Set<String> readStringSet(DataInputStream input)
            throws IOException {
        int size = readCollectionSize(input);
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (int index = 0; index < size; index++) {
            if (!values.add(readString(input))) {
                throw new IOException(
                        "Scheduler queue set contains a duplicate value");
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
                    "Scheduler queue optional marker is invalid");
        }
        return value == 1;
    }

    private void writeCollectionSize(
            DataOutputStream output,
            int size) throws IOException {
        if (size < 0 || size > MAX_COLLECTION_ITEMS) {
            throw new IOException(
                    "Scheduler queue collection exceeds supported bounds");
        }
        output.writeInt(size);
    }

    private int readCollectionSize(DataInputStream input)
            throws IOException {
        int size = input.readInt();
        if (size < 0 || size > MAX_COLLECTION_ITEMS) {
            throw new IOException(
                    "Scheduler queue collection size is invalid");
        }
        return size;
    }

    private void writeString(
            DataOutputStream output,
            String value) throws IOException {
        byte[] encoded = encodeUtf8(
                Objects.requireNonNull(value, "value must not be null"));
        if (encoded.length > MAX_STRING_BYTES) {
            throw new IOException(
                    "Scheduler queue string exceeds supported bounds");
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
                    "Scheduler queue string length is invalid");
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
                    "Scheduler queue string is not valid UTF-8",
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
                    "Scheduler queue string is not valid Unicode text",
                    exception);
        }
    }

    private Path artifactPath(String queueId) {
        Path artifact = storageRoot
                .resolve(queueId + FILE_SUFFIX)
                .normalize();
        if (!artifact.startsWith(storageRoot)) {
            throw new IllegalArgumentException(
                    "Scheduler queue identity resolves outside storage");
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

    private CorruptedSchedulerQueueStateException corrupted(
            String queueId,
            String reason) {
        return new CorruptedSchedulerQueueStateException(
                "corrupted Scheduler queue " + queueId + ": " + reason);
    }

    private CorruptedSchedulerQueueStateException corrupted(
            String queueId,
            String reason,
            Throwable cause) {
        return new CorruptedSchedulerQueueStateException(
                "corrupted Scheduler queue " + queueId + ": " + reason,
                cause);
    }
}
