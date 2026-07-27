package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemSubmissionManifestMigrationTest {
    private static final int ENVELOPE_MAGIC = 0x45534d31;
    private static final String PAYLOAD_KIND = "durable-submission-manifest";
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000f01";
    private static final String SUBMISSION_ID =
            "00000000-0000-0000-0000-000000000f02";

    @TempDir
    Path temporaryRoot;

    @Test
    void migratesExactSchemaV1IntentToNormalSchemaV2() throws Exception {
        FileSystemSubmissionManifestStore store =
                new FileSystemSubmissionManifestStore(temporaryRoot);
        Path artifact = writeSchemaV1(store);

        assertThrows(IOException.class, () -> store.resolve(SUBMISSION_ID));
        assertEquals(
                SubmissionManifestMigrationResult.MIGRATED,
                store.migrateSchemaV1ToCurrent(SUBMISSION_ID));
        DurableSubmissionManifest migrated = store.resolve(SUBMISSION_ID);
        assertEquals(SchedulerPriority.NORMAL, migrated.priority());
        assertEquals(QUEUE_ID, migrated.queueId());
        assertEquals("read-file-worker", migrated.requiredCapability());
        assertEquals(SUBMISSION_ID, migrated.workMessage().messageId());
        assertEquals(
                SubmissionManifestMigrationResult.ALREADY_CURRENT,
                store.migrateSchemaV1ToCurrent(SUBMISSION_ID));
        assertFalse(Files.exists(temporaryRoot.resolve(".manifest-migration-leftover")));
        assertFalse(hasMigrationCandidate());
    }

    @Test
    void absentMigrationDoesNotCreateTheRoot() throws Exception {
        Path absentRoot = temporaryRoot.resolve("absent");
        FileSystemSubmissionManifestStore store =
                new FileSystemSubmissionManifestStore(absentRoot);

        assertEquals(
                SubmissionManifestMigrationResult.ABSENT,
                store.migrateSchemaV1ToCurrent(SUBMISSION_ID));
        assertFalse(Files.exists(absentRoot));
    }

    @Test
    void sourceDriftRefusesReplacementAndCleansCandidate() throws Exception {
        FileSystemSubmissionManifestStore initial =
                new FileSystemSubmissionManifestStore(temporaryRoot);
        Path artifact = writeSchemaV1(initial);
        FileSystemSubmissionManifestStore drifting =
                new FileSystemSubmissionManifestStore(
                        temporaryRoot,
                        Files::delete);

        assertThrows(
                ConcurrentSubmissionManifestMigrationException.class,
                () -> drifting.migrateSchemaV1ToCurrent(SUBMISSION_ID));
        assertFalse(hasMigrationCandidate());
    }

    @Test
    void corruptInputIsPreserved() throws Exception {
        FileSystemSubmissionManifestStore store =
                new FileSystemSubmissionManifestStore(temporaryRoot);
        Path artifact = writeSchemaV1(store);
        byte[] corrupt = Files.readAllBytes(artifact);
        corrupt[corrupt.length - 1] ^= 1;
        Files.write(artifact, corrupt);

        assertThrows(
                IOException.class,
                () -> store.migrateSchemaV1ToCurrent(SUBMISSION_ID));
        assertArrayEquals(corrupt, Files.readAllBytes(artifact));
    }

    private Path writeSchemaV1(FileSystemSubmissionManifestStore store)
            throws Exception {
        Files.createDirectories(temporaryRoot);
        byte[] payload = schemaV1Payload();
        long storedAt = 1_753_631_200_000L;
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                ByteBuffer.allocate(
                                Integer.BYTES + Long.BYTES
                                        + Integer.BYTES + payload.length)
                        .putInt(ENVELOPE_MAGIC)
                        .putLong(storedAt)
                        .putInt(payload.length)
                        .put(payload)
                        .array());
        byte[] envelope = ByteBuffer.allocate(
                        Integer.BYTES + Long.BYTES + Integer.BYTES
                                + digest.length + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAt)
                .putInt(payload.length)
                .put(digest)
                .put(payload)
                .array();
        Path artifact = store.artifactPath(SUBMISSION_ID);
        Files.write(artifact, envelope);
        return artifact;
    }

    private boolean hasMigrationCandidate() throws IOException {
        try (var paths = Files.list(temporaryRoot)) {
            return paths.map(path -> path.getFileName().toString())
                    .anyMatch(name -> name.startsWith(
                            ".submission-migration-"));
        }
    }

    private byte[] schemaV1Payload() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(1);
            writeString(output, PAYLOAD_KIND);
            writeString(output, QUEUE_ID);
            output.writeInt(8);
            writeString(output, "read-file-worker");
            writeEnvelope(output, workMessage());
        }
        return bytes.toByteArray();
    }

    private void writeEnvelope(
            DataOutputStream output,
            MessageEnvelope envelope) throws IOException {
        writeString(output, MessageEnvelope.ENVELOPE_VERSION);
        writeString(output, envelope.messageId());
        writeString(output, envelope.correlationId());
        output.writeBoolean(false);
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
        output.writeInt(work.allowedTools().size());
        for (String allowed : work.allowedTools().stream()
                .sorted(Comparator.naturalOrder()).toList()) {
            writeString(output, allowed);
        }
        output.writeBoolean(work.executionInput().isPresent());
        WorkPayload.ExecutionInput input =
                work.executionInput().orElseThrow();
        writeString(output, input.targetPath());
        writeString(output, input.expectedContentSha256());
    }

    private void writeString(DataOutputStream output, String value)
            throws IOException {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private MessageEnvelope workMessage() {
        return new MessageEnvelope(
                SUBMISSION_ID,
                "manifest-migration-correlation",
                Optional.empty(),
                "manifest-migration-logical-run",
                "manifest-migration-test",
                Instant.parse("2026-07-27T12:00:00Z"),
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "manifest-migration-task",
                                "CURRENT_TASK.md",
                                "a".repeat(64)),
                        "b".repeat(64),
                        Set.of("read-file"),
                        Optional.of(new WorkPayload.ExecutionInput(
                                "CURRENT_TASK.md",
                                "c".repeat(64)))));
    }
}
