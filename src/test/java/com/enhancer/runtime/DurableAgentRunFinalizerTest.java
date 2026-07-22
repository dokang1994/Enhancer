package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.kernel.VerificationCode;
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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

class DurableAgentRunFinalizerTest {
    private static final String QUEUE_ID = "00000000-0000-0000-0000-000000000401";
    private static final String GOAL_ID = "00000000-0000-0000-0000-000000000402";
    private static final String AGENT_RUN_ID = "00000000-0000-0000-0000-000000000403";
    private static final String WORK_ID = "00000000-0000-0000-0000-000000000411";
    private static final String DEP_ID = "00000000-0000-0000-0000-000000000412";
    private static final String OWNER_ID = "00000000-0000-0000-0000-000000000421";
    private static final String TASK_ID = "gate-8-result-path-finalization";
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    @Test
    void verifiedOutcomeCompletesRuntimeAndReleasesDependent() throws Exception {
        Setup s = awaitingVerification(true);
        String reference = persistRunRecord(s, true);

        DurableAgentRunFinalizer finalizer = finalizer(s);
        WorkItemDisposition disposition =
                finalizer.finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, reference);

        assertEquals(WorkItemDisposition.VERIFIED_COMPLETED, disposition);
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        assertEquals(RuntimeAgentRunStatus.COMPLETED,
                runtime.agentRun().orElseThrow().status());
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore);
        assertEquals(Set.of(WORK_ID), queue.completedWorkItemIds());
        assertEquals(DEP_ID, queue.claimNext().orElseThrow().workItemId());
    }

    @Test
    void failedAttemptStopsAtRetryPendingWithoutQueueDisposition() throws Exception {
        Setup s = awaitingVerification(true);
        String reference = persistRunRecord(s, false);

        DurableAgentRunFinalizer finalizer = finalizer(s);
        RuntimeGoalStatus status =
                finalizer.recordAgentRunResult(GOAL_ID, AGENT_RUN_ID, reference);

        assertEquals(RuntimeGoalStatus.RETRY_PENDING, status);
        assertTrue(finalizer.finalizeTerminalDisposition(GOAL_ID).isEmpty());
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        assertEquals(RuntimeGoalStatus.RETRY_PENDING, runtime.goal().status());
        assertEquals(RuntimeAgentRunStatus.FAILED,
                runtime.agentRun().orElseThrow().status());
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore);
        assertTrue(queue.failedWorkItemIds().isEmpty());
        assertTrue(queue.completedWorkItemIds().isEmpty());
        assertEquals(WORK_ID, queue.claimNext().orElseThrow().workItemId());
    }

    @Test
    void refusedRetryDecisionPermitsOneTerminalFailedDisposition() throws Exception {
        Setup s = awaitingVerification(false);
        String reference = persistRunRecord(s, false);
        DurableAgentRunFinalizer finalizer = finalizer(s);
        assertEquals(
                RuntimeGoalStatus.RETRY_PENDING,
                finalizer.recordAgentRunResult(
                        GOAL_ID, AGENT_RUN_ID, reference));

        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        runtime.recordRetryDecision(new AgentRunRetryDecisionRecord(
                AGENT_RUN_ID,
                1,
                1,
                0,
                0,
                "c".repeat(64),
                AgentRunRetryDecision.refused(
                        AgentRunRetryRefusalReason.ATTEMPTS_EXHAUSTED)));
        runtime.abandonGoal();

        assertEquals(
                Optional.of(WorkItemDisposition.FAILED),
                finalizer.finalizeTerminalDisposition(GOAL_ID));
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore);
        assertEquals(Set.of(WORK_ID), queue.failedWorkItemIds());
    }

    @Test
    void recoverFinalizationAppliesDispositionFromTerminalRuntimeWithoutReference()
            throws Exception {
        Setup s = awaitingVerification(true);
        String reference = persistRunRecord(s, true);
        // Simulate a crash after the runtime terminal transition, before the queue disposition.
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        runtime.recordResult(
                AGENT_RUN_ID,
                terminalResultEnvelope(s.work, reference, VerificationStatus.VERIFIED));

        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore);
        DurableAgentRunFinalizer finalizer = new DurableAgentRunFinalizer(
                queue, s.runtimeStore, s.runRecordStore, CLOCK);

        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                finalizer.recoverFinalization(GOAL_ID));
        assertEquals(DEP_ID, queue.claimNext().orElseThrow().workItemId());
    }

    @Test
    void reFinalizeAfterTerminalIsIdempotent() throws Exception {
        Setup s = awaitingVerification(false);
        String reference = persistRunRecord(s, true);
        finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, reference);

        // A second finalize with the same reference must not throw and must not change state.
        WorkItemDisposition disposition =
                finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, reference);
        assertEquals(WorkItemDisposition.VERIFIED_COMPLETED, disposition);
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore);
        assertEquals(Set.of(WORK_ID), queue.completedWorkItemIds());

        // recoverFinalization on a fully finalized run is also a no-op disposition report.
        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                finalizer(s).recoverFinalization(GOAL_ID));
    }

    @Test
    void missingRunRecordFailsClosedAndLeavesRunRecoverable() throws Exception {
        Setup s = awaitingVerification(false);
        String missing = "run-record/00000000-0000-0000-0000-0000000009ff";

        assertThrows(com.enhancer.run.MissingRunRecordException.class, () ->
                finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, missing));

        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        assertEquals(RuntimeAgentRunStatus.AWAITING_VERIFICATION,
                runtime.agentRun().orElseThrow().status());
        // Fail-closed: no disposition recorded and the work stays recoverable (the durable queue's
        // recovery contract requeues in-flight work to pending, so it is claimable again).
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore);
        assertTrue(queue.completedWorkItemIds().isEmpty());
        assertTrue(queue.failedWorkItemIds().isEmpty());
        assertEquals(WORK_ID, queue.claimNext().orElseThrow().workItemId());
    }

    @Test
    void runRecordForADifferentTaskIsRejected() throws Exception {
        Setup s = awaitingVerification(false);
        String reference = s.runRecordStore.persist(new RunRecord(
                "logical-run-finalizer-1",
                Instant.parse("2026-07-17T11:00:00Z"),
                new ApprovedTask(
                        "a-different-task",
                        "Different task",
                        "Approved by test owner",
                        Set.of("read-file"),
                        "CURRENT_TASK.md"),
                new ToolRequest("read-file", "correlation-1", Map.of("path", "t.txt")),
                new PolicyDecision(PolicyDecisionStatus.ALLOWED, "C:/project",
                        Set.of("read-file"), Set.of(), 4096, 1000),
                success(),
                Optional.of("a".repeat(64)),
                VerificationDecision.verified("content matched"),
                1,
                AgentLoopStopReason.AWAITING_VERIFICATION,
                AgentLoopStopReason.COMPLETED)).reference();

        assertThrows(IllegalArgumentException.class, () ->
                finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, reference));
    }

    @Test
    void reFinalizeWithDifferentReferenceIsRejected() throws Exception {
        Setup s = awaitingVerification(false);
        String first = persistRunRecord(s, true);
        String second = persistRunRecord(s, true);
        finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, first);

        assertThrows(IllegalStateException.class, () ->
                finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, second));
    }

    @Test
    void finalizeBeforeExecutionAcknowledgementIsRejected() throws Exception {
        // Reach EXECUTING (leased) but do NOT completeExecution.
        FileSystemSchedulerQueueStore queueStore =
                new FileSystemSchedulerQueueStore(tempDir.resolve("queue"));
        FileSystemAgentRuntimeStateStore runtimeStore =
                new FileSystemAgentRuntimeStateStore(tempDir.resolve("runtime"));
        FileSystemRunRecordStore runRecordStore =
                new FileSystemRunRecordStore(tempDir.resolve("records"));
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, queueStore);
        queue.enqueue(new QueuedWork(workItem(WORK_ID), List.of()));
        new DurableAgentRunDispatcher(queue, runtimeStore, CLOCK)
                .claimAndLease(GOAL_ID, AGENT_RUN_ID, OWNER_ID, Duration.ofMinutes(5))
                .orElseThrow();
        String reference =
                runRecordStore.persist(runRecord(true)).reference();

        DurableAgentRunFinalizer finalizer = new DurableAgentRunFinalizer(
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, queueStore),
                runtimeStore, runRecordStore, CLOCK);
        assertThrows(IllegalStateException.class, () ->
                finalizer.finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, reference));
    }

    private MessageEnvelope terminalResultEnvelope(
            WorkItem work, String reference, VerificationStatus status) {
        MessageEnvelope workMessage = work.workMessage();
        return new MessageEnvelope(
                java.util.UUID.nameUUIDFromBytes(
                        ("agent-run-result:" + AGENT_RUN_ID)
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(),
                workMessage.correlationId(),
                Optional.of(workMessage.messageId()),
                workMessage.logicalRunId(),
                "agent-run-finalizer",
                Instant.parse("2026-07-17T12:00:00Z"),
                new com.enhancer.bus.ResultPayload(TASK_ID, reference, status));
    }

    // ---- shared helpers ----

    private DurableAgentRunFinalizer finalizer(Setup s) throws IOException {
        return new DurableAgentRunFinalizer(
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore),
                s.runtimeStore,
                s.runRecordStore,
                CLOCK);
    }

    private Setup awaitingVerification(boolean withDependent) throws IOException {
        FileSystemSchedulerQueueStore queueStore =
                new FileSystemSchedulerQueueStore(tempDir.resolve("queue"));
        FileSystemAgentRuntimeStateStore runtimeStore =
                new FileSystemAgentRuntimeStateStore(tempDir.resolve("runtime"));
        FileSystemRunRecordStore runRecordStore =
                new FileSystemRunRecordStore(tempDir.resolve("records"));

        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, queueStore);
        WorkItem work = workItem(WORK_ID);
        queue.enqueue(new QueuedWork(work, List.of()));
        if (withDependent) {
            queue.enqueue(new QueuedWork(workItem(DEP_ID), List.of(WORK_ID)));
        }

        DurableAgentRunDispatcher dispatcher =
                new DurableAgentRunDispatcher(queue, runtimeStore, CLOCK);
        AgentRunDispatch dispatch = dispatcher.claimAndLease(
                GOAL_ID, AGENT_RUN_ID, OWNER_ID, Duration.ofMinutes(5)).orElseThrow();

        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, runtimeStore, CLOCK);
        runtime.completeExecution(
                AGENT_RUN_ID, OWNER_ID, dispatch.lease().fenceToken());

        return new Setup(queueStore, runtimeStore, runRecordStore, work);
    }

    private String persistRunRecord(Setup s, boolean verified) throws IOException {
        return s.runRecordStore.persist(runRecord(verified)).reference();
    }

    private RunRecord runRecord(boolean verified) {
        return new RunRecord(
                "logical-run-finalizer-1",
                Instant.parse("2026-07-17T11:00:00Z"),
                new ApprovedTask(
                        TASK_ID,
                        "Finalize the Gate 8 result path",
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
                verified ? success() : success(),
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
                VerificationEvidence.capture("read succeeded", "content", Optional.empty()));
    }

    private static WorkItem workItem(String workItemId) {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                TASK_ID, "CURRENT_TASK.md", "a".repeat(64));
        MessageEnvelope envelope = new MessageEnvelope(
                incrementUuid(workItemId),
                "correlation-finalizer-1",
                Optional.empty(),
                "logical-run-finalizer-1",
                "finalizer-test",
                Instant.parse("2026-07-17T10:00:00Z"),
                new WorkPayload(revision, "b".repeat(64), Set.of("read-file")));
        return new WorkItem(workItemId, "read-file-worker", envelope);
    }

    private static String incrementUuid(String workItemId) {
        long suffix = Long.parseLong(workItemId.substring(workItemId.length() - 12));
        return String.format("00000000-0000-0000-0002-%012d", suffix);
    }

    private record Setup(
            FileSystemSchedulerQueueStore queueStore,
            FileSystemAgentRuntimeStateStore runtimeStore,
            FileSystemRunRecordStore runRecordStore,
            WorkItem work) {
    }
}
