package com.enhancer.runtime;

import com.enhancer.bus.ControlSignal;
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
import java.util.Objects;
import java.util.Optional;

/** Bounded integrity-checked filesystem adapter for authorization audit points. */
public final class FileSystemCancellationAuthorizationAuditStore
        implements CancellationAuthorizationAuditStore {
    private static final int ENVELOPE_MAGIC = 0x43414131;
    private static final int DIGEST_BYTES = 32;
    static final int HEADER_BYTES = Integer.BYTES + Integer.BYTES + DIGEST_BYTES;
    static final int MAX_STATE_BYTES = 16 * 1024;
    private static final int MAX_STRING_BYTES = 4 * 1024;
    private static final int STORE_SCHEMA_VERSION = 1;
    private static final String PAYLOAD_KIND = "cancellation-authorization-audit";
    private static final String FILE_SUFFIX = ".cancellation-authorization";

    private final Path storageRoot;

    public FileSystemCancellationAuthorizationAuditStore(Path storageRoot) {
        this.storageRoot = Objects.requireNonNull(
                storageRoot, "storageRoot must not be null")
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public CancellationAuthorizationAuditRecord persist(
            CancellationAuthorizationAuditRecord audit) throws IOException {
        CancellationAuthorizationAuditRecord checked = Objects.requireNonNull(
                audit, "audit must not be null");
        Path artifact = artifactPath(checked.authorizationId());
        Optional<CancellationAuthorizationAuditRecord> existing = readIfPresent(artifact);
        if (existing.isPresent()) {
            CancellationAuthorizationAuditRecord resolved = existing.orElseThrow();
            if (!resolved.equals(checked)) {
                throw new IOException(
                        "cancellation authorization identity is bound to different audit");
            }
            return resolved;
        }

        prepareRoot();
        byte[] payload = encode(checked);
        if (payload.length > MAX_STATE_BYTES) {
            throw new IOException(
                    "cancellation authorization audit exceeds supported bound");
        }
        byte[] digest = sha256(payload);
        ByteBuffer envelope = ByteBuffer.allocate(HEADER_BYTES + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putInt(payload.length)
                .put(digest)
                .put(payload);
        envelope.flip();
        Path candidate = Files.createTempFile(
                storageRoot, ".cancellation-authorization-", ".tmp");
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
                CancellationAuthorizationAuditRecord resolved =
                        resolve(checked.reference());
                if (!resolved.equals(checked)) {
                    throw new IOException(
                            "cancellation authorization identity was concurrently reused",
                            raced);
                }
                return resolved;
            } catch (AtomicMoveNotSupportedException unsupported) {
                throw new IOException(
                        "cancellation authorization storage requires atomic move support",
                        unsupported);
            }
        } finally {
            Files.deleteIfExists(candidate);
        }
        return resolve(checked.reference());
    }

    @Override
    public Optional<CancellationAuthorizationAuditRecord> find(String authorizationId)
            throws IOException {
        String canonicalId = RuntimeIdentity.canonicalUuid(
                authorizationId, "authorizationId");
        Optional<CancellationAuthorizationAuditRecord> found = readIfPresent(
                artifactPath(canonicalId));
        if (found.isPresent()
                && !found.orElseThrow().authorizationId().equals(canonicalId)) {
            throw corrupted("artifact identity does not match its point");
        }
        return found;
    }

    @Override
    public CancellationAuthorizationAuditRecord resolve(String reference)
            throws IOException {
        String authorizationId = parseReference(reference);
        return find(authorizationId).orElseThrow(() ->
                new IOException("cancellation authorization audit is missing: " + reference));
    }

    private Optional<CancellationAuthorizationAuditRecord> readIfPresent(Path artifact)
            throws IOException {
        if (!hasReadableRoot()) {
            return Optional.empty();
        }
        if (!Files.exists(artifact, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        if (!Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(artifact)) {
            throw corrupted("artifact must be a regular non-symbolic file");
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
            throw corrupted("envelope magic is invalid");
        }
        int payloadLength = buffer.getInt();
        if (payloadLength < 0
                || payloadLength > MAX_STATE_BYTES
                || payloadLength != buffer.remaining() - DIGEST_BYTES) {
            throw corrupted("declared payload length does not match artifact");
        }
        byte[] declaredDigest = new byte[DIGEST_BYTES];
        buffer.get(declaredDigest);
        byte[] payload = new byte[payloadLength];
        buffer.get(payload);
        if (!MessageDigest.isEqual(declaredDigest, sha256(payload))) {
            throw corrupted("envelope digest does not match payload");
        }
        CancellationAuthorizationAuditRecord audit = decode(payload);
        if (!artifact.equals(artifactPath(audit.authorizationId()))) {
            throw corrupted("artifact path does not match audit identity");
        }
        return Optional.of(audit);
    }

    private void prepareRoot() throws IOException {
        Path parent = storageRoot.getParent();
        if (parent == null || !isExactRealDirectory(parent)) {
            throw new IOException(
                    "cancellation authorization parent must be an existing exact real directory");
        }
        if (Files.exists(storageRoot, LinkOption.NOFOLLOW_LINKS)) {
            if (!isExactRealDirectory(storageRoot)) {
                throw new IOException(
                        "cancellation authorization storage root must be non-symbolic directory");
            }
        } else {
            Files.createDirectory(storageRoot);
            if (!isExactRealDirectory(storageRoot)) {
                throw new IOException(
                        "cancellation authorization storage root is not exact real directory");
            }
        }
    }

    private boolean hasReadableRoot() throws IOException {
        Path parent = storageRoot.getParent();
        if (parent == null) {
            throw corrupted("storage root requires a parent");
        }
        if (!Files.exists(parent, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        if (!isExactRealDirectory(parent)) {
            throw corrupted(
                    "storage parent must be a non-symbolic exact real directory");
        }
        if (!Files.exists(storageRoot, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        if (!isExactRealDirectory(storageRoot)) {
            throw corrupted(
                    "storage root must be a non-symbolic exact real directory");
        }
        return true;
    }

    private boolean isExactRealDirectory(Path path) throws IOException {
        return Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
                && path.toRealPath().equals(path.toAbsolutePath().normalize());
    }

    private Path artifactPath(String authorizationId) {
        return storageRoot.resolve(authorizationId + FILE_SUFFIX);
    }

    private byte[] encode(CancellationAuthorizationAuditRecord audit)
            throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(STORE_SCHEMA_VERSION);
            writeString(output, PAYLOAD_KIND);
            writeString(output, audit.schemaVersion());
            writeString(output, audit.authorizationId());
            writeString(output, audit.audience());
            writeString(output, audit.issuerId());
            writeString(output, audit.subjectId());
            writeString(output, audit.actorId());
            writeString(output, audit.goalId());
            writeString(output, audit.controlMessageId());
            writeString(output, audit.signal().name());
            writeString(output, audit.requestSha256());
            writeString(output, audit.proofSha256());
            writeString(output, audit.keyId());
            writeString(output, audit.publicKeySha256());
            writeString(output, audit.signatureAlgorithm());
            writeString(output, audit.trustConfigurationId());
            writeString(output, audit.trustConfigurationRevision());
            writeString(output, audit.policyRevision());
            writeInstant(output, audit.issuedAt());
            writeInstant(output, audit.expiresAt());
            writeInstant(output, audit.verifiedAt());
            writeOptionalInstant(output, audit.keyRevokedAt());
            writeString(output, audit.verifierVersion());
        }
        return bytes.toByteArray();
    }

    private CancellationAuthorizationAuditRecord decode(byte[] payload)
            throws IOException {
        try (DataInputStream input = new DataInputStream(
                new ByteArrayInputStream(payload))) {
            if (input.readInt() != STORE_SCHEMA_VERSION) {
                throw corrupted("store schema version is unsupported");
            }
            if (!PAYLOAD_KIND.equals(readString(input))) {
                throw corrupted("payload kind is invalid");
            }
            CancellationAuthorizationAuditRecord audit =
                    new CancellationAuthorizationAuditRecord(
                            readString(input),
                            readString(input),
                            readString(input),
                            readString(input),
                            readString(input),
                            readString(input),
                            readString(input),
                            readString(input),
                            readEnum(input, ControlSignal.class),
                            readString(input),
                            readString(input),
                            readString(input),
                            readString(input),
                            readString(input),
                            readString(input),
                            readString(input),
                            readString(input),
                            readInstant(input),
                            readInstant(input),
                            readInstant(input),
                            readOptionalInstant(input),
                            readString(input));
            if (input.read() != -1) {
                throw corrupted("payload contains trailing bytes");
            }
            return audit;
        } catch (EOFException exception) {
            throw corrupted("payload ended before all fields were read", exception);
        } catch (IOException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw corrupted("payload violates authorization audit contract", exception);
        }
    }

    private void writeString(DataOutputStream output, String value) throws IOException {
        byte[] encoded = encodeUtf8(value);
        if (encoded.length > MAX_STRING_BYTES) {
            throw new IOException(
                    "cancellation authorization string exceeds supported bound");
        }
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private String readString(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_STRING_BYTES || length > input.available()) {
            throw corrupted("string length is invalid");
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
            throw corrupted("string is not valid UTF-8", exception);
        }
    }

    private byte[] encodeUtf8(String value) throws IOException {
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(Objects.requireNonNull(
                            value, "value must not be null")));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException exception) {
            throw new IOException(
                    "cancellation authorization text is not valid Unicode", exception);
        }
    }

    private void writeInstant(DataOutputStream output, Instant value) throws IOException {
        output.writeLong(value.getEpochSecond());
        output.writeInt(value.getNano());
    }

    private Instant readInstant(DataInputStream input) throws IOException {
        try {
            return Instant.ofEpochSecond(input.readLong(), input.readInt());
        } catch (RuntimeException exception) {
            throw corrupted("instant is invalid", exception);
        }
    }

    private void writeOptionalInstant(
            DataOutputStream output, Optional<Instant> value) throws IOException {
        output.writeBoolean(value.isPresent());
        if (value.isPresent()) {
            writeInstant(output, value.orElseThrow());
        }
    }

    private Optional<Instant> readOptionalInstant(DataInputStream input)
            throws IOException {
        return input.readBoolean() ? Optional.of(readInstant(input)) : Optional.empty();
    }

    private <E extends Enum<E>> E readEnum(DataInputStream input, Class<E> type)
            throws IOException {
        String name = readString(input);
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException exception) {
            throw corrupted("enum value is unsupported", exception);
        }
    }

    private String parseReference(String reference) throws IOException {
        String checked = Objects.requireNonNull(reference, "reference must not be null");
        String[] parts = checked.split("/", -1);
        if (parts.length != 2 || !"cancellation-authorization".equals(parts[0])) {
            throw new IOException("cancellation authorization reference is invalid");
        }
        try {
            return RuntimeIdentity.canonicalUuid(parts[1], "authorizationId");
        } catch (IllegalArgumentException exception) {
            throw new IOException(
                    "cancellation authorization reference is invalid", exception);
        }
    }

    private byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private IOException corrupted(String reason) {
        return new IOException("cancellation authorization audit is corrupt: " + reason);
    }

    private IOException corrupted(String reason, Throwable cause) {
        return new IOException(
                "cancellation authorization audit is corrupt: " + reason, cause);
    }
}
