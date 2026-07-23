package com.enhancer.runtime;

import com.enhancer.bus.ResultPayload;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.run.ResolvedRunRecord;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure checkpoint-anchored projection of one stable Scheduler recovery prefix.
 */
public final class SchedulerRecoveryStatus {
    private final String queueId;
    private final long queueRevision;
    private final RecoveryPhase phase;
    private final Optional<String> goalId;
    private final Optional<String> agentRunId;
    private final Optional<String> replacementAgentRunId;
    private final Optional<String> runRecordReference;
    private final Optional<Long> runtimeRevision;
    private final Optional<RuntimeGoalStatus> goalStatus;
    private final Optional<RuntimeAgentRunStatus> agentRunStatus;
    private final Optional<SchedulerQueueStatus.WorkState> queueWorkState;
    private final Optional<VerificationStatus> runRecordVerificationStatus;
    private final int agentRunAttempts;

    private SchedulerRecoveryStatus(
            String queueId,
            long queueRevision,
            RecoveryPhase phase,
            Optional<String> goalId,
            Optional<String> agentRunId,
            Optional<String> replacementAgentRunId,
            Optional<String> runRecordReference,
            Optional<Long> runtimeRevision,
            Optional<RuntimeGoalStatus> goalStatus,
            Optional<RuntimeAgentRunStatus> agentRunStatus,
            Optional<SchedulerQueueStatus.WorkState> queueWorkState,
            Optional<VerificationStatus> runRecordVerificationStatus,
            int agentRunAttempts) {
        this.queueId = queueId;
        this.queueRevision = queueRevision;
        this.phase = phase;
        this.goalId = goalId;
        this.agentRunId = agentRunId;
        this.replacementAgentRunId = replacementAgentRunId;
        this.runRecordReference = runRecordReference;
        this.runtimeRevision = runtimeRevision;
        this.goalStatus = goalStatus;
        this.agentRunStatus = agentRunStatus;
        this.queueWorkState = queueWorkState;
        this.runRecordVerificationStatus = runRecordVerificationStatus;
        this.agentRunAttempts = agentRunAttempts;
    }

    public static SchedulerRecoveryStatus project(
            SchedulerQueueState queueState,
            Optional<PendingFinalization> pending,
            Optional<AgentRuntimeState> runtimeState,
            Optional<ResolvedRunRecord> runRecord) {
        Objects.requireNonNull(queueState, "queueState must not be null");
        Objects.requireNonNull(pending, "pending must not be null");
        Objects.requireNonNull(runtimeState, "runtimeState must not be null");
        Objects.requireNonNull(runRecord, "runRecord must not be null");
        if (pending.isEmpty()) {
            if (runtimeState.isPresent() || runRecord.isPresent()) {
                throw inconsistent(
                        "runtime or RunRecord cannot be correlated without a checkpoint");
            }
            return empty(queueState);
        }

        PendingFinalization checkpoint = pending.orElseThrow();
        if (checkpoint.runRecordReference().isPresent()
                != runRecord.isPresent()) {
            throw inconsistent(
                    "RunRecord resolution must match the checkpoint reference");
        }
        if (runtimeState.isEmpty()) {
            if (checkpoint.runRecordReference().isPresent()
                    || checkpoint.replacementAgentRunId().isPresent()) {
                throw inconsistent(
                        "referenced recovery state requires AgentRuntime");
            }
            return intentOnly(queueState, checkpoint);
        }

        AgentRuntimeState runtime = runtimeState.orElseThrow();
        validateGoal(checkpoint, runtime);
        SchedulerQueueStatus.WorkState workState =
                exactQueueWorkState(queueState, runtime.goal().workItem());
        validateRunRecord(checkpoint, runtime, runRecord);
        RecoveryPhase phase = phase(checkpoint, runtime, workState);
        Optional<RuntimeAgentRun> latest = runtime.agentRun();
        return new SchedulerRecoveryStatus(
                queueState.queueId(),
                queueState.revision(),
                phase,
                Optional.of(checkpoint.goalId()),
                Optional.of(checkpoint.agentRunId()),
                checkpoint.replacementAgentRunId(),
                checkpoint.runRecordReference(),
                Optional.of(runtime.revision()),
                Optional.of(runtime.goal().status()),
                latest.map(RuntimeAgentRun::status),
                Optional.of(workState),
                runRecord.map(value ->
                        value.record().verification().status()),
                runtime.agentRuns().size());
    }

