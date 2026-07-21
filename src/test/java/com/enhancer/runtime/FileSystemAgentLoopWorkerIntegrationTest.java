package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.FileSystemEvidenceStore;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Wires the real AgentLoop-backed execution port into the durable worker over real filesystem
 * stores: one cycle drives claim, real read-file Tool execution, real evidence, real independent
 * digest verification, a real persisted RunRecord, and the matching queue disposition.
 */
class FileSystemAgentLoopWorkerIntegrationTest {
    private static final String QUEUE_ID = "00000000-0000-0000-0000-000000003001";
    private static final String WORK_ID = "00000000-0000-0000-0000-000000003011";
    private static final String DEP_ID = "00000000-0000-0000-0000-000000003012";
    private static final String OWNER_ID = "00000000-0000-0000-0000-000000003021";
    private static final String TASK_ID = "gate-8-agentloop-execution-port";
    private static final String SOURCE_DOCUMENT = "CURRENT_TASK.md";
    private static final Duration LEASE = Duration.ofMinutes(5);
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-20T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private Path projectRoot;
    private String approvedDigest;

    @BeforeEach
    void setUp() throws Exception {
        projectRoot = Files.createDirectories(tempDir.resolve("project"));
        byte[] content = "Approved task document content.\n"
                .getBytes(StandardCharsets.UTF_8);
        Files.write(projectRoot.resolve(SOURCE_DOCUMENT), content);
        approvedDigest = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content));
    }

    @Test
    void verifiedExecutionCompletesTheClaimAndItsDependentEndToEnd()
            throws Exception {
        Stores s = stores();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, s.queueStore());
        queue.enqueue(new QueuedWork(workItem(WORK_ID, approvedDigest), List.of()));
        queue.enqueue(new QueuedWork(
                workItem(DEP_ID, approvedDigest), List.of(WORK_ID)));

        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                worker(s).runOneCycle(LEASE));
        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                worker(s).runOneCycle(LEASE));

        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore());
        assertEquals(Set.of(WORK_ID, DEP_ID), recovered.completedWorkItemIds());
        assertEquals(2, s.runRecordStore().references().size());
        assertTrue(s.checkpointStore().findPending().isEmpty());
    }

    @Test
    void executesADeclaredArbitraryTargetToVerifiedCompletion() throws Exception {
        byte[] target = "Arbitrary governed target file.\n"
                .getBytes(StandardCharsets.UTF_8);
        java.nio.file.Files.createDirectories(projectRoot.resolve("docs"));
        Files.write(projectRoot.resolve("docs/target.md"), target);
        String targetDigest = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(target));
        Stores s = stores();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, s.queueStore());
        queue.enqueue(new QueuedWork(
                workItemWithInput(WORK_ID, new WorkPayload.ExecutionInput(
                        "docs/target.md", targetDigest)),
                List.of()));

        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                worker(s).runOneCycle(LEASE));

        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore());
        assertEquals(Set.of(WORK_ID), recovered.completedWorkItemIds());
    }

    @Test
    void digestMismatchFailsTheClaimAndBlocksTheDependent() throws Exception {
        Stores s = stores();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, s.queueStore());
        queue.enqueue(new QueuedWork(
                workItem(WORK_ID, "a".repeat(64)), List.of()));
        queue.enqueue(new QueuedWork(
                workItem(DEP_ID, approvedDigest), List.of(WORK_ID)));

        assertEquals(Optional.of(WorkItemDisposition.FAILED),
                worker(s).runOneCycle(LEASE));
        assertTrue(worker(s).runOneCycle(LEASE).isEmpty());

        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore());
        assertEquals(Set.of(WORK_ID), recovered.failedWorkItemIds());
        assertTrue(recovered.completedWorkItemIds().isEmpty());
        assertTrue(s.checkpointStore().findPending().isEmpty());
    }

    @Test
    void productionProcessIsolatedCompositionCompletesAndRetiresItsSpool()
            throws Exception {
        Stores s = stores();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, s.queueStore());
        queue.enqueue(new QueuedWork(workItem(WORK_ID, approvedDigest), List.of()));
        Path invocationRoot = tempDir.resolve("invocations");

        DurableAgentRunWorker worker = DurableAgentRunWorker.processIsolated(
                queue,
                s.runtimeStore(),
                s.checkpointStore(),
                projectRoot,
                tempDir.resolve("evidence"),
                tempDir.resolve("records"),
                invocationRoot,
                s.runRecordStore(),
                OWNER_ID,
                CLOCK,
                Duration.ofSeconds(20));

        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                worker.runOneCycle(LEASE));
        assertEquals(Set.of(WORK_ID),
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore())
                        .completedWorkItemIds());
        assertEquals(1, s.runRecordStore().references().size());
        assertTrue(s.checkpointStore().findPending().isEmpty());
        assertFalse(hasEntries(invocationRoot),
                "checkpointed work/result spools must be retired after the cycle");
    }

    // ---- shared helpers ----

    private DurableAgentRunWorker worker(Stores s) throws IOException {
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore());
        return new DurableAgentRunWorker(
                new DurableAgentRunDispatcher(queue, s.runtimeStore(), CLOCK),
                new AgentLoopAgentRunExecution(
                        projectRoot,
                        s.evidenceStore(),
                        s.runRecordStore(),
                        CLOCK),
                s.checkpointStore(),
                new DurableAgentRunFinalizer(
                        queue, s.runtimeStore(), s.runRecordStore(), CLOCK),
                s.runtimeStore(),
                OWNER_ID,
                CLOCK);
    }

    private Stores stores() {
        return new Stores(
                new FileSystemSchedulerQueueStore(tempDir.resolve("queue")),
                new FileSystemAgentRuntimeStateStore(tempDir.resolve("runtime")),
                new FileSystemRunRecordStore(tempDir.resolve("records")),
                new FileSystemPendingFinalizationStore(tempDir.resolve("checkpoint")),
                new FileSystemEvidenceStore(
                        tempDir.resolve("evidence"),
                        new EvidenceStoragePolicy(
                                EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES)));
    }

    private static boolean hasEntries(Path root) throws IOException {
        if (!Files.exists(root)) {
            return false;
        }
        try (var entries = Files.list(root)) {
            return entries.findAny().isPresent();
        }
    }

    private WorkItem workItem(String workItemId, String sourceSha256) {
        return workItem(workItemId, sourceSha256, Optional.empty());
    }

    private WorkItem workItemWithInput(
            String workItemId, WorkPayload.ExecutionInput executionInput) {
        return workItem(workItemId, approvedDigest, Optional.of(executionInput));
    }

    private WorkItem workItem(
            String workItemId,
            String sourceSha256,
            Optional<WorkPayload.ExecutionInput> executionInput) {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                TASK_ID, SOURCE_DOCUMENT, sourceSha256);
        MessageEnvelope envelope = new MessageEnvelope(
                messageUuid(workItemId),
                "correlation-agentloop-worker-1",
                Optional.empty(),
                "logical-run-agentloop-worker-1",
                "agentloop-worker-integration-test",
                Instant.parse("2026-07-20T10:00:00Z"),
                new WorkPayload(
                        revision, "d".repeat(64), Set.of("read-file"), executionInput));
        return new WorkItem(workItemId, "read-file-worker", envelope);
    }

    private static String messageUuid(String workItemId) {
        long suffix = Long.parseLong(workItemId.substring(workItemId.length() - 12));
        return String.format("00000000-0000-0000-0004-%012d", suffix);
    }

    private record Stores(
            FileSystemSchedulerQueueStore queueStore,
            FileSystemAgentRuntimeStateStore runtimeStore,
            FileSystemRunRecordStore runRecordStore,
            FileSystemPendingFinalizationStore checkpointStore,
            FileSystemEvidenceStore evidenceStore) {
    }
}
