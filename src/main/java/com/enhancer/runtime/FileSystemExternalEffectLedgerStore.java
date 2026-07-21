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

/**
 * One-current-snapshot filesystem adapter for a Goal's external-effect ledger. Atomic
 * publication prevents partial visibility; parent-directory power-loss durability is not claimed.
 */
public final class FileSystemExternalEffectLedgerStore
        implements ExternalEffectLedgerStore {
    private static final int ENVELOPE_MAGIC = 0x45464C31;
    private static final int DIGEST_BYTES = 32;
    static final int HEADER_BYTES =
            Integer.BYTES + Long.BYTES + Integer.BYTES + DIGEST_BYTES;
    static final int MAX_STATE_BYTES = 1024 * 1024;
    private static final int MAX_STRING_BYTES = 4 * 1024;
    private static final String PAYLOAD_KIND = "external-effect-ledger";
    private static final String FILE_SUFFIX = ".external-effects";

    private final Path storageRoot;

    public FileSystemExternalEffectLedgerStore(Path storageRoot) {
        Objects.requireNonNull(storageRoot, "storageRoot must not be null");
        this.storageRoot = storageRoot.toAbsolutePath().normalize();
    }

    @Override
    public void create(ExternalEffectLedgerState initialState) throws IOException {
        Objects.requireNonNull(initialState, "initialState must not be null");
        if (initialState.revision() != 0) {
            throw new IllegalArgumentException(
                    "initial external effect ledger revision must be zero");
        }
        if (!initialState.records().isEmpty()) {
            throw new IOException(
                    "Initial external effect ledger must be empty");
        }
        prepareRoot();
        if (Files.exists(
                artifactPath(initialState.goalId()), LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "External effect ledger already exists: "
                            + initialState.goalId());
        }
        publish(initialState, false);
    }

    @Override
    public void update(ExternalEffectLedgerState nextState) throws IOException {
        Objects.requireNonNull(nextState, "nextState must not be null");
        ExternalEffectLedgerState current = resolve(nextState.goalId());
        if (!nextState.isValidSuccessorOf(current)) {
            throw new IOException(
                    "External effect ledger must append one prepared effect or "
                            + "terminate one prepared effect");
        }
        publish(nextState, true);
    }

    @Override
    public ExternalEffectLedgerState resolve(String goalId) throws IOException {
        String canonicalGoalId = RuntimeIdentity.canonicalUuid(goalId, "goalId");
        Path artifact = artifactPath(canonicalGoalId);
        if (!Files.exists(artifact, LinkOption.NOFOLLOW_LINKS)) {
            throw new MissingExternalEffectLedgerException(canonicalGoalId);
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
                    artifact, HEADER_BYTES + MAX_STATE_BYTES);
        } catch (FileSizeLimitExceededException exception) {
            throw corrupted(
                    canonicalGoalId,
                    "artifact grew outside supported bounds while reading",
                    exception);
        } catch (NoSuchFileException exception) {
            throw new MissingExternalEffectLedgerException(canonicalGoalId);
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
                    "External effect ledger storage root must be a directory");
        }
    }

    private void publish(
            ExternalEffectLedgerState state,
            boolean replaceExisting) throws IOException {
        byte[] payload = encode(state);
        if (payload.length > MAX_STATE_BYTES) {
            throw new IOException(
                    "External effect ledger exceeds the supported size limit");
        }
        long storedAtMillis = Instant.now().toEpochMilli();
        byte[] digest = envelopeDigest(
                storedAtMillis, payload.length, payload);
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
                            artifactPath(state.goalId()),
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.move(
                            pending,
                            artifactPath(state.goalId()),
                            StandardCopyOption.ATOMIC_MOVE);
                }
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException(
                        "External effect ledger storage requires atomic move support",
                        exception);
            } catch (FileAlreadyExistsException exception) {
                throw new IOException(
                        "External effect ledger already exists: " + state.goalId(),
                        exception);
            }
        } finally {
            Files.deleteIfExists(pending);
        }
    }

    private byte[] encode(ExternalEffectLedgerState state) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(state.schemaVersion());
            writeString(output, PAYLOAD_KIND);
            writeString(output, state.goalId());
            output.writeLong(state.revision());
            output.writeInt(state.records().size());
            for (ExternalEffectRecord record : state.records()) {
                ExternalEffectRequest request = record.request();
                writeString(output, request.idempotencyKey());
                writeString(output, request.goalId());
                writeString(output, request.agentRunId());
                writeString(output, request.workItemId());
                writeString(output, request.operationName());
                writeString(output, request.operationSha256());
                writeString(output, record.status().name());
            }
        }
        return bytes.toByteArray();
    }

    private ExternalEffectLedgerState decode(
            String expectedGoalId,
            byte[] payload) throws CorruptedExternalEffectLedgerException {
        try (DataInputStream input =
                new DataInputStream(new ByteArrayInputStream(payload))) {
            int schema = input.readInt();
            if (schema != ExternalEffectLedgerState.CURRENT_SCHEMA_VERSION) {
                throw corrupted(
                        expectedGoalId, "state schema version is unsupported");
            }
            if (!PAYLOAD_KIND.equals(readString(input))) {
                throw corrupted(expectedGoalId, "state payload kind is invalid");
            }
            String goalId = readString(input);
            if (!expectedGoalId.equals(goalId)) {
                throw corrupted(expectedGoalId, "state Goal identity does not match");
            }
            long revision = input.readLong();
            int count = input.readInt();
            if (count < 0 || count > ExternalEffectLedgerState.MAX_EFFECTS) {
                throw corrupted(expectedGoalId, "state effect count is invalid");
            }
            List<ExternalEffectRecord> records = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                ExternalEffectRequest request = new ExternalEffectRequest(
                        readString(input),
                        readString(input),
                        readString(input),
                        readString(input),
                        readString(input),
                        readString(input));
                ExternalEffectStatus status = ExternalEffectStatus.valueOf(
                        readString(input));
                records.add(new ExternalEffectRecord(request, status));
            }
            if (input.available() != 0) {
                throw corrupted(expectedGoalId, "state contains trailing bytes");
            }
            return new ExternalEffectLedgerState(
                    schema, goalId, revision, records);
        } catch (CorruptedExternalEffectLedgerException exception) {
            throw exception;
        } catch (EOFException exception) {
            throw corrupted(
                    expectedGoalId,
                    "state ended before all fields were read",
                    exception);
        } catch (IOException | RuntimeException exception) {
            throw corrupted(
                    expectedGoalId, "state could not be decoded", exception);
        }
    }

    private void writeString(DataOutputStream output, String value)
            throws IOException {
        byte[] encoded = encodeUtf8(
                Objects.requireNonNull(value, "value must not be null"));
        if (encoded.length > MAX_STRING_BYTES) {
            throw new IOException(
                    "External effect ledger string exceeds supported bounds");
        }
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private String readString(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_STRING_BYTES || length > input.available()) {
            throw new IOException(
                    "External effect ledger string length is invalid");
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
                    "External effect ledger string is not valid UTF-8",
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
                    "External effect ledger string is not valid Unicode text",
                    exception);
        }
    }

    private Path artifactPath(String goalId) {
        Path artifact = storageRoot.resolve(goalId + FILE_SUFFIX).normalize();
        if (!artifact.startsWith(storageRoot)) {
            throw new IllegalArgumentException(
                    "External effect Goal identity resolves outside storage");
        }
        return artifact;
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

    private CorruptedExternalEffectLedgerException corrupted(
            String goalId,
            String reason) {
        return new CorruptedExternalEffectLedgerException(
                "corrupted external effect ledger " + goalId + ": " + reason);
    }

    private CorruptedExternalEffectLedgerException corrupted(
            String goalId,
            String reason,
            Throwable cause) {
        return new CorruptedExternalEffectLedgerException(
                "corrupted external effect ledger " + goalId + ": " + reason,
                cause);
    }
}
