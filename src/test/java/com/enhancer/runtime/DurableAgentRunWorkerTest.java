package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ResultPayload;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationCode;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.kernel.VerificationStatus;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DurableAgentRunWorkerTest {
    private static final String QUEUE_ID = "00000000-0000-0000-0000-000000000901";
    private static final String GOAL_ID = "00000000-0000-0000-0000-000000000902";
    private static final String AGENT_RUN_ID = "00000000-0000-0000-0000-000000000903";
    private static final String WORK_ID = "00000000-0000-0000-0000-000000000911";
    private static final String DEP_ID = "00000000-0000-0000-0000-000000000912";
    private static final String OWNER_ID = "00000000-0000-0000-0000-000000000921";
    private static final String TASK_ID = "gate-8-in-process-scheduler-worker";
    private static final Duration LEASE = Duration.ofMinutes(5);
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    @Test
    void verifiedCycleCompletesRuntimeAndQueueAndClearsIntent() throws Exception {
        Stores s = stores();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, s.queueStore());
        queue.enqueue(new QueuedWork(workItem(WORK_ID), List.of()));
        queue.enqueue(new QueuedWork(workItem(DEP_ID), List.of(WORK_ID)));

        Optional<WorkItemDisposition> disposition =
                worker(s, executionPersisting(s, true)).runOneCycle(LEASE);

        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED), disposition);
        List<String> goalIds = goalIds(s);
        assertEquals(1, goalIds.size());
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(goalIds.get(0), s.runtimeStore(), CLOCK);
        assertEquals(RuntimeAgentRunStatus.COMPLETED,
                runtime.agentRun().orElseThrow().status());
        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore());
        assertEquals(Set.of(WORK_ID), recovered.completedWorkItemIds());
        assertEquals(DEP_ID, recovered.claimNext().orElseThrow().workItemId());
        assertTrue(s.checkpointStore().findPending().isEmpty());
    }

    @Test
    void failedCycleParksAtRetryPendingAndRetainsIntent() throws Exception {
        Stores s = stores();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, s.queueStore());
        queue.enqueue(new QueuedWork(workItem(WORK_ID), List.of()));
        queue.enqueue(new QueuedWork(workItem(DEP_ID), List.of(WORK_ID)));

        Optional<WorkItemDisposition> disposition =
                worker(s, executionPersisting(s, false)).runOneCycle(LEASE);

        assertTrue(disposition.isEmpty());
        List<String> goalIds = goalIds(s);
        assertEquals(1, goalIds.size());
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(goalIds.get(0), s.runtimeStore(), CLOCK);
        assertEquals(RuntimeGoalStatus.RETRY_PENDING, runtime.goal().status());
        assertEquals(RuntimeAgentRunStatus.FAILED,
                runtime.agentRun().orElseThrow().status());
        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore());
        assertTrue(recovered.failedWorkItemIds().isEmpty());
        assertTrue(recovered.completedWorkItemIds().isEmpty());
        assertEquals(WORK_ID, recovered.claimNext().orElseThrow().workItemId());
        PendingFinalization pending = s.checkpointStore().findPending().orElseThrow();
        assertEquals(goalIds.get(0), pending.goalId());
        assertTrue(pending.runRecordReference().isPresent());
    }

    @Test
    void emptyQueueReturnsEmptyAndLeavesNoDurableTrace() throws Exception {
        Stores s = stores();
        DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, s.queueStore());

        Optional<WorkItemDisposition> disposition =
                worker(s, forbiddenExecution()).runOneCycle(LEASE);

        assertTrue(disposition.isEmpty());
        assertTrue(s.checkpointStore().findPending().isEmpty());
        assertEquals(List.of(), goalIds(s));
    }

    @Test
    void resumesInterruptedCycleAfterExecutionAcknowledgement() throws Exception {
        Stores s = stores();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, s.queueStore());
        queue.enqueue(new QueuedWork(workItem(WORK_ID), List.of()));
        queue.enqueue(new QueuedWork(workItem(DEP_ID), List.of(WORK_ID)));
        s.checkpointStore().record(
                new PendingFinalization(GOAL_ID, AGENT_RUN_ID, Optional.empty()));
        AgentRunDispatch dispatch =
                new DurableAgentRunDispatcher(queue, s.runtimeStore(), CLOCK)
                        .claimAndLease(GOAL_ID, AGENT_RUN_ID, OWNER_ID, LEASE)
                        .orElseThrow();
        String reference = s.runRecordStore().persist(runRecord(true)).reference();
        s.checkpointStore().record(new PendingFinalization(
                GOAL_ID, AGENT_RUN_ID, Optional.of(reference)));
        DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore(), CLOCK)
                .completeExecution(AGENT_RUN_ID, OWNER_ID, dispatch.lease().fenceToken());
        // Crash before finalize: a fresh worker must finish WITHOUT re-executing.

        Optional<WorkItemDisposition> disposition =
                worker(s, forbiddenExecution()).runOneCycle(LEASE);

        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED), disposition);
        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore());
        assertEquals(Set.of(WORK_ID), recovered.completedWorkItemIds());
        assertEquals(DEP_ID, recovered.claimNext().orElseThrow().workItemId());
        assertTrue(s.checkpointStore().findPending().isEmpty());
    }

    @Test
    void recoversDispositionAfterRuntimeTerminalBeforeQueueDisposition() throws Exception {
        Stores s = stores();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, s.queueStore());
        WorkItem work = workItem(WORK_ID);
        queue.enqueue(new QueuedWork(work, List.of()));
        queue.enqueue(new QueuedWork(workItem(DEP_ID), List.of(WORK_ID)));
        s.checkpointStore().record(
                new PendingFinalization(GOAL_ID, AGENT_RUN_ID, Optional.empty()));
        AgentRunDispatch dispatch =
                new DurableAgentRunDispatcher(queue, s.runtimeStore(), CLOCK)
                        .claimAndLease(GOAL_ID, AGENT_RUN_ID, OWNER_ID, LEASE)
                        .orElseThrow();
        String reference = s.runRecordStore().persist(runRecord(true)).reference();
        s.checkpointStore().record(new PendingFinalization(
                GOAL_ID, AGENT_RUN_ID, Optional.of(reference)));
        DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore(), CLOCK)
                .completeExecution(AGENT_RUN_ID, OWNER_ID, dispatch.lease().fenceToken());
        DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore(), CLOCK)
                .recordResult(AGENT_RUN_ID, terminalResultEnvelope(
                        work, reference, VerificationStatus.VERIFIED));
        // Crash after the runtime terminal transition, before the queue disposition.

        Optional<WorkItemDisposition> disposition =
                worker(s, forbiddenExecution()).runOneCycle(LEASE);

        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED), disposition);
        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore());
        assertEquals(Set.of(WORK_ID), recovered.completedWorkItemIds());
        assertEquals(DEP_ID, recovered.claimNext().orElseThrow().workItemId());
        assertTrue(s.checkpointStore().findPending().isEmpty());
    }

    @Test
    void reDrivesWithSameIdentitiesAfterCrashWhileExecuting() throws Exception {
        Stores s = stores();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, s.queueStore());
        queue.enqueue(new QueuedWork(workItem(WORK_ID), List.of()));
        s.checkpointStore().record(
                new PendingFinalization(GOAL_ID, AGENT_RUN_ID, Optional.empty()));
        new DurableAgentRunDispatcher(queue, s.runtimeStore(), CLOCK)
                .claimAndLease(GOAL_ID, AGENT_RUN_ID, OWNER_ID, LEASE)
                .orElseThrow();
        // Crash while EXECUTING, before any RunRecord: re-execution is required.

        Optional<WorkItemDisposition> disposition =
                worker(s, executionPersisting(s, true)).runOneCycle(LEASE);

        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED), disposition);
        assertEquals(List.of(GOAL_ID), goalIds(s));
        assertTrue(s.checkpointStore().findPending().isEmpty());
    }

    @Test
    void reDrivesWhenRuntimeStateWasNeverCreated() throws Exception {
        Stores s = stores();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, s.queueStore());
        queue.enqueue(new QueuedWork(workItem(WORK_ID), List.of()));
        s.checkpointStore().record(
                new PendingFinalization(GOAL_ID, AGENT_RUN_ID, Optional.empty()));
        // Crash after the intent, before the dispatcher created any runtime state.

        Optional<WorkItemDisposition> disposition =
                worker(s, executionPersisting(s, true)).runOneCycle(LEASE);

        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED), disposition);
        assertEquals(List.of(GOAL_ID), goalIds(s));
        assertTrue(s.checkpointStore().findPending().isEmpty());
    }

    @Test
    void emptyQueueIntentFromACrashConvergesToNoResidue() throws Exception {
        Stores s = stores();
        DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, s.queueStore());
        s.checkpointStore().record(
                new PendingFinalization(GOAL_ID, AGENT_RUN_ID, Optional.empty()));
        // Crash after the intent while the queue was empty.

        Optional<WorkItemDisposition> disposition =
                worker(s, forbiddenExecution()).runOneCycle(LEASE);

        assertTrue(disposition.isEmpty());
        assertTrue(s.checkpointStore().findPending().isEmpty());
        assertEquals(List.of(), goalIds(s));
    }

    @Test
    void executionFailureLeavesTheCycleRecoverable() throws Exception {
        Stores s = stores();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, s.queueStore());
        queue.enqueue(new QueuedWork(workItem(WORK_ID), List.of()));
        DurableAgentRunWorker failing = worker(s, dispatch -> {
            throw new IOException("tool execution failed");
        });

        assertThrows(IOException.class, () -> failing.runOneCycle(LEASE));

        PendingFinalization pending =
                s.checkpointStore().findPending().orElseThrow();
        assertTrue(pending.runRecordReference().isEmpty());
        DurableAgentRuntime runtime = DurableAgentRuntime.recover(
                pending.goalId(), s.runtimeStore(), CLOCK);
        assertEquals(RuntimeAgentRunStatus.EXECUTING,
                runtime.agentRun().orElseThrow().status());
        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore());
        assertTrue(recovered.completedWorkItemIds().isEmpty());
        assertTrue(recovered.failedWorkItemIds().isEmpty());

        // A fresh worker over the same stores finishes the interrupted cycle.
        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                worker(s, executionPersisting(s, true)).runOneCycle(LEASE));
        assertTrue(s.checkpointStore().findPending().isEmpty());
    }

    @Test
    void retriesSpoolCleanupAfterCheckpointWithoutExecutingAgain() throws Exception {
        Stores s = stores();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, s.queueStore());
        queue.enqueue(new QueuedWork(workItem(WORK_ID), List.of()));
        AtomicInteger executions = new AtomicInteger();
        AtomicInteger cleanups = new AtomicInteger();
        AgentRunExecution execution = new AgentRunExecution() {
            @Override
            public String execute(AgentRunDispatch dispatch) throws IOException {
                executions.incrementAndGet();
                return s.runRecordStore().persist(runRecord(true)).reference();
            }

            @Override
            public void cleanupAfterCheckpoint(AgentRunDispatch dispatch) throws IOException {
                if (cleanups.incrementAndGet() == 1) {
                    throw new IOException("spool cleanup failed");
                }
            }
        };

        IOException failedCleanup = assertThrows(
                IOException.class,
                () -> worker(s, execution).runOneCycle(LEASE));

        assertTrue(failedCleanup.getMessage().contains("spool cleanup failed"));
        assertTrue(s.checkpointStore().findPending().orElseThrow()
                .runRecordReference().isPresent());
        assertEquals(1, executions.get());
        assertEquals(1, cleanups.get());

        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                worker(s, execution).runOneCycle(LEASE));
        assertEquals(1, executions.get(),
                "a checkpointed reference must suppress re-execution");
        assertEquals(2, cleanups.get());
        assertTrue(s.checkpointStore().findPending().isEmpty());
    }

    private MessageEnvelope terminalResultEnvelope(
            WorkItem work, String reference, VerificationStatus status) {
        MessageEnvelope workMessage = work.workMessage();
        return new MessageEnvelope(
                UUID.nameUUIDFromBytes(
                        ("agent-run-result:" + AGENT_RUN_ID)
                                .getBytes(StandardCharsets.UTF_8)).toString(),
                workMessage.correlationId(),
                Optional.of(workMessage.messageId()),
                workMessage.logicalRunId(),
                "agent-run-finalizer",
                Instant.parse("2026-07-17T12:00:00Z"),
                new ResultPayload(TASK_ID, reference, status));
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
                new FileSystemPendingFinalizationStore(tempDir.resolve("checkpoint")),
                tempDir.resolve("runtime"));
    }

    private AgentRunExecution executionPersisting(Stores s, boolean verified) {
        return dispatch -> s.runRecordStore().persist(runRecord(verified)).reference();
    }

    private AgentRunExecution forbiddenExecution() {
        return dispatch -> {
            throw new IllegalStateException("execution must not run on this path");
        };
    }

    private List<String> goalIds(Stores s) throws IOException {
        if (!Files.exists(s.runtimeRoot())) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(s.runtimeRoot())) {
            return files
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".agent-runtime"))
                    .map(name -> name.substring(
                            0, name.length() - ".agent-runtime".length()))
                    .toList();
        }
    }

    private RunRecord runRecord(boolean verified) {
        return new RunRecord(
                "logical-run-worker-1",
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
                "correlation-worker-1",
                Optional.empty(),
                "logical-run-worker-1",
                "worker-test",
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
            FileSystemPendingFinalizationStore checkpointStore,
            Path runtimeRoot) {
    }
}
