package com.enhancer.run;

import com.enhancer.io.BoundedFileOperations;
import com.enhancer.io.FileSizeLimitExceededException;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.kernel.VerificationCode;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.kernel.VerificationStatus;
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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

public final class FileSystemRunRecordStore implements RunRecordStore {
    private static final int ENVELOPE_MAGIC = 0x454E5234;
    private static final int PAYLOAD_VERSION = 1;
    private static final int DIGEST_BYTES = 32;
    private static final int HEADER_BYTES = Integer.BYTES + Long.BYTES + Integer.BYTES + DIGEST_BYTES;
    private static final int MAX_PAYLOAD_BYTES = 4 * 1024 * 1024;
    private static final int MAX_STRING_BYTES = 1024 * 1024;
    private static final int MAX_COLLECTION_ITEMS = 4096;
    private static final String REFERENCE_PREFIX = "run-record/";
    private static final String FILE_SUFFIX = ".run-record";

    private final Path storageRoot;

    public FileSystemRunRecordStore(Path storageRoot) {
        Objects.requireNonNull(storageRoot, "storageRoot must not be null");
        this.storageRoot = storageRoot.toAbsolutePath().normalize();
    }

    @Override
    public StoredRunRecord persist(RunRecord record) throws IOException {
        Objects.requireNonNull(record, "record must not be null");
        Files.createDirectories(storageRoot);
        if (!Files.isDirectory(storageRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("RunRecord storage root must be a directory");
        }

        byte[] payload = encode(record);
        if (payload.length > MAX_PAYLOAD_BYTES) {
            throw new IOException("RunRecord payload exceeds the supported size limit");
        }
        String recordId = UUID.randomUUID().toString();
        Instant storedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        byte[] digest = envelopeDigest(storedAt.toEpochMilli(), payload.length, payload);
        ByteBuffer envelope = ByteBuffer.allocate(HEADER_BYTES + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAt.toEpochMilli())
                .putInt(payload.length)
                .put(digest)
                .put(payload);
        envelope.flip();

        Path pending = Files.createTempFile(storageRoot, ".pending-", ".tmp");
        Path destination = artifactPath(recordId);
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
                Files.move(pending, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException("atomic RunRecord persistence is not supported", exception);
            }
        } finally {
            Files.deleteIfExists(pending);
        }

        return new StoredRunRecord(
                recordId,
                REFERENCE_PREFIX + recordId,
                storedAt,
                payload.length,
                HexFormat.of().formatHex(digest));
    }