    private static SchedulerRecoveryStatus empty(
            SchedulerQueueState queueState) {
        return new SchedulerRecoveryStatus(
                queueState.queueId(),
                queueState.revision(),
                RecoveryPhase.NO_PENDING_CYCLE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                0);
    }

    private static SchedulerRecoveryStatus intentOnly(
            SchedulerQueueState queueState,
            PendingFinalization checkpoint) {
        return new SchedulerRecoveryStatus(
                queueState.queueId(),
                queueState.revision(),
                RecoveryPhase.INTENT_RECORDED,
                Optional.of(checkpoint.goalId()),
                Optional.of(checkpoint.agentRunId()),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                0);
    }

    private static void validateGoal(
            PendingFinalization checkpoint,
            AgentRuntimeState runtime) {
        if (!runtime.goal().goalId().equals(checkpoint.goalId())) {
            throw inconsistent(
                    "checkpoint Goal does not match AgentRuntime");
        }
        Optional<RuntimeAgentRun> latest = runtime.agentRun();
        if (checkpoint.replacementAgentRunId().isPresent()) {
            String replacement =
                    checkpoint.replacementAgentRunId().orElseThrow();
            if (latest.isEmpty()
                    || (!latest.orElseThrow().agentRunId().equals(
                                    checkpoint.agentRunId())
                            && !latest.orElseThrow().agentRunId().equals(
                                    replacement))) {
                throw inconsistent(
                        "checkpoint replacement does not match AgentRuntime");
            }
            if (runtime.goal().status() != RuntimeGoalStatus.RETRY_PENDING
                    && runtime.goal().status() != RuntimeGoalStatus.ACTIVE) {
                throw inconsistent(
                        "replacement checkpoint requires retry-pending or active Goal");
            }
        } else if (latest.isPresent()
                && !latest.orElseThrow().agentRunId().equals(
                        checkpoint.agentRunId())) {
            throw inconsistent(
                    "checkpoint AgentRun does not match AgentRuntime");
        }
    }

    private static SchedulerQueueStatus.WorkState exactQueueWorkState(
            SchedulerQueueState queueState,
            WorkItem runtimeWorkItem) {
        List<QueuedWork> admitted = queueState.admittedWork();
        QueuedWork exact = admitted.stream()
                .filter(value -> value.workItem().workItemId().equals(
                        runtimeWorkItem.workItemId()))
                .findFirst()
                .orElseThrow(() -> inconsistent(
                        "AgentRuntime WorkItem is not admitted to the queue"));
        if (!exact.workItem().equals(runtimeWorkItem)) {
            throw inconsistent(
                    "AgentRuntime WorkItem does not match the exact queue admission");
        }
        return SchedulerQueueStatus.project(queueState).workItems().stream()
                .filter(value -> value.workItemId().equals(
                        runtimeWorkItem.workItemId()))
                .map(SchedulerQueueStatus.WorkStatus::state)
                .findFirst()
                .orElseThrow(() -> inconsistent(
                        "queue status does not contain the runtime WorkItem"));
    }

    private static void validateRunRecord(
            PendingFinalization checkpoint,
            AgentRuntimeState runtime,
            Optional<ResolvedRunRecord> runRecord) {
        if (runRecord.isEmpty()) {
            return;
        }
        ResolvedRunRecord resolved = runRecord.orElseThrow();
        String reference =
                checkpoint.runRecordReference().orElseThrow();
        if (!resolved.metadata().reference().equals(reference)) {
            throw inconsistent(
                    "resolved RunRecord does not match the checkpoint reference");
        }
        DurableAgentRunFinalizer.requireBinding(
                resolved.record(), runtime.goal().workItem());
        Optional<RuntimeAgentRun> latest = runtime.agentRun();
        if (latest.isPresent() && latest.orElseThrow().status().isTerminal()) {
            ResultPayload result = (ResultPayload) latest.orElseThrow()
                    .resultMessage().orElseThrow().payload();
            if (!result.runRecordReference().equals(reference)
                    || result.verificationStatus()
                            != resolved.record().verification().status()) {
                throw inconsistent(
                        "terminal AgentRuntime result does not match the RunRecord");
            }
        }
    }

