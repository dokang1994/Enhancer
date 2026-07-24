package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemPendingFinalizationMigrationIntegrationTest {
    private static final int ENVELOPE_MAGIC = 0x50464331;
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000811";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000812";
    private static final String REFERENCE =
            "run-record/00000000-0000-0000-0000-000000000813";
    private static final String OTHER_REFERENCE =
            "run-record/00000000-0000-0000-0000-000000000814";

    @TempDir
    Path temporaryRoot;

    @Test
    void migratesExactSchemaV1ValuesWhileOrdinaryResolutionRemainsFailClosed()
            throws Exception {
        Path root = temporaryRoot.resolve("with-reference");
        byte[] original = schemaV1Envelope(Optional.of(REFERENCE), 1000L);
        writeArtifact(root, original);
        FileSystemPendingFinalizationStore store =
                new FileSystemPendingFinalizationStore(root);

        assertThrows(
                CorruptedPendingFinalizationException.class,
                store::findPending);

        assertEquals(
                PendingFinalizationMigrationResult.MIGRATED,
                store.migrateSchemaV1ToCurrent());
        assertEquals(
                Optional.of(new PendingFinalization(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        Optional.of(REFERENCE),
                        Optional.empty())),
                store.findPending());
        assertFalse(MessageDigest.isEqual(
                original, Files.readAllBytes(artifact(root))));
        assertEquals(List.of("pending.finalization"), fileNames(root));
    }

    @Test
    void migratesSchemaV1WithoutARunRecordReference() throws Exception {
        Path root = temporaryRoot.resolve("without-reference");
        writeArtifact(root, schemaV1Envelope(Optional.empty(), 1001L));
        FileSystemPendingFinalizationStore store =
                new FileSystemPendingFinalizationStore(root);

        assertEquals(
                PendingFinalizationMigrationResult.MIGRATED,
                store.migrateSchemaV1ToCurrent());
        assertEquals(
                Optional.of(new PendingFinalization(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        Optional.empty(),
                        Optional.empty())),
                store.findPending());
    }

    @Test
    void absentAndCurrentArtifactsAreRevisionFreeNonWritingOutcomes()
            throws Exception {
        Path absentRoot = temporaryRoot.resolve("absent");
        assertEquals(
                PendingFinalizationMigrationResult.ABSENT,
                new FileSystemPendingFinalizationStore(absentRoot)
                        .migrateSchemaV1ToCurrent());
        assertFalse(Files.exists(absentRoot));

        Path currentRoot = temporaryRoot.resolve("current");
        FileSystemPendingFinalizationStore currentStore =
                new FileSystemPendingFinalizationStore(currentRoot);
        currentStore.record(new PendingFinalization(
                GOAL_ID, AGENT_RUN_ID, Optional.of(REFERENCE)));
        byte[] before = Files.readAllBytes(artifact(currentRoot));

        assertEquals(
                PendingFinalizationMigrationResult.ALREADY_CURRENT,
                currentStore.migrateSchemaV1ToCurrent());
        assertArrayEquals(before, Files.readAllBytes(artifact(currentRoot)));
        assertEquals(List.of("pending.finalization"), fileNames(currentRoot));
    }

    @Test
    void corruptAndFutureArtifactsFailWithoutChangingTheOriginal()
            throws Exception {
        Path corruptRoot = temporaryRoot.resolve("corrupt");
        byte[] corrupt = schemaV1Envelope(Optional.of(REFERENCE), 1002L);
        corrupt[corrupt.length - 1] ^= 1;
        writeArtifact(corruptRoot, corrupt);

        assertThrows(
                CorruptedPendingFinalizationException.class,
                () -> new FileSystemPendingFinalizationStore(corruptRoot)
                        .migrateSchemaV1ToCurrent());
        assertArrayEquals(corrupt, Files.readAllBytes(artifact(corruptRoot)));
        assertEquals(List.of("pending.finalization"), fileNames(corruptRoot));

        Path futureRoot = temporaryRoot.resolve("future");
        byte[] future = envelope(schemaPayload(
                3, Optional.of(REFERENCE)), 1003L);
        writeArtifact(futureRoot, future);

        CorruptedPendingFinalizationException failure = assertThrows(
                CorruptedPendingFinalizationException.class,
                () -> new FileSystemPendingFinalizationStore(futureRoot)
                        .migrateSchemaV1ToCurrent());
        assertTrue(failure.getMessage().contains("version"));
        assertArrayEquals(future, Files.readAllBytes(artifact(futureRoot)));
        assertEquals(List.of("pending.finalization"), fileNames(futureRoot));
    }

    @Test
    void sourceDriftRefusesPublicationAndPreservesTheChangedSource()
            throws Exception {
        Path root = temporaryRoot.resolve("drift");
        byte[] original = schemaV1Envelope(Optional.of(REFERENCE), 1004L);
        byte[] changed = schemaV1Envelope(
                Optional.of(OTHER_REFERENCE), 1005L);
        writeArtifact(root, original);
        FileSystemPendingFinalizationStore store =
                new FileSystemPendingFinalizationStore(
                        root,
                        source -> Files.write(source, changed));

        assertThrows(
                ConcurrentPendingFinalizationMigrationException.class,
                store::migrateSchemaV1ToCurrent);

        assertArrayEquals(changed, Files.readAllBytes(artifact(root)));
        assertEquals(List.of("pending.finalization"), fileNames(root));
    }

    @Test
    void failureAfterCandidateValidationCleansItAndPreservesTheOriginal()
            throws Exception {
        Path root = temporaryRoot.resolve("candidate-failure");
        byte[] original = schemaV1Envelope(
                Optional.of(REFERENCE), 1006L);
        writeArtifact(root, original);
        FileSystemPendingFinalizationStore store =
                new FileSystemPendingFinalizationStore(
                        root,
                        ignored -> {
                            throw new IOException(
                                    "injected pre-publication failure");
                        });

        assertThrows(
                IOException.class,
                store::migrateSchemaV1ToCurrent);

        assertArrayEquals(original, Files.readAllBytes(artifact(root)));
        assertEquals(List.of("pending.finalization"), fileNames(root));
    }

    private static byte[] schemaV1Envelope(
            Optional<String> reference,
            long storedAtMillis) throws Exception {
        return envelope(schemaPayload(1, reference), storedAtMillis);
    }

    private static byte[] schemaPayload(
            int schemaVersion,
            Optional<String> reference) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(schemaVersion);
            writeString(output, "pending-finalization");
            writeString(output, GOAL_ID);
            writeString(output, AGENT_RUN_ID);
            output.writeBoolean(reference.isPresent());
            if (reference.isPresent()) {
                writeString(output, reference.orElseThrow());
            }
        }
        return bytes.toByteArray();
    }

    private static byte[] envelope(
            byte[] payload,
            long storedAtMillis) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                ByteBuffer.allocate(
                                Integer.BYTES
                                        + Long.BYTES
                                        + Integer.BYTES
                                        + payload.length)
                        .putInt(ENVELOPE_MAGIC)
                        .putLong(storedAtMillis)
                        .putInt(payload.length)
                        .put(payload)
                        .array());
        return ByteBuffer.allocate(
                        FileSystemPendingFinalizationStore.HEADER_BYTES
                                + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAtMillis)
                .putInt(payload.length)
                .put(digest)
                .put(payload)
                .array();
    }

    private static void writeString(
            DataOutputStream output,
            String value) throws Exception {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private static void writeArtifact(Path root, byte[] bytes)
            throws Exception {
        Files.createDirectories(root);
        Files.write(artifact(root), bytes);
    }

    private static Path artifact(Path root) {
        return root.resolve("pending.finalization");
    }

    private static List<String> fileNames(Path root) throws Exception {
        try (var entries = Files.list(root)) {
            return entries.map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }
}
