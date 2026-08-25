package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemSubmissionManifestMigrationInspectionTest {
    private static final int ENVELOPE_MAGIC = 0x45534d31;
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000002511";
    private static final String SUBMISSION_ID =
            "00000000-0000-0000-0000-000000002512";

    @TempDir
    Path temporaryRoot;

    @Test
    void inspectsSchemaV2AsV3WithoutWritingOrCreatingACandidate()
            throws Exception {
        Files.createDirectories(temporaryRoot);
        FileSystemSubmissionManifestStore store =
                new FileSystemSubmissionManifestStore(temporaryRoot);
        Path artifact = store.artifactPath(SUBMISSION_ID);
        byte[] source = envelope(schemaV2Payload());
        Files.write(artifact, source);
        FileTime artifactTime = Files.getLastModifiedTime(artifact);
        FileTime rootTime = Files.getLastModifiedTime(temporaryRoot);

        SubmissionManifestMigrationInspection inspection =
                store.inspectForMigration(SUBMISSION_ID).orElseThrow();

        assertEquals(2, inspection.sourceSchemaVersion());
        assertFalse(inspection.alreadyCurrent());
        assertEquals(expectedManifest(), inspection.manifest());
        assertArrayEquals(source, inspection.sourceBytes());
        assertArrayEquals(source, Files.readAllBytes(artifact));
        assertEquals(artifactTime, Files.getLastModifiedTime(artifact));
        assertEquals(rootTime, Files.getLastModifiedTime(temporaryRoot));
        try (Stream<Path> entries = Files.list(temporaryRoot)) {
            assertEquals(Set.of(artifact), Set.copyOf(entries.toList()));
        }
    }

    @Test
    void inspectsSchemaV1ThroughItsAcceptedNormalPriorityProjection()
            throws Exception {
        Files.createDirectories(temporaryRoot);
        FileSystemSubmissionManifestStore store =
                new FileSystemSubmissionManifestStore(temporaryRoot);
        Path artifact = store.artifactPath(SUBMISSION_ID);
        byte[] source = envelope(schemaV1Payload());
        Files.write(artifact, source);

        SubmissionManifestMigrationInspection inspection =
                store.inspectForMigration(SUBMISSION_ID).orElseThrow();

        assertEquals(1, inspection.sourceSchemaVersion());
        assertFalse(inspection.alreadyCurrent());
        assertEquals(SchedulerPriority.NORMAL, inspection.manifest().priority());
        assertEquals(workMessage(), inspection.manifest().workMessage());
        assertArrayEquals(source, inspection.sourceBytes());
        assertArrayEquals(source, Files.readAllBytes(artifact));
    }

    @Test
    void inspectsCurrentSchemaAndAbsenceWithoutWriting() throws Exception {
        Path absentRoot = temporaryRoot.resolve("absent-manifest");
        FileSystemSubmissionManifestStore store =
                new FileSystemSubmissionManifestStore(absentRoot);

        assertTrue(store.inspectForMigration(SUBMISSION_ID).isEmpty());
        assertFalse(Files.exists(absentRoot));

        DurableSubmissionManifest expected = expectedManifest();
        store.storeIdempotently(expected);
        Path artifact = store.artifactPath(SUBMISSION_ID);
        byte[] source = Files.readAllBytes(artifact);
        FileTime artifactTime = Files.getLastModifiedTime(artifact);
        SubmissionManifestMigrationInspection inspection =
                store.inspectForMigration(SUBMISSION_ID).orElseThrow();

        assertEquals(3, inspection.sourceSchemaVersion());
        assertTrue(inspection.alreadyCurrent());
        assertEquals(expected, inspection.manifest());
        assertArrayEquals(source, inspection.sourceBytes());
        assertArrayEquals(source, Files.readAllBytes(artifact));
        assertEquals(artifactTime, Files.getLastModifiedTime(artifact));
    }

    private static byte[] schemaV1Payload() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(1);
            writeString(output, "durable-submission-manifest");
            writeString(output, QUEUE_ID);
            output.writeInt(8);
            writeString(output, "independent-capability");
            writeLegacyEnvelope(output, workMessage());
        }
        return bytes.toByteArray();
    }

    private static byte[] schemaV2Payload() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(2);
            writeString(output, "durable-submission-manifest");
            writeString(output, QUEUE_ID);
            output.writeInt(8);
            writeString(output, "independent-capability");
            writeString(output, SchedulerPriority.EXPEDITED.name());
            writeLegacyEnvelope(output, workMessage());
        }
        return bytes.toByteArray();
    }

    private static byte[] envelope(byte[] payload) throws Exception {
        long storedAt = 1_777_777_778L;
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                ByteBuffer.allocate(Integer.BYTES + Long.BYTES
                                + Integer.BYTES + payload.length)
                        .putInt(ENVELOPE_MAGIC)
                        .putLong(storedAt)
                        .putInt(payload.length)
                        .put(payload)
                        .array());
        return ByteBuffer.allocate(Integer.BYTES + Long.BYTES + Integer.BYTES
                        + digest.length + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAt)
                .putInt(payload.length)
                .put(digest)
                .put(payload)
                .array();
    }

    private static void writeLegacyEnvelope(
            DataOutputStream output,
            MessageEnvelope envelope) throws Exception {
        writeString(output, MessageEnvelope.ENVELOPE_VERSION);
        writeString(output, envelope.messageId());
        writeString(output, envelope.correlationId());
        output.writeBoolean(false);
        writeString(output, envelope.logicalRunId());
        writeString(output, envelope.producer());
        output.writeLong(envelope.occurredAt().getEpochSecond());
        output.writeInt(envelope.occurredAt().getNano());
        WorkPayload work = (WorkPayload) envelope.payload();
        writeString(output, work.taskRevision().taskId());
        writeString(output, work.taskRevision().sourceDocument());
        writeString(output, work.taskRevision().sourceSha256());
        writeString(output, work.snapshotId());
        output.writeInt(work.allowedTools().size());
        for (String tool : work.allowedTools().stream()
                .sorted(Comparator.naturalOrder()).toList()) {
            writeString(output, tool);
        }
        output.writeBoolean(false);
    }

    private static DurableSubmissionManifest expectedManifest() {
        return new DurableSubmissionManifest(
                QUEUE_ID,
                8,
                "independent-capability",
                workMessage(),
                SchedulerPriority.EXPEDITED);
    }

    private static MessageEnvelope workMessage() {
        return new MessageEnvelope(
                SUBMISSION_ID,
                "manifest-v2-correlation",
                Optional.empty(),
                "manifest-v2-logical-run",
                "manifest-v2-producer",
                Instant.parse("2026-08-25T06:07:08.009000010Z"),
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "manifest-v2-migration",
                                "CURRENT_TASK.md",
                                "a".repeat(64)),
                        "b".repeat(64),
                        Set.of("read-file", "model-invoke")));
    }

    private static void writeString(DataOutputStream output, String value)
            throws Exception {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(encoded.length);
        output.write(encoded);
    }
}
