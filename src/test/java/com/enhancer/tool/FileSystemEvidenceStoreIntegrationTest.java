package com.enhancer.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemEvidenceStoreIntegrationTest {
    private static final int ENVELOPE_MAGIC = 0x454E4832;

    @TempDir
    Path tempDirectory;

    @Test
    void persistsAndResolvesUtf8EvidenceAcrossStoreInstances() throws IOException {
        Path storageRoot = tempDirectory.resolve("evidence");
        EvidenceStoragePolicy policy = policy(64 * 1024);
        FileSystemEvidenceStore writer = new FileSystemEvidenceStore(storageRoot, policy);
        String runId = writer.createRun();
        String content = "complete evidence: verified";

        StoredEvidence stored = writer.persist(runId, content);
        ResolvedEvidence resolved = new FileSystemEvidenceStore(storageRoot, policy)
                .resolve(stored.reference());

        assertEquals(runId, stored.runId());
        assertEquals(UUID.fromString(runId).toString(), runId);
        assertEquals(UUID.fromString(stored.evidenceId()).toString(), stored.evidenceId());
        assertEquals("evidence/" + runId + "/" + stored.evidenceId(), stored.reference());
        assertEquals(content.getBytes(StandardCharsets.UTF_8).length, stored.contentLength());
        assertEquals(64, stored.sha256().length());
        assertEquals(stored, resolved.metadata());
        assertEquals(content, resolved.content());
        assertEquals(policy, writer.storagePolicy());

        try (Stream<Path> paths = Files.walk(storageRoot)) {
            assertFalse(paths.anyMatch(path -> path.getFileName().toString().startsWith(".pending-")));
        }
    }

    @Test
    void generatesUniqueRunAndEvidenceIdentities() throws IOException {
        FileSystemEvidenceStore store = store(1024);
        String firstRun = store.createRun();
        String secondRun = store.createRun();
        StoredEvidence first = store.persist(firstRun, "first");
        StoredEvidence second = store.persist(firstRun, "second");

        assertNotEquals(firstRun, secondRun);
        assertNotEquals(first.evidenceId(), second.evidenceId());
        assertNotEquals(first.reference(), second.reference());
    }

    @Test
    void rejectsMalformedMissingAndOversizedEvidence() throws IOException {
        FileSystemEvidenceStore store = store(16);
        String runId = store.createRun();

        assertThrows(IllegalArgumentException.class, () -> store.resolve("../escape"));
        assertThrows(
                MissingEvidenceException.class,
                () -> store.resolve("evidence/" + runId + "/" + UUID.randomUUID()));
        IOException oversized = assertThrows(
                IOException.class,
                () -> store.persist(runId, "x".repeat(17)));

        assertTrue(oversized.getMessage().contains("size"));
    }

    @Test
    void rejectsDigestMismatchMalformedEnvelopeAndInvalidUtf8() throws IOException {
        Path storageRoot = tempDirectory.resolve("evidence");
        FileSystemEvidenceStore store = new FileSystemEvidenceStore(storageRoot, policy(1024));
        String runId = store.createRun();

        StoredEvidence stored = store.persist(runId, "trusted evidence");
        Path storedPath = artifactPath(storageRoot, stored);
        byte[] corrupted = Files.readAllBytes(storedPath);
        corrupted[corrupted.length - 1] ^= 1;
        Files.write(storedPath, corrupted);
        CorruptedEvidenceException digestFailure = assertThrows(
                CorruptedEvidenceException.class,
                () -> store.resolve(stored.reference()));
        assertTrue(digestFailure.getMessage().contains("digest"));

        StoredEvidence wrongLength = store.persist(runId, "length evidence");
        Path wrongLengthPath = artifactPath(storageRoot, wrongLength);
        byte[] wrongLengthEnvelope = Files.readAllBytes(wrongLengthPath);
        ByteBuffer.wrap(wrongLengthEnvelope).putLong(Integer.BYTES + Long.BYTES, 999);
        Files.write(wrongLengthPath, wrongLengthEnvelope);
        CorruptedEvidenceException lengthFailure = assertThrows(
                CorruptedEvidenceException.class,
                () -> store.resolve(wrongLength.reference()));
        assertTrue(lengthFailure.getMessage().contains("length"));

        String malformedId = UUID.randomUUID().toString();
        Path malformedPath = artifactPath(storageRoot, runId, malformedId);
        Files.writeString(malformedPath, "not an envelope", StandardCharsets.UTF_8);
        assertThrows(
                CorruptedEvidenceException.class,
                () -> store.resolve(reference(runId, malformedId)));

        String invalidUtf8Id = UUID.randomUUID().toString();
        Path invalidUtf8Path = artifactPath(storageRoot, runId, invalidUtf8Id);
        Files.write(invalidUtf8Path, envelope(new byte[] {(byte) 0xC3, 0x28}));
        CorruptedEvidenceException encodingFailure = assertThrows(
                CorruptedEvidenceException.class,
                () -> store.resolve(reference(runId, invalidUtf8Id)));
        assertTrue(encodingFailure.getMessage().contains("UTF-8"));
    }

    @Test
    void rejectsTimestampMetadataTampering() throws IOException {
        Path storageRoot = tempDirectory.resolve("evidence");
        FileSystemEvidenceStore store = new FileSystemEvidenceStore(storageRoot, policy(1024));
        String runId = store.createRun();
        StoredEvidence stored = store.persist(runId, "trusted evidence");
        Path artifact = artifactPath(storageRoot, stored);
        byte[] envelope = Files.readAllBytes(artifact);
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        buffer.putLong(Integer.BYTES, buffer.getLong(Integer.BYTES) + 1);
        Files.write(artifact, envelope);

        CorruptedEvidenceException exception = assertThrows(
                CorruptedEvidenceException.class,
                () -> store.resolve(stored.reference()));

        assertTrue(exception.getMessage().contains("digest"));
    }

    private FileSystemEvidenceStore store(long maxContentBytes) {
        return new FileSystemEvidenceStore(
                tempDirectory.resolve("evidence"),
                policy(maxContentBytes));
    }

    private EvidenceStoragePolicy policy(long maxContentBytes) {
        return new EvidenceStoragePolicy(maxContentBytes);
    }

    private Path artifactPath(Path root, StoredEvidence evidence) {
        return artifactPath(root, evidence.runId(), evidence.evidenceId());
    }

    private Path artifactPath(Path root, String runId, String evidenceId) {
        return root.resolve(runId).resolve(evidenceId + ".evidence");
    }

    private String reference(String runId, String evidenceId) {
        return "evidence/" + runId + "/" + evidenceId;
    }

    private byte[] envelope(byte[] payload) {
        long storedAt = System.currentTimeMillis();
        byte[] digest = sha256(ByteBuffer.allocate(
                        Integer.BYTES + Long.BYTES + Long.BYTES + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAt)
                .putLong(payload.length)
                .put(payload)
                .array());
        return ByteBuffer.allocate(Integer.BYTES + Long.BYTES + Long.BYTES + digest.length + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAt)
                .putLong(payload.length)
                .put(digest)
                .put(payload)
                .array();
    }

    private byte[] sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            assertEquals(64, HexFormat.of().formatHex(digest).length());
            return digest;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
