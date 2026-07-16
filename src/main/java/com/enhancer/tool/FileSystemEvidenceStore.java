package com.enhancer.tool;

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
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

public final class FileSystemEvidenceStore implements EvidenceStore {
    private static final int ENVELOPE_MAGIC = 0x454E4832;
    private static final int DIGEST_BYTES = 32;
    private static final int HEADER_BYTES = Integer.BYTES + Long.BYTES + Long.BYTES + DIGEST_BYTES;
    private static final String REFERENCE_PREFIX = "evidence/";
    private static final String FILE_SUFFIX = ".evidence";

    private final Path storageRoot;
    private final EvidenceStoragePolicy storagePolicy;

    public FileSystemEvidenceStore(
            Path storageRoot,
            EvidenceStoragePolicy storagePolicy) {
        Objects.requireNonNull(storageRoot, "storageRoot must not be null");
        this.storageRoot = storageRoot.toAbsolutePath().normalize();
        this.storagePolicy = Objects.requireNonNull(
                storagePolicy,
                "storagePolicy must not be null");
    }

    @Override
    public String createRun() throws IOException {
        Files.createDirectories(storageRoot);
        if (!Files.isDirectory(storageRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("evidence storage root must be a directory");
        }

        for (int attempt = 0; attempt < 10; attempt++) {
            String runId = UUID.randomUUID().toString();
            try {
                Files.createDirectory(storageRoot.resolve(runId));
                return runId;
            } catch (java.nio.file.FileAlreadyExistsException ignored) {
                // Retry the practically impossible UUID collision without weakening uniqueness.
            }
        }
        throw new IOException("could not allocate a unique evidence run identity");
    }

    @Override
    public StoredEvidence persist(String runId, String content) throws IOException {
        StoredEvidence.requireCanonicalUuid(runId, "runId");
        Objects.requireNonNull(content, "content must not be null");

        Path runDirectory = runDirectory(runId);
        if (!Files.isDirectory(runDirectory, LinkOption.NOFOLLOW_LINKS)) {
            throw new MissingEvidenceException("evidence/" + runId);
        }

        if (content.length() > storagePolicy.maxContentBytes()) {
            throw new IOException("evidence content size exceeds storage policy limit");
        }
        byte[] contentBytes = encodeUtf8(content);
        if (contentBytes.length > storagePolicy.maxContentBytes()) {
            throw new IOException("evidence content size exceeds storage policy limit");
        }

        String evidenceId = UUID.randomUUID().toString();
        String reference = reference(runId, evidenceId);
        Instant storedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        byte[] digest = envelopeDigest(storedAt.toEpochMilli(), contentBytes.length, contentBytes);
        ByteBuffer envelope = ByteBuffer.allocate(HEADER_BYTES + contentBytes.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAt.toEpochMilli())
                .putLong(contentBytes.length)
                .put(digest)
                .put(contentBytes);
        envelope.flip();

        Path pending = Files.createTempFile(runDirectory, ".pending-", ".tmp");
        Path destination = artifactPath(runId, evidenceId);
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
                throw new IOException("atomic evidence persistence is not supported", exception);
            }
        } finally {
            Files.deleteIfExists(pending);
        }