    private static RecoveryPhase phase(
            PendingFinalization checkpoint,
            AgentRuntimeState runtime,
            SchedulerQueueStatus.WorkState workState) {
        RuntimeGoalStatus goal = runtime.goal().status();
        Optional<RuntimeAgentRun> latest = runtime.agentRun();
        if (goal == RuntimeGoalStatus.COMPLETED
                || goal == RuntimeGoalStatus.FAILED) {
            SchedulerQueueStatus.WorkState terminal =
                    goal == RuntimeGoalStatus.COMPLETED
                            ? SchedulerQueueStatus.WorkState.VERIFIED
                            : SchedulerQueueStatus.WorkState.FAILED;
            if (workState == terminal) {
                return RecoveryPhase.CHECKPOINT_CLEAR_PENDING;
            }
            requireRecoverableQueueWork(workState);
            return RecoveryPhase.QUEUE_DISPOSITION_PENDING;
        }

        requireRecoverableQueueWork(workState);
        if (checkpoint.replacementAgentRunId().isPresent()) {
            return RecoveryPhase.REPLACEMENT_RECORDED;
        }
        if (goal == RuntimeGoalStatus.RETRY_PENDING) {
            if (latest.isEmpty()
                    || latest.orElseThrow().status()
                            != RuntimeAgentRunStatus.FAILED
                    || checkpoint.runRecordReference().isEmpty()) {
                throw inconsistent(
                        "retry-pending Goal requires a failed checkpointed attempt");
            }
            return RecoveryPhase.RETRY_RESOLUTION_PENDING;
        }
        if (goal != RuntimeGoalStatus.ACCEPTED
                && goal != RuntimeGoalStatus.ACTIVE) {
            throw inconsistent("Goal status is not a recoverable prefix");
        }
        if (latest.isEmpty()) {
            if (checkpoint.runRecordReference().isPresent()) {
                throw inconsistent(
                        "RunRecord checkpoint requires an AgentRun");
            }
            return RecoveryPhase.RUNTIME_RECORDED;
        }
        return switch (latest.orElseThrow().status()) {
            case PLANNING, READY, EXECUTING ->
                    checkpoint.runRecordReference().isPresent()
                            ? RecoveryPhase.RUN_RECORD_RECORDED
                            : RecoveryPhase.RUNTIME_RECORDED;
            case AWAITING_VERIFICATION -> {
                if (checkpoint.runRecordReference().isEmpty()) {
                    throw inconsistent(
                            "verification-waiting AgentRun requires a RunRecord");
                }
                yield RecoveryPhase.RESULT_RECORDING_PENDING;
            }
            case COMPLETED, FAILED -> throw inconsistent(
                    "terminal AgentRun requires matching terminal Goal");
        };
    }

    private static void requireRecoverableQueueWork(
            SchedulerQueueStatus.WorkState state) {
        if (state != SchedulerQueueStatus.WorkState.READY
                && state != SchedulerQueueStatus.WorkState.ACTIVE) {
            throw inconsistent(
                    "non-terminal runtime requires ready or active queue work");
        }
    }

    private static IllegalArgumentException inconsistent(String message) {
        return new IllegalArgumentException(
                "Scheduler recovery prefix is inconsistent: " + message);
    }

    public String queueId() {
        return queueId;
    }

    public long queueRevision() {
        return queueRevision;
    }

    public RecoveryPhase phase() {
        return phase;
    }

    public Optional<String> goalId() {
        return goalId;
    }

    public Optional<String> agentRunId() {
        return agentRunId;
    }

    public Optional<String> replacementAgentRunId() {
        return replacementAgentRunId;
    }

    public Optional<String> runRecordReference() {
        return runRecordReference;
    }

    public Optional<Long> runtimeRevision() {
        return runtimeRevision;
    }

    public Optional<RuntimeGoalStatus> goalStatus() {
        return goalStatus;
    }

    public Optional<RuntimeAgentRunStatus> agentRunStatus() {
        return agentRunStatus;
    }

    public Optional<SchedulerQueueStatus.WorkState> queueWorkState() {
        return queueWorkState;
    }

    public Optional<VerificationStatus> runRecordVerificationStatus() {
        return runRecordVerificationStatus;
    }

    public int agentRunAttempts() {
        return agentRunAttempts;
    }

    public enum RecoveryPhase {
        NO_PENDING_CYCLE,
        INTENT_RECORDED,
        RUNTIME_RECORDED,
        RUN_RECORD_RECORDED,
        RESULT_RECORDING_PENDING,
        RETRY_RESOLUTION_PENDING,
        REPLACEMENT_RECORDED,
        QUEUE_DISPOSITION_PENDING,
        CHECKPOINT_CLEAR_PENDING
    }
}