    @Override
    public List<String> references() throws IOException {
        if (!Files.isDirectory(storageRoot, LinkOption.NOFOLLOW_LINKS)) {
            return List.of();
        }
        List<String> references = new ArrayList<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(storageRoot)) {
            for (Path entry : entries) {
                String fileName = entry.getFileName().toString();
                if (!fileName.endsWith(FILE_SUFFIX)
                        || !Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }
                String recordId = fileName.substring(
                        0,
                        fileName.length() - FILE_SUFFIX.length());
                try {
                    StoredRunRecord.requireCanonicalUuid(recordId);
                } catch (IllegalArgumentException exception) {
                    continue;
                }
                references.add(REFERENCE_PREFIX + recordId);
            }
        }
        references.sort(Comparator.naturalOrder());
        return List.copyOf(references);
    }

    @Override
    public List<String> recentReferences(int limit) throws IOException {
        if (limit < 1 || limit > RunRecordStore.MAX_REFERENCE_WINDOW) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and " + RunRecordStore.MAX_REFERENCE_WINDOW);
        }
        if (!Files.isDirectory(storageRoot, LinkOption.NOFOLLOW_LINKS)) {
            return List.of();
        }
        Comparator<RecordFile> oldestFirst = Comparator
                .comparingLong(RecordFile::modifiedMillis)
                .thenComparing(RecordFile::reference, Comparator.reverseOrder());
        PriorityQueue<RecordFile> recent = new PriorityQueue<>(limit, oldestFirst);
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(storageRoot)) {
            for (Path entry : entries) {
                Optional<String> reference = referenceFor(entry);
                if (reference.isEmpty()) {
                    continue;
                }
                RecordFile candidate = new RecordFile(
                        reference.orElseThrow(),
                        Files.getLastModifiedTime(entry, LinkOption.NOFOLLOW_LINKS).toMillis());
                recent.add(candidate);
                if (recent.size() > limit) {
                    recent.remove();
                }
            }
        }
        Comparator<RecordFile> newestFirst = Comparator
                .comparingLong(RecordFile::modifiedMillis)
                .reversed()
                .thenComparing(RecordFile::reference);
        return recent.stream()
                .sorted(newestFirst)
                .map(RecordFile::reference)
                .toList();
    }

    private Optional<String> referenceFor(Path entry) {
        String fileName = entry.getFileName().toString();
        if (!fileName.endsWith(FILE_SUFFIX)
                || !Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        String recordId = fileName.substring(0, fileName.length() - FILE_SUFFIX.length());
        try {
            StoredRunRecord.requireCanonicalUuid(recordId);
            return Optional.of(REFERENCE_PREFIX + recordId);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    @Override
    public ResolvedRunRecord resolve(String reference) throws IOException {
        String recordId = parseReference(reference);
        Path artifact = artifactPath(recordId);
        if (!Files.exists(artifact, LinkOption.NOFOLLOW_LINKS)) {
            throw new MissingRunRecordException(reference);
        }
        if (!Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS)) {
            throw corrupted(reference, "artifact is not a regular file");
        }

        long artifactSize = Files.size(artifact);
        if (artifactSize < HEADER_BYTES || artifactSize > HEADER_BYTES + MAX_PAYLOAD_BYTES) {
            throw corrupted(reference, "artifact size is outside supported bounds");
        }

        byte[] envelope;
        try {
            envelope = BoundedFileOperations.readAllBytes(
                    artifact,
                    HEADER_BYTES + MAX_PAYLOAD_BYTES);
        } catch (FileSizeLimitExceededException exception) {
            throw corrupted(
                    reference,
                    "artifact grew outside supported bounds while reading");
        } catch (NoSuchFileException exception) {
            throw new MissingRunRecordException(reference);
        }
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        if (buffer.getInt() != ENVELOPE_MAGIC) {
            throw corrupted(reference, "envelope header is invalid");
        }
        long storedAtMillis = buffer.getLong();
        int declaredLength = buffer.getInt();
        if (declaredLength < 0
                || declaredLength > MAX_PAYLOAD_BYTES
                || declaredLength != buffer.remaining() - DIGEST_BYTES) {
            throw corrupted(reference, "declared payload length does not match the artifact");
        }
        byte[] declaredDigest = new byte[DIGEST_BYTES];
        buffer.get(declaredDigest);
        byte[] payload = new byte[declaredLength];
        buffer.get(payload);
        byte[] actualDigest = envelopeDigest(storedAtMillis, declaredLength, payload);
        if (!MessageDigest.isEqual(declaredDigest, actualDigest)) {
            throw corrupted(reference, "envelope digest does not match stored metadata");
        }

        RunRecord record = decode(reference, payload);
        StoredRunRecord metadata = new StoredRunRecord(
                recordId,
                reference,
                Instant.ofEpochMilli(storedAtMillis),
                payload.length,
                HexFormat.of().formatHex(declaredDigest));
        return new ResolvedRunRecord(metadata, record);
    }

    private byte[] encode(RunRecord record) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(PAYLOAD_VERSION);
            writeString(output, record.logicalRunId());
            output.writeLong(record.recordedAt().toEpochMilli());

            ApprovedTask task = record.approvedTask();
            writeString(output, task.taskId());
            writeString(output, task.description());
            writeString(output, task.approvalEvidence());
            writeSet(output, task.allowedTools());
            writeString(output, task.sourceDocument());

            ToolRequest request = record.toolRequest();
            writeString(output, request.toolName());
            writeString(output, request.correlationId());
            writeMap(output, request.arguments());

            PolicyDecision policy = record.policyDecision();
            writeString(output, policy.status().name());
            writeString(output, policy.projectRoot());
            writeSet(output, policy.allowedTools());
            writeSet(output, policy.deniedTools());
            output.writeLong(policy.maxReadBytes());
            output.writeLong(policy.timeoutMillis());

            ToolResult result = record.toolResult();
            writeString(output, result.toolName());
            writeString(output, result.status().name());
            writeOptionalInt(output, result.exitCode());
            writeOptionalString(
                    output,
                    result.failureCode().map(Enum::name));
            writeEvidence(output, result.evidence());
            writeOptionalString(output, record.expectedContentSha256());

            VerificationDecision verification = record.verification();
            writeString(output, verification.status().name());
            writeString(output, verification.code().name());
            writeString(output, verification.reason());

            output.writeInt(record.iterations());
            writeString(output, record.workerStopReason().name());
            writeString(output, record.finalStopReason().name());
        }
        return bytes.toByteArray();
    }

    private RunRecord decode(String reference, byte[] payload)
            throws CorruptedRunRecordException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            if (input.readInt() != PAYLOAD_VERSION) {
                throw corrupted(reference, "payload version is unsupported");
            }
            String logicalRunId = readString(input);
            Instant recordedAt = Instant.ofEpochMilli(input.readLong());

            ApprovedTask task = new ApprovedTask(
                    readString(input),
                    readString(input),
                    readString(input),
                    readSet(input),
                    readString(input));
            ToolRequest request = new ToolRequest(
                    readString(input),
                    readString(input),
                    readMap(input));
            PolicyDecision policy = new PolicyDecision(
                    readEnum(input, PolicyDecisionStatus.class),
                    readString(input),
                    readSet(input),
                    readSet(input),
                    input.readLong(),
                    input.readLong());
            ToolResult result = new ToolResult(
                    readString(input),
                    readEnum(input, ToolResultStatus.class),
                    readOptionalInt(input),
                    readOptionalEnum(input, ToolFailureCode.class),
                    readEvidence(input));
            Optional<String> expectedContentSha256 = readOptionalString(input);
            VerificationDecision verification = new VerificationDecision(
                    readEnum(input, VerificationStatus.class),
                    readEnum(input, VerificationCode.class),
                    readString(input));
            int iterations = input.readInt();
            AgentLoopStopReason workerStopReason = readEnum(
                    input,
                    AgentLoopStopReason.class);
            AgentLoopStopReason finalStopReason = readEnum(
                    input,
                    AgentLoopStopReason.class);
            if (input.available() != 0) {
                throw corrupted(reference, "payload contains trailing bytes");
            }
            return new RunRecord(
                    logicalRunId,
                    recordedAt,
                    task,
                    request,
                    policy,
                    result,
                    expectedContentSha256,
                    verification,
                    iterations,
                    workerStopReason,
                    finalStopReason);
        } catch (CorruptedRunRecordException exception) {
            throw exception;
        } catch (EOFException exception) {
            throw corrupted(reference, "payload ended before all fields were read", exception);
        } catch (IOException | RuntimeException exception) {
            throw corrupted(reference, "payload could not be decoded", exception);
        }
    }

    private void writeEvidence(DataOutputStream output, VerificationEvidence evidence)
            throws IOException {
        writeString(output, evidence.summary());
        writeString(output, evidence.outputTail());
        output.writeInt(evidence.originalOutputLength());
        output.writeBoolean(evidence.truncated());
        writeOptionalString(output, evidence.fullOutputReference());
        writeOptionalString(output, evidence.contentSha256());
    }

    private VerificationEvidence readEvidence(DataInputStream input) throws IOException {
        return new VerificationEvidence(
                readString(input),
                readString(input),
                input.readInt(),
                input.readBoolean(),
                readOptionalString(input),
                readOptionalString(input));
    }

    private void writeString(DataOutputStream output, String value) throws IOException {
        byte[] encoded = encodeUtf8(Objects.requireNonNull(value, "value must not be null"));
        if (encoded.length > MAX_STRING_BYTES) {
            throw new IOException("RunRecord string exceeds the supported size limit");
        }
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private String readString(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_STRING_BYTES || length > input.available()) {
            throw new IOException("RunRecord string length is invalid");
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
            throw new IOException("RunRecord string is not valid UTF-8", exception);
        }
    }

    private void writeSet(DataOutputStream output, Set<String> values) throws IOException {
        TreeSet<String> ordered = new TreeSet<>(values);
        output.writeInt(ordered.size());
        for (String value : ordered) {
            writeString(output, value);
        }
    }

    private Set<String> readSet(DataInputStream input) throws IOException {
        int size = readCollectionSize(input);
        Set<String> values = new LinkedHashSet<>();
        for (int index = 0; index < size; index++) {
            if (!values.add(readString(input))) {
                throw new IOException("RunRecord set contains a duplicate value");
            }
        }
        return Set.copyOf(values);
    }

    private void writeMap(DataOutputStream output, Map<String, String> values) throws IOException {
        TreeMap<String, String> ordered = new TreeMap<>(values);
        output.writeInt(ordered.size());
        for (Map.Entry<String, String> entry : ordered.entrySet()) {
            writeString(output, entry.getKey());
            writeString(output, entry.getValue());
        }
    }

    private Map<String, String> readMap(DataInputStream input) throws IOException {
        int size = readCollectionSize(input);
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 0; index < size; index++) {
            String key = readString(input);
            if (values.put(key, readString(input)) != null) {
                throw new IOException("RunRecord map contains a duplicate key");
            }
        }
        return Map.copyOf(values);
    }

    private int readCollectionSize(DataInputStream input) throws IOException {
        int size = input.readInt();
        if (size < 0 || size > MAX_COLLECTION_ITEMS) {
            throw new IOException("RunRecord collection size is invalid");
        }
        return size;
    }

    private void writeOptionalString(DataOutputStream output, Optional<String> value)
            throws IOException {
        output.writeBoolean(value.isPresent());
        if (value.isPresent()) {
            writeString(output, value.orElseThrow());
        }
    }

    private Optional<String> readOptionalString(DataInputStream input) throws IOException {
        return input.readBoolean() ? Optional.of(readString(input)) : Optional.empty();
    }

    private void writeOptionalInt(DataOutputStream output, OptionalInt value) throws IOException {
        output.writeBoolean(value.isPresent());
        if (value.isPresent()) {
            output.writeInt(value.orElseThrow());
        }
    }

    private OptionalInt readOptionalInt(DataInputStream input) throws IOException {
        return input.readBoolean() ? OptionalInt.of(input.readInt()) : OptionalInt.empty();
    }

    private <E extends Enum<E>> Optional<E> readOptionalEnum(
            DataInputStream input,
            Class<E> enumType) throws IOException {
        return input.readBoolean()
                ? Optional.of(parseEnum(readString(input), enumType))
                : Optional.empty();
    }

    private <E extends Enum<E>> E readEnum(DataInputStream input, Class<E> enumType)
            throws IOException {
        return parseEnum(readString(input), enumType);
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> enumType) throws IOException {
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new IOException("RunRecord enum value is invalid", exception);
        }
    }

    private String parseReference(String reference) {
        Objects.requireNonNull(reference, "reference must not be null");
        if (!reference.startsWith(REFERENCE_PREFIX)) {
            throw new IllegalArgumentException("RunRecord reference has an invalid prefix");
        }
        String recordId = reference.substring(REFERENCE_PREFIX.length());
        if (recordId.contains("/")) {
            throw new IllegalArgumentException("RunRecord reference has too many segments");
        }
        StoredRunRecord.requireCanonicalUuid(recordId);
        return recordId;
    }

    private Path artifactPath(String recordId) {
        Path artifact = storageRoot.resolve(recordId + FILE_SUFFIX).normalize();
        if (!artifact.startsWith(storageRoot)) {
            throw new IllegalArgumentException("RunRecord identity resolves outside storage");
        }
        return artifact;
    }

    private byte[] sha256(byte[] payload) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(payload);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private byte[] envelopeDigest(long storedAtMillis, int payloadLength, byte[] payload) {
        return sha256(ByteBuffer.allocate(
                        Integer.BYTES + Long.BYTES + Integer.BYTES + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAtMillis)
                .putInt(payloadLength)
                .put(payload)
                .array());
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
            throw new IOException("RunRecord string is not valid Unicode text", exception);
        }
    }

    private CorruptedRunRecordException corrupted(String reference, String reason) {
        return new CorruptedRunRecordException(
                "corrupted RunRecord " + reference + ": " + reason);
    }

    private CorruptedRunRecordException corrupted(
            String reference,
            String reason,
            Throwable cause) {
        return new CorruptedRunRecordException(
                "corrupted RunRecord " + reference + ": " + reason,
                cause);
    }

    private record RecordFile(String reference, long modifiedMillis) {
    }
}