        return new StoredEvidence(
                runId,
                evidenceId,
                reference,
                storedAt,
                contentBytes.length,
                HexFormat.of().formatHex(digest));
    }

    @Override
    public ResolvedEvidence resolve(String reference) throws IOException {
        EvidenceIdentity identity = parseReference(reference);
        Path runDirectory = runDirectory(identity.runId());
        if (!Files.exists(runDirectory, LinkOption.NOFOLLOW_LINKS)) {
            throw new MissingEvidenceException(reference);
        }
        if (!Files.isDirectory(runDirectory, LinkOption.NOFOLLOW_LINKS)) {
            throw corrupted(reference, "run directory is not a contained directory");
        }
        Path artifact = artifactPath(identity.runId(), identity.evidenceId());
        if (!Files.exists(artifact, LinkOption.NOFOLLOW_LINKS)) {
            throw new MissingEvidenceException(reference);
        }
        if (!Files.isRegularFile(artifact, LinkOption.NOFOLLOW_LINKS)) {
            throw corrupted(reference, "artifact is not a regular file");
        }

        long artifactSize = Files.size(artifact);
        long maximumArtifactSize = HEADER_BYTES + storagePolicy.maxContentBytes();
        if (artifactSize < HEADER_BYTES || artifactSize > maximumArtifactSize) {
            throw corrupted(reference, "artifact size is outside policy bounds");
        }

        byte[] envelope;
        try {
            envelope = Files.readAllBytes(artifact);
        } catch (NoSuchFileException exception) {
            throw new MissingEvidenceException(reference);
        }
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        if (buffer.getInt() != ENVELOPE_MAGIC) {
            throw corrupted(reference, "envelope header is invalid");
        }

        long storedAtMillis = buffer.getLong();
        long declaredLength = buffer.getLong();
        if (declaredLength < 0
                || declaredLength > storagePolicy.maxContentBytes()
                || declaredLength != buffer.remaining() - DIGEST_BYTES) {
            throw corrupted(reference, "declared content length does not match the artifact");
        }

        byte[] declaredDigest = new byte[DIGEST_BYTES];
        buffer.get(declaredDigest);
        byte[] contentBytes = new byte[(int) declaredLength];
        buffer.get(contentBytes);
        byte[] actualDigest = envelopeDigest(storedAtMillis, declaredLength, contentBytes);
        if (!MessageDigest.isEqual(declaredDigest, actualDigest)) {
            throw corrupted(reference, "envelope digest does not match stored metadata");
        }

        String content = decodeUtf8(reference, contentBytes);
        StoredEvidence metadata = new StoredEvidence(
                identity.runId(),
                identity.evidenceId(),
                reference,
                Instant.ofEpochMilli(storedAtMillis),
                declaredLength,
                HexFormat.of().formatHex(declaredDigest));
        return new ResolvedEvidence(metadata, content);
    }

    @Override
    public EvidenceStoragePolicy storagePolicy() {
        return storagePolicy;
    }

    private EvidenceIdentity parseReference(String reference) {
        Objects.requireNonNull(reference, "reference must not be null");
        if (!reference.startsWith(REFERENCE_PREFIX)) {
            throw new IllegalArgumentException("evidence reference has an invalid prefix");
        }
        String[] segments = reference.split("/", -1);
        if (segments.length != 3 || !segments[0].equals("evidence")) {
            throw new IllegalArgumentException("evidence reference must contain run and evidence identities");
        }
        StoredEvidence.requireCanonicalUuid(segments[1], "runId");
        StoredEvidence.requireCanonicalUuid(segments[2], "evidenceId");
        return new EvidenceIdentity(segments[1], segments[2]);
    }

    private Path runDirectory(String runId) {
        Path runDirectory = storageRoot.resolve(runId).normalize();
        if (!runDirectory.startsWith(storageRoot)) {
            throw new IllegalArgumentException("run identity resolves outside evidence storage");
        }
        return runDirectory;
    }

    private Path artifactPath(String runId, String evidenceId) {
        Path artifact = runDirectory(runId).resolve(evidenceId + FILE_SUFFIX).normalize();
        if (!artifact.startsWith(storageRoot)) {
            throw new IllegalArgumentException("evidence identity resolves outside evidence storage");
        }
        return artifact;
    }

    private String reference(String runId, String evidenceId) {
        return REFERENCE_PREFIX + runId + "/" + evidenceId;
    }

    private String decodeUtf8(String reference, byte[] bytes) throws CorruptedEvidenceException {
        try {
            CharBuffer decoded = StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return decoded.toString();
        } catch (CharacterCodingException exception) {
            throw corrupted(reference, "content is not valid UTF-8", exception);
        }
    }

    private byte[] encodeUtf8(String content) throws IOException {
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8
                    .newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(content));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException exception) {
            throw new IOException("evidence content is not valid Unicode text", exception);
        }
    }

    private byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private byte[] envelopeDigest(long storedAtMillis, long contentLength, byte[] contentBytes) {
        return sha256(ByteBuffer.allocate(
                        Integer.BYTES + Long.BYTES + Long.BYTES + contentBytes.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAtMillis)
                .putLong(contentLength)
                .put(contentBytes)
                .array());
    }

    private CorruptedEvidenceException corrupted(String reference, String reason) {
        return new CorruptedEvidenceException("corrupted evidence " + reference + ": " + reason);
    }

    private CorruptedEvidenceException corrupted(
            String reference,
            String reason,
            Throwable cause) {
        return new CorruptedEvidenceException(
                "corrupted evidence " + reference + ": " + reason,
                cause);
    }

    private record EvidenceIdentity(String runId, String evidenceId) {
    }
}
