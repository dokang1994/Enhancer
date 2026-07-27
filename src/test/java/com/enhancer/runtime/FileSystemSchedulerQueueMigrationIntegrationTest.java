package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemSchedulerQueueMigrationIntegrationTest {
    private static final int ENVELOPE_MAGIC = 0x45535131;
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000001301";
    private static final String FIRST_ID =
            "00000000-0000-0000-0000-000000001311";
    private static final String SECOND_ID =
            "00000000-0000-0000-0000-000000001312";
    private static final String LOGICAL_RUN = "queue-migration-run";

    @TempDir
    Path temporaryRoot;

    @Test
    void migratesExactSchemaV2StateWhileOrdinaryResolutionRemainsFailClosed()
            throws Exception {
        Path root = temporaryRoot.resolve("exact");
        QueuedWork first = new QueuedWork(workItem(FIRST_ID), List.of());
        QueuedWork second = new QueuedWork(workItem(SECOND_ID), List.of());
        byte[] original = schemaV2Envelope(
                List.of(first, second),
                List.of(first),
                Optional.of(second),
                1000L);
        writeArtifact(root, original);
        FileSystemSchedulerQueueStore store =
                new FileSystemSchedulerQueueStore(root);

        assertThrows(
                CorruptedSchedulerQueueStateException.class,
                () -> store.resolve(QUEUE_ID));

        assertEquals(
                SchedulerQueueMigrationResult.MIGRATED,
                store.migrateSchemaV2ToCurrent(QUEUE_ID));
        SchedulerQueueState migrated = store.resolve(QUEUE_ID);
        assertEquals(3, migrated.schemaVersion());
        assertEquals(7, migrated.revision());
        assertEquals(8, migrated.maxWorkItems());
        assertEquals(Optional.of(LOGICAL_RUN), migrated.logicalRunId());
        assertEquals(List.of(FIRST_ID, SECOND_ID), migrated.admissionOrder());
        assertEquals(List.of(first, second), migrated.admittedWork());
        assertEquals(List.of(first), migrated.pendingWork());
        assertEquals(Optional.of(second), migrated.activeWork());
        assertTrue(migrated.admittedWork().stream()
                .allMatch(work -> work.priority() == SchedulerPriority.NORMAL));
        assertEquals(4, migrated.maximumExpeditedBurst());
        assertEquals(0, migrated.consecutiveExpeditedClaims());
        assertTrue(migrated.recoveryPreferredWorkItemId().isEmpty());

        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID,
                        new FileSystemSchedulerQueueStore(root));
        SchedulerQueueState recoveryState = store.resolve(QUEUE_ID);
        assertEquals(Optional.of(SECOND_ID),
                recoveryState.recoveryPreferredWorkItemId());
        assertEquals(0, recoveryState.consecutiveExpeditedClaims());

        assertEquals(SECOND_ID,
                recovered.claimNext().orElseThrow().workItemId());
        SchedulerQueueState reclaimed = store.resolve(QUEUE_ID);
        assertTrue(reclaimed.recoveryPreferredWorkItemId().isEmpty());
        assertEquals(0, reclaimed.consecutiveExpeditedClaims());
    }

    @Test
    void absentAndCurrentArtifactsAreIdempotentNonWritingOutcomes()
            throws Exception {
        Path absentRoot = temporaryRoot.resolve("absent");
        assertEquals(
                SchedulerQueueMigrationResult.ABSENT,
                new FileSystemSchedulerQueueStore(absentRoot)
                        .migrateSchemaV2ToCurrent(QUEUE_ID));
        assertFalse(Files.exists(absentRoot));

        Path currentRoot = temporaryRoot.resolve("current");
        FileSystemSchedulerQueueStore current =
                new FileSystemSchedulerQueueStore(currentRoot);
        current.create(SchedulerQueueState.initial(QUEUE_ID, 8));
        byte[] before = Files.readAllBytes(artifact(currentRoot));

        assertEquals(
                SchedulerQueueMigrationResult.ALREADY_CURRENT,
                current.migrateSchemaV2ToCurrent(QUEUE_ID));
        assertArrayEquals(before, Files.readAllBytes(artifact(currentRoot)));
        assertFalse(hasMigrationCandidate(currentRoot));
    }

    @Test
    void corruptAndFutureArtifactsFailWithoutChangingTheOriginal()
            throws Exception {
        Path corruptRoot = temporaryRoot.resolve("corrupt");
        byte[] corrupt = schemaV2Envelope(
                List.of(), List.of(), Optional.empty(), 1001L);
        corrupt[corrupt.length - 1] ^= 1;
        writeArtifact(corruptRoot, corrupt);

        assertThrows(
                CorruptedSchedulerQueueStateException.class,
                () -> new FileSystemSchedulerQueueStore(corruptRoot)
                        .migrateSchemaV2ToCurrent(QUEUE_ID));
        assertArrayEquals(corrupt, Files.readAllBytes(artifact(corruptRoot)));
        assertFalse(hasMigrationCandidate(corruptRoot));

        Path futureRoot = temporaryRoot.resolve("future");
        byte[] future = schemaEnvelope(
                4, List.of(), List.of(), Optional.empty(), 1002L);
        writeArtifact(futureRoot, future);

        CorruptedSchedulerQueueStateException failure = assertThrows(
                CorruptedSchedulerQueueStateException.class,
                () -> new FileSystemSchedulerQueueStore(futureRoot)
                        .migrateSchemaV2ToCurrent(QUEUE_ID));
        assertTrue(failure.getMessage().contains("version"));
        assertArrayEquals(future, Files.readAllBytes(artifact(futureRoot)));
        assertFalse(hasMigrationCandidate(futureRoot));
    }

    @Test
    void sourceDriftRefusesPublicationAndPreservesTheChangedSource()
            throws Exception {
        Path root = temporaryRoot.resolve("drift");
        byte[] original = schemaV2Envelope(
                List.of(), List.of(), Optional.empty(), 1003L);
        byte[] changed = schemaV2Envelope(
                List.of(), List.of(), Optional.empty(), 1004L);
        writeArtifact(root, original);
        FileSystemSchedulerQueueStore store =
                new FileSystemSchedulerQueueStore(
                        root,
                        source -> Files.write(source, changed));

        assertThrows(
                ConcurrentSchedulerQueueMigrationException.class,
                () -> store.migrateSchemaV2ToCurrent(QUEUE_ID));
        assertArrayEquals(changed, Files.readAllBytes(artifact(root)));
        assertFalse(hasMigrationCandidate(root));
    }

    @Test
    void failureAfterCandidateValidationCleansItAndPreservesTheOriginal()
            throws Exception {
        Path root = temporaryRoot.resolve("candidate-failure");
        byte[] original = schemaV2Envelope(
                List.of(), List.of(), Optional.empty(), 1005L);
        writeArtifact(root, original);
        FileSystemSchedulerQueueStore store =
                new FileSystemSchedulerQueueStore(
                        root,
                        ignored -> {
                            throw new IOException(
                                    "injected pre-publication failure");
                        });

        assertThrows(
                IOException.class,
                () -> store.migrateSchemaV2ToCurrent(QUEUE_ID));
        assertArrayEquals(original, Files.readAllBytes(artifact(root)));
        assertFalse(hasMigrationCandidate(root));
    }

    private static byte[] schemaV2Envelope(
            List<QueuedWork> admitted,
            List<QueuedWork> pending,
            Optional<QueuedWork> active,
            long storedAtMillis) throws Exception {
        return schemaEnvelope(2, admitted, pending, active, storedAtMillis);
    }

    private static byte[] schemaEnvelope(
            int schemaVersion,
            List<QueuedWork> admitted,
            List<QueuedWork> pending,
            Optional<QueuedWork> active,
            long storedAtMillis) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(schemaVersion);
            writeString(output, "scheduler-queue-state");
            writeString(output, QUEUE_ID);
            output.writeLong(7);
            output.writeInt(8);
            output.writeBoolean(!admitted.isEmpty());
            if (!admitted.isEmpty()) {
                writeString(output, LOGICAL_RUN);
            }
            writeStringList(output, admitted.stream()
                    .map(work -> work.workItem().workItemId())
                    .toList());
            writeQueuedWorkList(output, admitted);
            writeQueuedWorkList(output, pending);
            output.writeBoolean(active.isPresent());
            if (active.isPresent()) {
                writeQueuedWork(output, active.orElseThrow());
            }
            writeStringSet(output, Set.of());
            writeStringSet(output, Set.of());
        }
        byte[] payload = bytes.toByteArray();
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
                        FileSystemSchedulerQueueStore.HEADER_BYTES
                                + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAtMillis)
                .putInt(payload.length)
                .put(digest)
                .put(payload)
                .array();
    }

    private static void writeQueuedWorkList(
            DataOutputStream output,
            List<QueuedWork> work) throws Exception {
        output.writeInt(work.size());
        for (QueuedWork queuedWork : work) {
            writeQueuedWork(output, queuedWork);
        }
    }

    private static void writeQueuedWork(
            DataOutputStream output,
            QueuedWork queuedWork) throws Exception {
        WorkItem workItem = queuedWork.workItem();
        writeString(output, workItem.workItemId());
        writeString(output, workItem.requiredCapability());
        MessageEnvelope envelope = workItem.workMessage();
        writeString(output, MessageEnvelope.ENVELOPE_VERSION);
        writeString(output, envelope.messageId());
        writeString(output, envelope.correlationId());
        output.writeBoolean(envelope.causationId().isPresent());
        if (envelope.causationId().isPresent()) {
            writeString(output, envelope.causationId().orElseThrow());
        }
        writeString(output, envelope.logicalRunId());
        writeString(output, envelope.producer());
        output.writeLong(envelope.occurredAt().getEpochSecond());
        output.writeInt(envelope.occurredAt().getNano());
        WorkPayload payload = (WorkPayload) envelope.payload();
        writeString(output, payload.taskRevision().taskId());
        writeString(output, payload.taskRevision().sourceDocument());
        writeString(output, payload.taskRevision().sourceSha256());
        writeString(output, payload.snapshotId());
        writeStringSet(output, payload.allowedTools());
        output.writeBoolean(payload.executionInput().isPresent());
        if (payload.executionInput().isPresent()) {
            WorkPayload.ExecutionInput input =
                    payload.executionInput().orElseThrow();
            writeString(output, input.targetPath());
            writeString(output, input.expectedContentSha256());
        }
        writeStringSet(output, queuedWork.dependencyWorkItemIds());
    }

    private static void writeStringList(
            DataOutputStream output,
            List<String> values) throws Exception {
        output.writeInt(values.size());
        for (String value : values) {
            writeString(output, value);
        }
    }

    private static void writeStringSet(
            DataOutputStream output,
            Set<String> values) throws Exception {
        output.writeInt(values.size());
        for (String value : values) {
            writeString(output, value);
        }
    }

    private static void writeString(
            DataOutputStream output,
            String value) throws Exception {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private static WorkItem workItem(String workItemId) {
        long suffix = Long.parseLong(
                workItemId.substring(workItemId.length() - 12));
        return new WorkItem(
                workItemId,
                "read-file-worker",
                new MessageEnvelope(
                        String.format(
                                "00000000-0000-0000-0002-%012d",
                                suffix),
                        "queue-migration-correlation",
                        Optional.empty(),
                        LOGICAL_RUN,
                        "queue-migration-test",
                        Instant.parse("2026-07-27T12:00:00Z"),
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "queue-schema-v3-migration",
                                        "CURRENT_TASK.md",
                                        "a".repeat(64)),
                                "b".repeat(64),
                                Set.of("read-file"))));
    }

    private static void writeArtifact(Path root, byte[] bytes)
            throws Exception {
        Files.createDirectories(root);
        Files.write(artifact(root), bytes);
    }

    private static Path artifact(Path root) {
        return root.resolve(QUEUE_ID + ".scheduler-queue");
    }

    private static boolean hasMigrationCandidate(Path root)
            throws Exception {
        try (var entries = Files.list(root)) {
            return entries.anyMatch(path -> path.getFileName().toString()
                    .startsWith(".queue-migration-"));
        }
    }
}
