package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
        recovered.completeActive(firstId);
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
        byte[] unsupported = Files.readAllBytes(artifact);
        ByteBuffer.wrap(unsupported).putInt(
                PAYLOAD_OFFSET,
                SchedulerQueueState.CURRENT_SCHEMA_VERSION + 1);
        replaceDigest(unsupported);
        Files.write(artifact, unsupported);
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

    private Path artifact(String queueId) {
        return storageRoot.resolve(queueId + ".scheduler-queue");
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
