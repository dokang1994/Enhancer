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
import com.enhancer.run.PolicyDecision;
import com.enhancer.run.PolicyDecisionStatus;
import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.run.RunRecord;
import com.enhancer.run.StoredRunRecord;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SchedulerRecoveryStatusTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000951";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000952";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000953";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000954";
    private static final String REPLACEMENT_AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000956";
    private static final String RECORD_ID =
            "00000000-0000-0000-0000-000000000957";
    private static final String REFERENCE = "run-record/" + RECORD_ID;
    private static final Instant NOW =
            Instant.parse("2026-07-23T06:00:00Z");

    @Test
    void reportsNoPendingCycleWithoutInventingRuntimeCorrelation() {
        SchedulerRecoveryStatus status = SchedulerRecoveryStatus.project(
                SchedulerQueueState.initial(QUEUE_ID, 4),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        assertEquals(
                SchedulerRecoveryStatus.RecoveryPhase.NO_PENDING_CYCLE,
                status.phase());
        assertEquals(QUEUE_ID, status.queueId());
        assertEquals(0, status.queueRevision());
        assertTrue(status.goalId().isEmpty());
        assertTrue(status.queueWorkState().isEmpty());
        assertTrue(status.runRecordReference().isEmpty());
    }

    @Test
    void distinguishesIntentOnlyFromTheFirstRuntimePrefix() {
        WorkItem workItem = workItem();
        SchedulerQueueState queue = queueState(workItem);
        PendingFinalization pending = new PendingFinalization(
                GOAL_ID, AGENT_RUN_ID, Optional.empty());

        SchedulerRecoveryStatus intent = SchedulerRecoveryStatus.project(
                queue,
                Optional.of(pending),
                Optional.empty(),
                Optional.empty());
        SchedulerRecoveryStatus runtime = SchedulerRecoveryStatus.project(
                queue,
                Optional.of(pending),
                Optional.of(AgentRuntimeState.initial(GOAL_ID, workItem)),
                Optional.empty());

        assertEquals(
                SchedulerRecoveryStatus.RecoveryPhase.INTENT_RECORDED,
                intent.phase());
        assertEquals(
                SchedulerRecoveryStatus.RecoveryPhase.RUNTIME_RECORDED,
                runtime.phase());
        assertEquals(Optional.of(GOAL_ID), runtime.goalId());
        assertEquals(Optional.of(AGENT_RUN_ID), runtime.agentRunId());
        assertEquals(Optional.of(0L), runtime.runtimeRevision());
        assertEquals(Optional.of(RuntimeGoalStatus.ACCEPTED),
                runtime.goalStatus());
        assertEquals(Optional.of(SchedulerQueueStatus.WorkState.READY),
                runtime.queueWorkState());
    }

    @Test
    void rejectsRuntimeWorkThatIsNotTheExactQueueAdmission() {
        PendingFinalization pending = new PendingFinalization(
                GOAL_ID, AGENT_RUN_ID, Optional.empty());
        WorkItem admitted = workItem();
        WorkItem different = new WorkItem(
                WORK_ITEM_ID,
                "different-capability",
                admitted.workMessage());

        assertThrows(IllegalArgumentException.class, () ->
                SchedulerRecoveryStatus.project(
                        queueState(admitted),
                        Optional.of(pending),
                        Optional.of(AgentRuntimeState.initial(
                                GOAL_ID, different)),
                        Optional.empty()));
    }

    @Test
    void distinguishesRunRecordAndResultRecordingPrefixes() {
        WorkItem workItem = workItem();
        AgentRuntimeState executing = executing(workItem);
        PendingFinalization pending = new PendingFinalization(
                GOAL_ID, AGENT_RUN_ID, Optional.of(REFERENCE));
        ResolvedRunRecord record = resolvedRunRecord(false);

        SchedulerRecoveryStatus recorded =
                SchedulerRecoveryStatus.project(
                        queueState(workItem),
                        Optional.of(pending),
                        Optional.of(executing),
                        Optional.of(record));
        SchedulerRecoveryStatus awaiting =
                SchedulerRecoveryStatus.project(
                        queueState(workItem),
                        Optional.of(pending),
                        Optional.of(executing.completeExecution(
                                AGENT_RUN_ID,
                                "owner",
                                1,
                                NOW.plusSeconds(1))),
                        Optional.of(record));

        assertEquals(
                SchedulerRecoveryStatus.RecoveryPhase.RUN_RECORD_RECORDED,
                recorded.phase());
        assertEquals(Optional.of(RuntimeAgentRunStatus.EXECUTING),
                recorded.agentRunStatus());
        assertEquals(Optional.of(VerificationStatus.REJECTED),
                recorded.runRecordVerificationStatus());
        assertEquals(
                SchedulerRecoveryStatus.RecoveryPhase.RESULT_RECORDING_PENDING,
                awaiting.phase());
    }

    @Test
    void distinguishesRetryResolutionAndCheckpointedReplacement() {
        WorkItem workItem = workItem();
        AgentRuntimeState awaiting = executing(workItem)
                .completeExecution(
                        AGENT_RUN_ID,
                        "owner",
                        1,
                        NOW.plusSeconds(1));
        AgentRuntimeState retryPending = awaiting.recordAttemptResult(
                AGENT_RUN_ID,
                resultMessage(VerificationStatus.REJECTED));
        ResolvedRunRecord record = resolvedRunRecord(false);

        SchedulerRecoveryStatus retry =
                SchedulerRecoveryStatus.project(
                        queueState(workItem),
                        Optional.of(new PendingFinalization(
                                GOAL_ID,
                                AGENT_RUN_ID,
                                Optional.of(REFERENCE))),
                        Optional.of(retryPending),
                        Optional.of(record));
        SchedulerRecoveryStatus replacement =
                SchedulerRecoveryStatus.project(
                        queueState(workItem),
                        Optional.of(new PendingFinalization(
                                GOAL_ID,
                                AGENT_RUN_ID,
                                Optional.of(REFERENCE),
                                Optional.of(REPLACEMENT_AGENT_RUN_ID))),
                        Optional.of(retryPending),
                        Optional.of(record));

        assertEquals(
                SchedulerRecoveryStatus.RecoveryPhase.RETRY_RESOLUTION_PENDING,
                retry.phase());
        assertEquals(
                SchedulerRecoveryStatus.RecoveryPhase.REPLACEMENT_RECORDED,
                replacement.phase());
        assertEquals(Optional.of(REPLACEMENT_AGENT_RUN_ID),
                replacement.replacementAgentRunId());
    }

    @Test
    void distinguishesTerminalQueueDispositionFromCheckpointClearing() {
        WorkItem workItem = workItem();
        AgentRuntimeState completed = executing(workItem)
                .completeExecution(
                        AGENT_RUN_ID,
                        "owner",
                        1,
                        NOW.plusSeconds(1))
                .recordAttemptResult(
                        AGENT_RUN_ID,
                        resultMessage(VerificationStatus.VERIFIED));
        PendingFinalization pending = new PendingFinalization(
                GOAL_ID, AGENT_RUN_ID, Optional.of(REFERENCE));
        ResolvedRunRecord record = resolvedRunRecord(true);

        SchedulerRecoveryStatus disposition =
                SchedulerRecoveryStatus.project(
                        queueState(workItem),
                        Optional.of(pending),
                        Optional.of(completed),
                        Optional.of(record));
        SchedulerRecoveryStatus clearing =
                SchedulerRecoveryStatus.project(
                        verifiedQueueState(workItem),
                        Optional.of(pending),
                        Optional.of(completed),
                        Optional.of(record));

        assertEquals(
                SchedulerRecoveryStatus.RecoveryPhase.QUEUE_DISPOSITION_PENDING,
                disposition.phase());
        assertEquals(
                SchedulerRecoveryStatus.RecoveryPhase.CHECKPOINT_CLEAR_PENDING,
                clearing.phase());
        assertEquals(Optional.of(SchedulerQueueStatus.WorkState.VERIFIED),
                clearing.queueWorkState());
    }

    @Test
    void rejectsATerminalResultBoundToAnotherRunRecord() {
        WorkItem workItem = workItem();
        AgentRuntimeState completed = executing(workItem)
                .completeExecution(
                        AGENT_RUN_ID,
                        "owner",
                        1,
                        NOW.plusSeconds(1))
                .recordAttemptResult(
                        AGENT_RUN_ID,
                        resultMessage(VerificationStatus.VERIFIED));
        ResolvedRunRecord differentMetadata = new ResolvedRunRecord(
                new StoredRunRecord(
                        "00000000-0000-0000-0000-000000000958",
                        "run-record/00000000-0000-0000-0000-000000000958",
                        NOW,
                        1,
                        "c".repeat(64)),
                runRecord(true));

        assertThrows(IllegalArgumentException.class, () ->
                SchedulerRecoveryStatus.project(
                        verifiedQueueState(workItem),
                        Optional.of(new PendingFinalization(
                                GOAL_ID,
                                AGENT_RUN_ID,
                                Optional.of(REFERENCE))),
                        Optional.of(completed),
                        Optional.of(differentMetadata)));
    }

    private SchedulerQueueState queueState(WorkItem workItem) {
        SingleWorkerSchedulerQueue queue =
                new SingleWorkerSchedulerQueue(4);
        queue.enqueue(new QueuedWork(workItem, List.of()));
        return queue.snapshot(QUEUE_ID, 1);
    }

    private SchedulerQueueState verifiedQueueState(WorkItem workItem) {
        SingleWorkerSchedulerQueue queue =
                new SingleWorkerSchedulerQueue(4);
        queue.enqueue(new QueuedWork(workItem, List.of()));
        queue.claimNext();
        queue.completeActiveVerified(WORK_ITEM_ID);
        return queue.snapshot(QUEUE_ID, 3);
    }

    private AgentRuntimeState executing(WorkItem workItem) {
        return AgentRuntimeState.initial(GOAL_ID, workItem)
                .beginAgentRun(AGENT_RUN_ID)
                .markReady(AGENT_RUN_ID)
                .acquireLease(
                        AGENT_RUN_ID,
                        "owner",
                        NOW,
                        Duration.ofMinutes(5));
    }

    private MessageEnvelope resultMessage(
            VerificationStatus status) {
        WorkItem workItem = workItem();
        return new MessageEnvelope(
                "00000000-0000-0000-0000-000000000959",
                workItem.workMessage().correlationId(),
                Optional.of(workItem.workMessage().messageId()),
                workItem.logicalRunId(),
                "scheduler-recovery-status-test",
                NOW.plusSeconds(2),
                new ResultPayload(
                        workItem.taskRevision().taskId(),
                        REFERENCE,
                        status));
    }

    private ResolvedRunRecord resolvedRunRecord(boolean verified) {
        return new ResolvedRunRecord(
                new StoredRunRecord(
                        RECORD_ID,
                        REFERENCE,
                        NOW,
                        1,
                        "c".repeat(64)),
                runRecord(verified));
    }

    private RunRecord runRecord(boolean verified) {
        return new RunRecord(
                "evidence-run",
                NOW,
                new ApprovedTask(
                        "scheduler-recovery-status-test",
                        "Inspect Scheduler recovery",
                        "Approved by test",
                        Set.of("read-file"),
                        "CURRENT_TASK.md"),
                new ToolRequest(
                        "read-file",
                        "evidence-run",
                        Map.of("path", "CURRENT_TASK.md")),
                new PolicyDecision(
                        PolicyDecisionStatus.ALLOWED,
                        "C:/project",
                        Set.of("read-file"),
                        Set.of(),
                        4096,
                        1000),
                new ToolResult(
                        "read-file",
                        ToolResultStatus.SUCCESS,
                        OptionalInt.empty(),
                        VerificationEvidence.capture(
                                "read succeeded",
                                "content",
                                Optional.empty())),
                Optional.of("a".repeat(64)),
                verified
                        ? VerificationDecision.verified(
                                "content matched")
                        : VerificationDecision.rejected(
                                VerificationCode.CONTENT_MISMATCH,
                                "content differed"),
                1,
                AgentLoopStopReason.AWAITING_VERIFICATION,
                verified
                        ? AgentLoopStopReason.COMPLETED
                        : AgentLoopStopReason.AWAITING_VERIFICATION);
    }

    private WorkItem workItem() {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                "scheduler-recovery-status-test",
                "CURRENT_TASK.md",
                "a".repeat(64));
        MessageEnvelope envelope = new MessageEnvelope(
                "00000000-0000-0000-0000-000000000955",
                "scheduler-recovery-status-correlation",
                Optional.empty(),
                "scheduler-recovery-status-logical-run",
                "scheduler-recovery-status-test",
                NOW,
                new WorkPayload(
                        revision,
                        "b".repeat(64),
                        Set.of("read-file")));
        return new WorkItem(
                WORK_ITEM_ID, "read-file-worker", envelope);
    }
}
