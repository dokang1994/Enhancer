package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemSchedulerQueueStoreIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000301";
    private static final int DIGEST_OFFSET =
            Integer.BYTES + Long.BYTES + Integer.BYTES;
    private static final int PAYLOAD_OFFSET = DIGEST_OFFSET + 32;

    @TempDir
    Path storageRoot;

    @Test
    void createsUpdatesAndResolvesAcrossStoreInstances() throws Exception {
        FileSystemSchedulerQueueStore store =
                new FileSystemSchedulerQueueStore(storageRoot);
        SchedulerQueueState initial = SchedulerQueueState.initial(QUEUE_ID, 8);
        store.create(initial);

        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID,
                        new FileSystemSchedulerQueueStore(storageRoot));

        assertEquals(QUEUE_ID, queue.queueId());
        assertEquals(0, queue.revision());
        assertEquals(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                store.resolve(QUEUE_ID).schemaVersion());
        assertThrows(IOException.class, () -> store.create(initial));
        assertThrows(IOException.class, () -> store.update(initial));
    }

    @Test
    void refusesUpdateWhileAnotherJvmOwnsTheQueueWriterLock()
            throws Exception {
        FileSystemSchedulerQueueStore store =
                new FileSystemSchedulerQueueStore(storageRoot);
        SchedulerQueueState initial = SchedulerQueueState.initial(QUEUE_ID, 8);
        store.create(initial);
        SingleWorkerSchedulerQueue candidate =
                new SingleWorkerSchedulerQueue(initial);
        candidate.enqueue(new QueuedWork(
                workItem(
                        "00000000-0000-0000-0000-000000000302",
                        "reader"),
                List.of()));
        SchedulerQueueState next = candidate.snapshot(QUEUE_ID, 1);

        Process lockHolder = startLockHolder();
        try {
            BufferedReader output = new BufferedReader(new InputStreamReader(
                    lockHolder.getInputStream(), StandardCharsets.UTF_8));
            assertEquals("LOCKED", output.readLine());

            ConcurrentSchedulerQueueUpdateException conflict = assertThrows(
                    ConcurrentSchedulerQueueUpdateException.class,
                    () -> store.update(next));

            assertTrue(conflict.getMessage().contains(QUEUE_ID));
            SchedulerQueueState unchanged = store.resolve(QUEUE_ID);
            assertEquals(initial.revision(), unchanged.revision());
            assertEquals(initial.admittedWork(), unchanged.admittedWork());
            assertEquals(initial.pendingWork(), unchanged.pendingWork());
            assertEquals(initial.activeWork(), unchanged.activeWork());
        } finally {
            lockHolder.getOutputStream().close();
            if (!lockHolder.waitFor(5, TimeUnit.SECONDS)) {
                lockHolder.destroyForcibly();
                assertTrue(lockHolder.waitFor(5, TimeUnit.SECONDS));
            }
        }
        assertEquals(0, lockHolder.exitValue());
    }

    @Test
    void staleStoreInstanceCannotOverwriteACommittedRevision() throws Exception {
        FileSystemSchedulerQueueStore store =
                new FileSystemSchedulerQueueStore(storageRoot);
        DurableSingleWorkerSchedulerQueue first =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store);
        DurableSingleWorkerSchedulerQueue stale =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID,
                        new FileSystemSchedulerQueueStore(storageRoot));
        WorkItem committed = workItem(
                "00000000-0000-0000-0000-000000000303",
                "reader");
        WorkItem refused = workItem(
                "00000000-0000-0000-0000-000000000304",
                "reviewer");

        first.enqueue(new QueuedWork(committed, List.of()));
        assertThrows(
                IOException.class,
                () -> stale.enqueue(new QueuedWork(refused, List.of())));

        SchedulerQueueState resolved = store.resolve(QUEUE_ID);
        assertEquals(1, resolved.revision());
        assertEquals(
                List.of(new QueuedWork(committed, List.of())),
                resolved.admittedWork());
    }

    @Test
    void restoresExactWorkAndRequeuesActiveStateFromTheFilesystem()
            throws Exception {
        String firstId =
                "00000000-0000-0000-0000-000000000311";
        String secondId =
                "00000000-0000-0000-0000-000000000312";
        FileSystemSchedulerQueueStore store =
                new FileSystemSchedulerQueueStore(storageRoot);
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        8,
                        store);
        WorkItem first = workItem(firstId, "reader-\uD83D\uDE80");
        WorkItem second = workItem(secondId, "reviewer");
        queue.enqueue(new QueuedWork(first, List.of()));
        queue.enqueue(new QueuedWork(second, List.of(firstId)));
        queue.claimNext().orElseThrow();

        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID,
                        new FileSystemSchedulerQueueStore(storageRoot));
        WorkItem restored = recovered.claimNext().orElseThrow();

        assertEquals(first, restored);
        assertEquals(first.workMessage(), restored.workMessage());
        assertEquals(Set.of("read-file", "verify"), restored.allowedTools());
        assertEquals("reader-\uD83D\uDE80", restored.requiredCapability());
        recovered.completeActiveVerified(firstId);
        DurableSingleWorkerSchedulerQueue finalRecovery =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID,
                        new FileSystemSchedulerQueueStore(storageRoot));
        assertEquals(second, finalRecovery.claimNext().orElseThrow());
    }

    @Test
    void rejectsMissingCorruptTrailingAndUnsupportedState() throws Exception {
        FileSystemSchedulerQueueStore store =
                new FileSystemSchedulerQueueStore(storageRoot);
        store.create(SchedulerQueueState.initial(QUEUE_ID, 8));
        Path artifact = artifact(QUEUE_ID);

        byte[] corrupt = Files.readAllBytes(artifact);
        corrupt[corrupt.length - 1] ^= 0x01;
        Files.write(artifact, corrupt);
        assertThrows(CorruptedSchedulerQueueStateException.class, () ->
                store.resolve(QUEUE_ID));

        Files.delete(artifact);
        store.create(SchedulerQueueState.initial(QUEUE_ID, 8));
        byte[] trailing = Files.readAllBytes(artifact);
        Files.write(artifact, ByteBuffer.allocate(trailing.length + 1)
                .put(trailing)
                .put((byte) 1)
                .array());
        assertThrows(CorruptedSchedulerQueueStateException.class, () ->
                store.resolve(QUEUE_ID));

        Files.delete(artifact);
        store.create(SchedulerQueueState.initial(QUEUE_ID, 8));
        byte[] priorSchema = Files.readAllBytes(artifact);
        ByteBuffer.wrap(priorSchema).putInt(
                PAYLOAD_OFFSET,
                1);
        replaceDigest(priorSchema);
        Files.write(artifact, priorSchema);
        CorruptedSchedulerQueueStateException exception = assertThrows(
                CorruptedSchedulerQueueStateException.class,
                () -> store.resolve(QUEUE_ID));
        assertTrue(exception.getMessage().contains("version"));

        assertThrows(MissingSchedulerQueueStateException.class, () ->
                store.resolve(
                        "00000000-0000-0000-0000-000000000399"));
    }

    @Test
    void rejectsOversizedAndNonRegularArtifacts() throws Exception {
        FileSystemSchedulerQueueStore store =
                new FileSystemSchedulerQueueStore(storageRoot);
        Files.createDirectories(storageRoot);
        Path artifact = artifact(QUEUE_ID);
        try (FileChannel channel = FileChannel.open(
                artifact,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            channel.position(
                    FileSystemSchedulerQueueStore.MAX_STATE_BYTES
                            + FileSystemSchedulerQueueStore.HEADER_BYTES);
            channel.write(ByteBuffer.wrap(new byte[] {1}));
        }
        assertThrows(CorruptedSchedulerQueueStateException.class, () ->
                store.resolve(QUEUE_ID));

        Files.delete(artifact);
        Files.createDirectory(artifact);
        assertThrows(CorruptedSchedulerQueueStateException.class, () ->
                store.resolve(QUEUE_ID));
    }

    @Test
    void roundTripsFailedDispositionAcrossStoreInstances() throws Exception {
        String firstId =
                "00000000-0000-0000-0000-000000000321";
        FileSystemSchedulerQueueStore store =
                new FileSystemSchedulerQueueStore(storageRoot);
        store.create(SchedulerQueueState.initial(QUEUE_ID, 8));

        SingleWorkerSchedulerQueue queue = new SingleWorkerSchedulerQueue(8);
        queue.enqueue(new QueuedWork(workItem(firstId, "reader"), List.of()));
        queue.claimNext().orElseThrow();
        queue.failActive(firstId);
        store.update(queue.snapshot(QUEUE_ID, 1));

        SchedulerQueueState resolved =
                new FileSystemSchedulerQueueStore(storageRoot).resolve(QUEUE_ID);
        assertEquals(Set.of(firstId), resolved.failedWorkItemIds());
        assertEquals(Set.of(), resolved.completedWorkItemIds());
        assertEquals(
                List.of(new QueuedWork(workItem(firstId, "reader"), List.of())),
                resolved.admittedWork());
    }

    @Test
    void rejectsRewrittenExactAdmissionHistoryAcrossUpdates() throws Exception {
        String firstId =
                "00000000-0000-0000-0000-000000000322";
        FileSystemSchedulerQueueStore store =
                new FileSystemSchedulerQueueStore(storageRoot);
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store);
        queue.enqueue(new QueuedWork(workItem(firstId, "reader"), List.of()));
        SchedulerQueueState current = store.resolve(QUEUE_ID);
        QueuedWork changed = new QueuedWork(
                workItem(firstId, "changed-reader"),
                List.of());
        SchedulerQueueState rewritten = new SchedulerQueueState(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                QUEUE_ID,
                current.revision() + 1,
                current.maxWorkItems(),
                current.logicalRunId(),
                current.admissionOrder(),
                List.of(changed),
                List.of(changed),
                Optional.empty(),
                Set.of(),
                Set.of());

        assertThrows(IOException.class, () -> store.update(rewritten));
        assertEquals(current.admittedWork(), store.resolve(QUEUE_ID).admittedWork());
    }

    @Test
    void recoversFailedDispositionFromTheFilesystem() throws Exception {
        String firstId =
                "00000000-0000-0000-0000-000000000331";
        String secondId =
                "00000000-0000-0000-0000-000000000332";
        FileSystemSchedulerQueueStore store =
                new FileSystemSchedulerQueueStore(storageRoot);
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store);
        queue.enqueue(new QueuedWork(workItem(firstId, "reader"), List.of()));
        queue.enqueue(new QueuedWork(
                workItem(secondId, "reviewer"), List.of(firstId)));
        queue.claimNext().orElseThrow();
        queue.failActive(firstId);

        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID,
                        new FileSystemSchedulerQueueStore(storageRoot));
        assertEquals(Set.of(firstId), recovered.failedWorkItemIds());
        assertTrue(recovered.claimNext().isEmpty());
    }

    @Test
    void roundTripsADeclaredExecutionInputAcrossStoreInstances()
            throws Exception {
        String workItemId = "00000000-0000-0000-0000-000000000361";
        WorkPayload.ExecutionInput input = new WorkPayload.ExecutionInput(
                "docs/target-🚀.md", "e".repeat(64));
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        8,
                        new FileSystemSchedulerQueueStore(storageRoot));
        queue.enqueue(new QueuedWork(
                workItemWithInput(workItemId, Optional.of(input)),
                List.of()));

        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID,
                        new FileSystemSchedulerQueueStore(storageRoot));
        WorkItem claimed = recovered.claimNext().orElseThrow();
        assertEquals(Optional.of(input), claimed.executionInput());
        assertEquals(
                workItem(workItemId, "worker").workMessage().messageId(),
                claimed.workMessage().messageId());
    }

    private static WorkItem workItemWithInput(
            String workItemId,
            Optional<WorkPayload.ExecutionInput> executionInput) {
        WorkItem base = workItem(workItemId, "worker");
        MessageEnvelope message = base.workMessage();
        WorkPayload payload = (WorkPayload) message.payload();
        return new WorkItem(
                workItemId,
                base.requiredCapability(),
                new MessageEnvelope(
                        message.messageId(),
                        message.correlationId(),
                        message.causationId(),
                        message.logicalRunId(),
                        message.producer(),
                        message.occurredAt(),
                        new WorkPayload(
                                payload.taskRevision(),
                                payload.snapshotId(),
                                payload.allowedTools(),
                                executionInput)));
    }

    private Path artifact(String queueId) {
        return storageRoot.resolve(queueId + ".scheduler-queue");
    }

    private Process startLockHolder() throws IOException {
        return new ProcessBuilder(
                IsolatedWorkerLauncher.javaExecutable().toString(),
                "-cp",
                System.getProperty("java.class.path"),
                SchedulerQueueLockHolderMain.class.getName(),
                storageRoot.toString(),
                QUEUE_ID)
                .redirectErrorStream(true)
                .start();
    }

    private static WorkItem workItem(
            String workItemId,
            String capability) {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                "gate-8-durable-scheduler-queue-state",
                "CURRENT_TASK.md",
                "a".repeat(64));
        long suffix = Long.parseLong(
                workItemId.substring(workItemId.length() - 12));
        MessageEnvelope envelope = new MessageEnvelope(
                String.format(
                        "00000000-0000-0000-0002-%012d",
                        suffix),
                "correlation-filesystem-1",
                Optional.of(
                        "00000000-0000-0000-0003-000000000001"),
                "logical-run-filesystem-1",
                "gate-8-filesystem-\uD83D\uDE80",
                Instant.parse("2026-07-16T16:30:00.123456789Z"),
                new WorkPayload(
                        revision,
                        "b".repeat(64),
                        Set.of("read-file", "verify")));
        return new WorkItem(workItemId, capability, envelope);
    }

    private static void replaceDigest(byte[] envelope) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        int magic = buffer.getInt(0);
        long storedAt = buffer.getLong(Integer.BYTES);
        int payloadLength = buffer.getInt(Integer.BYTES + Long.BYTES);
        byte[] payload = new byte[payloadLength];
        System.arraycopy(envelope, PAYLOAD_OFFSET, payload, 0, payloadLength);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                ByteBuffer.allocate(
                        Integer.BYTES
                                + Long.BYTES
                                + Integer.BYTES
                                + payload.length)
                        .putInt(magic)
                        .putLong(storedAt)
                        .putInt(payloadLength)
                        .put(payload)
                        .array());
        System.arraycopy(digest, 0, envelope, DIGEST_OFFSET, digest.length);
    }
}
