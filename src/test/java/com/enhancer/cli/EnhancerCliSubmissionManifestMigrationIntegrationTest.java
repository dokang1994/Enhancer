package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.runtime.FileSystemSubmissionManifestStore;
import com.enhancer.runtime.SchedulerPriority;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.PrintStream;
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

class EnhancerCliSubmissionManifestMigrationIntegrationTest {
    private static final int ENVELOPE_MAGIC = 0x45534d31;
    private static final String SUBMISSION_ID =
            "00000000-0000-0000-0000-000000001501";
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000001502";

    @TempDir
    Path temporaryRoot;

    @Test
    void explicitlyMigratesOneManifestAndReportsIdempotentCurrentState()
            throws Exception {
        Path root = temporaryRoot.resolve("submissions");
        Files.createDirectories(root);
        Path artifact =
                root.resolve(SUBMISSION_ID + ".submission-manifest");
        Files.write(artifact, schemaV1Envelope());

        Captured migrated = execute(new String[] {
                "scheduler-migrate-submission-manifest",
                "--submission-root", root.toString(),
                "--submission-id", SUBMISSION_ID
        });

        assertEquals(0, migrated.exitCode());
        assertTrue(migrated.stdout().contains("status=MIGRATED"));
        assertTrue(migrated.stdout().contains(
                "submissionId=" + SUBMISSION_ID));
        assertTrue(migrated.stdout().contains("sourceSchemaVersion=1"));
        assertTrue(migrated.stdout().contains(
                "targetSchemaVersion="
                        + FileSystemSubmissionManifestStore.CURRENT_SCHEMA_VERSION));
        assertEquals(
                SchedulerPriority.NORMAL,
                new FileSystemSubmissionManifestStore(root)
                        .resolve(SUBMISSION_ID)
                        .priority());
        byte[] current = Files.readAllBytes(artifact);

        Captured replay = execute(new String[] {
                "scheduler-migrate-submission-manifest",
                "--submission-root", root.toString(),
                "--submission-id", SUBMISSION_ID
        });

        assertEquals(0, replay.exitCode());
        assertTrue(replay.stdout().contains("status=ALREADY_CURRENT"));
        assertArrayEquals(current, Files.readAllBytes(artifact));
    }

    @Test
    void absentManifestDoesNotCreateTheNamedRoot() {
        Path root = temporaryRoot.resolve("absent");

        Captured absent = execute(new String[] {
                "scheduler-migrate-submission-manifest",
                "--submission-root", root.toString(),
                "--submission-id", SUBMISSION_ID
        });

        assertEquals(0, absent.exitCode());
        assertTrue(absent.stdout().contains("status=ABSENT"));
        assertTrue(absent.stdout().contains("sourceSchemaVersion=NONE"));
        assertFalse(Files.exists(root));
    }

    private byte[] schemaV1Envelope() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(1);
            writeString(output, "durable-submission-manifest");
            writeString(output, QUEUE_ID);
            output.writeInt(8);
            writeString(output, "read-file-worker");
            writeEnvelope(output);
        }
        byte[] payload = bytes.toByteArray();
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
        return ByteBuffer.allocate(
                        Integer.BYTES + Long.BYTES + Integer.BYTES
                                + digest.length + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAt)
                .putInt(payload.length)
                .put(digest)
                .put(payload)
                .array();
    }

    private void writeEnvelope(DataOutputStream output) throws Exception {
        MessageEnvelope envelope = workMessage();
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
        output.writeBoolean(false);
    }

    private MessageEnvelope workMessage() {
        return new MessageEnvelope(
                SUBMISSION_ID,
                "manifest-cli-correlation",
                Optional.empty(),
                "manifest-cli-logical-run",
                "manifest-cli-test",
                Instant.parse("2026-07-27T12:00:00Z"),
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "manifest-cli-task",
                                "CURRENT_TASK.md",
                                "a".repeat(64)),
                        "b".repeat(64),
                        Set.of("read-file")));
    }

    private static void writeString(
            DataOutputStream output,
            String value) throws Exception {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private Captured execute(String[] arguments) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(
                arguments,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Captured(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private record Captured(int exitCode, String stdout, String stderr) {
    }
}
