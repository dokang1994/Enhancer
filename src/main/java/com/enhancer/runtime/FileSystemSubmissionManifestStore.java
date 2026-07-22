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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Integrity-protected atomic filesystem storage for immutable submission manifests. */
public final class FileSystemSubmissionManifestStore
        implements SubmissionManifestStore {
    private static final int ENVELOPE_MAGIC = 0x45534d31;
    private static final int SCHEMA_VERSION = 1;
    private static final int DIGEST_BYTES = 32;
    private static final int HEADER_BYTES =
            Integer.BYTES + Long.BYTES + Integer.BYTES + DIGEST_BYTES;
    private static final int MAX_PAYLOAD_BYTES = 4 * 1024 * 1024;
    private static final int MAX_STRING_BYTES = 1024 * 1024;
    private static final String PAYLOAD_KIND = "durable-submission-manifest";
    private static final String FILE_SUFFIX = ".submission-manifest";

    private final Path storageRoot;

    public FileSystemSubmissionManifestStore(Path storageRoot) {
        Objects.requireNonNull(storageRoot, "storageRoot must not be null");
        this.storageRoot = storageRoot.toAbsolutePath().normalize();
    }

    @Override
    public boolean storeIdempotently(DurableSubmissionManifest manifest)
            throws IOException {
        Objects.requireNonNull(manifest, "manifest must not be null");
        prepareRoot();
        Path destination = artifactPath(manifest.submissionId());
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
            DurableSubmissionManifest existing = resolve(manifest.submissionId());
            if (existing.equals(manifest)) {
                return false;
            }
            throw new IllegalArgumentException(
                    "submission identity was stored with different content");
        }
        publish(manifest, destination);
        return true;
    }

    @Override
    public DurableSubmissionManifest resolve(String submissionId)
            throws IOException {
        String canonicalId = canonicalUuid(submissionId, "submissionId");
        Path artifact = artifactPath(canonicalId);
        if (!Files.exists(artifact, LinkOption.NOFOLLOW_LINKS)) {
            throw missing(canonicalId);
        }
        if (!Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS)) {
            throw corrupted(canonicalId, "artifact is not a regular file");
        }
        long size = Files.size(artifact);
        if (size < HEADER_BYTES || size > HEADER_BYTES + MAX_PAYLOAD_BYTES) {
            throw corrupted(canonicalId, "artifact size is outside supported bounds");
        }

        byte[] envelope;
        try {
            envelope = BoundedFileOperations.readAllBytes(
                    artifact, HEADER_BYTES + MAX_PAYLOAD_BYTES);
        } catch (FileSizeLimitExceededException exception) {
            throw corrupted(
                    canonicalId,
                    "artifact grew outside supported bounds while reading",
                    exception);
        } catch (NoSuchFileException exception) {
            throw missing(canonicalId);
        }
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        if (buffer.getInt() != ENVELOPE_MAGIC) {
            throw corrupted(canonicalId, "envelope header is invalid");
        }
        long storedAtMillis = buffer.getLong();
        int declaredLength = buffer.getInt();
        if (declaredLength < 0
                || declaredLength > MAX_PAYLOAD_BYTES
                || declaredLength != buffer.remaining() - DIGEST_BYTES) {
            throw corrupted(
                    canonicalId,
                    "declared payload length does not match the artifact");
        }
        byte[] declaredDigest = new byte[DIGEST_BYTES];
        buffer.get(declaredDigest);
        byte[] payload = new byte[declaredLength];
        buffer.get(payload);
        byte[] actualDigest = envelopeDigest(
                storedAtMillis, declaredLength, payload);
        if (!MessageDigest.isEqual(declaredDigest, actualDigest)) {
            throw corrupted(
                    canonicalId,
                    "envelope digest does not match stored metadata");
        }
        return decode(canonicalId, payload);
    }

    Path artifactPath(String submissionId) {
        String canonicalId = canonicalUuid(submissionId, "submissionId");
        Path artifact = storageRoot.resolve(canonicalId + FILE_SUFFIX).normalize();
        if (!artifact.startsWith(storageRoot)) {
            throw new IllegalArgumentException(
                    "submission identity resolves outside storage");
        }
        return artifact;
    }

    private void prepareRoot() throws IOException {
        Files.createDirectories(storageRoot);
        if (!Files.isDirectory(storageRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("submission manifest storage root must be a directory");
        }
    }

    private void publish(
            DurableSubmissionManifest manifest,
            Path destination) throws IOException {
        byte[] payload = encode(manifest);
        if (payload.length > MAX_PAYLOAD_BYTES) {
            throw new IOException(
                    "submission manifest exceeds the supported size limit");
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
                Files.move(pending, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException(
                        "atomic submission manifest persistence is not supported",
                        exception);
            }
        } finally {
            Files.deleteIfExists(pending);
        }
    }

    private byte[] encode(DurableSubmissionManifest manifest) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(SCHEMA_VERSION);
            writeString(output, PAYLOAD_KIND);
            writeString(output, manifest.queueId());
            output.writeInt(manifest.maxWorkItems());
            writeString(output, manifest.requiredCapability());
            writeMessageEnvelope(output, manifest.workMessage());
        }
        return bytes.toByteArray();
    }

    private DurableSubmissionManifest decode(
            String expectedSubmissionId,
            byte[] payload) throws IOException {
        try (DataInputStream input =
                new DataInputStream(new ByteArrayInputStream(payload))) {
            if (input.readInt() != SCHEMA_VERSION) {
                throw corrupted(
                        expectedSubmissionId,
                        "payload schema version is unsupported");
            }
            if (!PAYLOAD_KIND.equals(readString(input))) {
                throw corrupted(expectedSubmissionId, "payload kind is invalid");
            }
            DurableSubmissionManifest manifest = new DurableSubmissionManifest(
                    readString(input),
                    input.readInt(),
                    readString(input),
                    readMessageEnvelope(input));
            if (!expectedSubmissionId.equals(manifest.submissionId())) {
                throw corrupted(
                        expectedSubmissionId,
                        "submission identity does not match artifact");
            }
            if (input.available() != 0) {
                throw corrupted(
                        expectedSubmissionId,
                        "payload contains trailing bytes");
            }
            return manifest;
        } catch (CorruptedManifestException exception) {
            throw exception;
        } catch (EOFException exception) {
            throw corrupted(
                    expectedSubmissionId,
                    "payload ended before all fields were read",
                    exception);
        } catch (IOException exception) {
            throw corrupted(
                    expectedSubmissionId,
                    "payload could not be decoded",
                    exception);
        } catch (RuntimeException exception) {
            throw corrupted(
                    expectedSubmissionId,
                    "payload could not be decoded",
                    exception);
        }
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
        WorkPayload work = (WorkPayload) envelope.payload();
        ApprovedTaskRevision revision = work.taskRevision();
        writeString(output, revision.taskId());
        writeString(output, revision.sourceDocument());
        writeString(output, revision.sourceSha256());
        writeString(output, work.snapshotId());
        writeStringSet(output, work.allowedTools());
        writeExecutionInput(output, work.executionInput());
    }

    private MessageEnvelope readMessageEnvelope(DataInputStream input)
            throws IOException {
        if (!MessageEnvelope.ENVELOPE_VERSION.equals(readString(input))) {
            throw new IOException("message envelope version is unsupported");
        }
        String messageId = readString(input);
        String correlationId = readString(input);
        Optional<String> causationId = readOptionalString(input);
        String logicalRunId = readString(input);
        String producer = readString(input);
        Instant occurredAt = Instant.ofEpochSecond(input.readLong(), input.readInt());
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                readString(input), readString(input), readString(input));
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
                        revision, snapshotId, allowedTools, executionInput));
    }

    private void writeExecutionInput(
            DataOutputStream output,
            Optional<WorkPayload.ExecutionInput> input) throws IOException {
        output.writeBoolean(input.isPresent());
        if (input.isPresent()) {
            WorkPayload.ExecutionInput value = input.orElseThrow();
            writeString(output, value.targetPath());
            writeString(output, value.expectedContentSha256());
        }
    }

    private Optional<WorkPayload.ExecutionInput> readExecutionInput(
            DataInputStream input) throws IOException {
        if (!readPresence(input)) {
            return Optional.empty();
        }
        return Optional.of(new WorkPayload.ExecutionInput(
                readString(input), readString(input)));
    }

    private void writeStringSet(DataOutputStream output, Set<String> values)
            throws IOException {
        List<String> ordered = new ArrayList<>(values);
        ordered.sort(Comparator.naturalOrder());
        output.writeInt(ordered.size());
        for (String value : ordered) {
            writeString(output, value);
        }
    }

    private Set<String> readStringSet(DataInputStream input) throws IOException {
        int size = input.readInt();
        if (size < 0 || size > WorkPayload.MAX_ALLOWED_TOOLS) {
            throw new IOException("allowed tool count is outside supported bounds");
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (int index = 0; index < size; index++) {
            if (!values.add(readString(input))) {
                throw new IOException("allowed tools contain a duplicate value");
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

    private boolean readPresence(DataInputStream input) throws IOException {
        int value = input.readUnsignedByte();
        if (value != 0 && value != 1) {
            throw new IOException("optional marker is invalid");
        }
        return value == 1;
    }

    private void writeString(DataOutputStream output, String value)
            throws IOException {
        byte[] encoded = encodeUtf8(
                Objects.requireNonNull(value, "value must not be null"));
        if (encoded.length > MAX_STRING_BYTES) {
            throw new IOException("submission manifest string exceeds supported bounds");
        }
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private String readString(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_STRING_BYTES || length > input.available()) {
            throw new IOException("submission manifest string length is invalid");
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
                    "submission manifest string is not valid UTF-8",
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
                    "submission manifest string is not valid Unicode text",
                    exception);
        }
    }

    private byte[] envelopeDigest(
            long storedAtMillis,
            int payloadLength,
            byte[] payload) {
        return sha256(ByteBuffer.allocate(
                        Integer.BYTES + Long.BYTES + Integer.BYTES + payload.length)
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
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String canonicalUuid(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        try {
            String canonical = UUID.fromString(value).toString();
            if (!canonical.equals(value)) {
                throw new IllegalArgumentException(name + " must be a canonical UUID");
            }
            return canonical;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(name + " must be a canonical UUID", exception);
        }
    }

    private IOException missing(String submissionId) {
        return new IOException("submission manifest is missing: " + submissionId);
    }

    private CorruptedManifestException corrupted(
            String submissionId,
            String reason) {
        return new CorruptedManifestException(
                "corrupted submission manifest " + submissionId + ": " + reason);
    }

    private CorruptedManifestException corrupted(
            String submissionId,
            String reason,
            Throwable cause) {
        return new CorruptedManifestException(
                "corrupted submission manifest " + submissionId + ": " + reason,
                cause);
    }

    private static final class CorruptedManifestException extends IOException {
        private static final long serialVersionUID = 1L;

        private CorruptedManifestException(String message) {
            super(message);
        }

        private CorruptedManifestException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
