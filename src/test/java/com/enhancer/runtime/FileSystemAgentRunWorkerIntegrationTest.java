package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationCode;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.PolicyDecision;
import com.enhancer.run.PolicyDecisionStatus;
import com.enhancer.run.RunRecord;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemAgentRunWorkerIntegrationTest {
    private static final String QUEUE_ID = "00000000-0000-0000-0000-000000001001";
    private static final String GOAL_ID = "00000000-0000-0000-0000-000000001002";
    private static final String AGENT_RUN_ID = "00000000-0000-0000-0000-000000001003";
    private static final String WORK_ID = "00000000-0000-0000-0000-000000001011";
    private static final String DEP_ID = "00000000-0000-0000-0000-000000001012";
    private static final String OWNER_ID = "00000000-0000-0000-0000-000000001021";
    private static final String TASK_ID = "gate-8-in-process-scheduler-worker";
    private static final Duration LEASE = Duration.ofMinutes(5);
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    @Test
    void interruptedCycleResumesOnAFreshWorkerAndDrivesTheDependentEndToEnd()
            throws Exception {
        Stores s = stores();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, s.queueStore());
        queue.enqueue(new QueuedWork(workItem(WORK_ID), List.of()));
        queue.enqueue(new QueuedWork(workItem(DEP_ID), List.of(WORK_ID)));
        s.checkpointStore().record(
                new PendingFinalization(GOAL_ID, AGENT_RUN_ID, Optional.empty()));
        new DurableAgentRunDispatcher(queue, s.runtimeStore(), CLOCK)
                .claimAndLease(GOAL_ID, AGENT_RUN_ID, OWNER_ID, LEASE)
                .orElseThrow();
        // Crash while EXECUTING; everything below runs on fresh instances over
        // the same storage roots.

        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                worker(s, executionPersisting(s, true)).runOneCycle(LEASE));
        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                worker(s, executionPersisting(s, true)).runOneCycle(LEASE));

        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore());
        assertEquals(Set.of(WORK_ID, DEP_ID), recovered.completedWorkItemIds());
        assertEquals(RuntimeAgentRunStatus.COMPLETED,
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore(), CLOCK)
                        .agentRun().orElseThrow().status());
        assertTrue(s.checkpointStore().findPending().isEmpty());
    }

    @Test
    void failedOutcomeBlocksTheDependentWorkItem() throws Exception {
        Stores s = stores();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, s.queueStore());
        queue.enqueue(new QueuedWork(workItem(WORK_ID), List.of()));
        queue.enqueue(new QueuedWork(workItem(DEP_ID), List.of(WORK_ID)));

        assertEquals(Optional.of(WorkItemDisposition.FAILED),
                worker(s, executionPersisting(s, false)).runOneCycle(LEASE));
        assertTrue(worker(s, forbiddenExecution()).runOneCycle(LEASE).isEmpty());

        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore());
        assertEquals(Set.of(WORK_ID), recovered.failedWorkItemIds());
        assertTrue(recovered.completedWorkItemIds().isEmpty());
        assertTrue(s.checkpointStore().findPending().isEmpty());
    }

    // ---- shared helpers ----

    private DurableAgentRunWorker worker(Stores s, AgentRunExecution execution)
            throws IOException {
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore());
        return new DurableAgentRunWorker(
                new DurableAgentRunDispatcher(queue, s.runtimeStore(), CLOCK),
                execution,
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
                new FileSystemPendingFinalizationStore(tempDir.resolve("checkpoint")));
    }

    private AgentRunExecution executionPersisting(Stores s, boolean verified) {
        return dispatch -> s.runRecordStore().persist(runRecord(verified)).reference();
    }

    private AgentRunExecution forbiddenExecution() {
        return dispatch -> {
            throw new IllegalStateException("execution must not run on this path");
        };
    }

    private RunRecord runRecord(boolean verified) {
        return new RunRecord(
                "logical-run-worker-integration-1",
                Instant.parse("2026-07-17T11:00:00Z"),
                new ApprovedTask(
                        TASK_ID,
                        "Drive one scheduling cycle",
                        "Approved by test owner",
                        Set.of("read-file"),
                        "CURRENT_TASK.md"),
                new ToolRequest("read-file", "correlation-1", Map.of("path", "target.txt")),
                new PolicyDecision(
                        PolicyDecisionStatus.ALLOWED,
                        "C:/project",
                        Set.of("read-file"),
                        Set.of(),
                        4096,
                        1000),
                success(),
                Optional.of("a".repeat(64)),
                verified
                        ? VerificationDecision.verified("content matched")
                        : VerificationDecision.rejected(
                                VerificationCode.CONTENT_MISMATCH, "content differed"),
                1,
                AgentLoopStopReason.AWAITING_VERIFICATION,
                verified
                        ? AgentLoopStopReason.COMPLETED
                        : AgentLoopStopReason.AWAITING_VERIFICATION);
    }

    private ToolResult success() {
        return new ToolResult(
                "read-file",
                ToolResultStatus.SUCCESS,
                OptionalInt.empty(),
                VerificationEvidence.capture(
                        "read succeeded", "content", Optional.empty()));
    }

    private static WorkItem workItem(String workItemId) {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                TASK_ID, "CURRENT_TASK.md", "a".repeat(64));
        MessageEnvelope envelope = new MessageEnvelope(
                incrementUuid(workItemId),
                "correlation-worker-integration-1",
                Optional.empty(),
                "logical-run-worker-integration-1",
                "worker-integration-test",
                Instant.parse("2026-07-17T10:00:00Z"),
                new WorkPayload(revision, "b".repeat(64), Set.of("read-file")));
        return new WorkItem(workItemId, "read-file-worker", envelope);
    }

    private static String incrementUuid(String workItemId) {
        long suffix = Long.parseLong(workItemId.substring(workItemId.length() - 12));
        return String.format("00000000-0000-0000-0002-%012d", suffix);
    }

    private record Stores(
            FileSystemSchedulerQueueStore queueStore,
            FileSystemAgentRuntimeStateStore runtimeStore,
            FileSystemRunRecordStore runRecordStore,
            FileSystemPendingFinalizationStore checkpointStore) {
    }
}
