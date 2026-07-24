package com.enhancer.runtime;

import java.util.Objects;
import java.util.Optional;

/**
 * Pure read-only projection of the process-isolated invocation namespace correlated to one
 * Scheduler recovery cycle.
 */
public final class SchedulerInvocationRecoveryStatus {
    private final String queueId;
    private final long queueRevision;
    private final SchedulerRecoveryStatus.RecoveryPhase schedulerPhase;
    private final RecoveryPhase phase;
    private final Optional<String> goalId;
    private final Optional<String> agentRunId;
    private final Optional<String> workItemId;
    private final Optional<Long> runtimeRevision;
    private final boolean invocationPresent;
    private final boolean workMessagePresent;
    private final boolean resultMessagePresent;

    private SchedulerInvocationRecoveryStatus(
            SchedulerRecoveryStatus scheduler,
            RecoveryPhase phase,
            Optional<String> goalId,
            Optional<String> agentRunId,
            Optional<String> workItemId,
            Optional<Long> runtimeRevision,
            boolean invocationPresent,
            boolean workMessagePresent,
            boolean resultMessagePresent) {
        this.queueId = scheduler.queueId();
        this.queueRevision = scheduler.queueRevision();
        this.schedulerPhase = scheduler.phase();
        this.phase = phase;
        this.goalId = goalId;
        this.agentRunId = agentRunId;
        this.workItemId = workItemId;
        this.runtimeRevision = runtimeRevision;
        this.invocationPresent = invocationPresent;
        this.workMessagePresent = workMessagePresent;
        this.resultMessagePresent = resultMessagePresent;
    }

    public static SchedulerInvocationRecoveryStatus project(
            SchedulerRecoveryStatus scheduler,
            Optional<AgentRuntimeState> runtimeState,
            Optional<InvocationSpoolState> spoolState) {
        Objects.requireNonNull(scheduler, "scheduler must not be null");
        Objects.requireNonNull(runtimeState, "runtimeState must not be null");
        Objects.requireNonNull(spoolState, "spoolState must not be null");
        if (scheduler.goalId().isEmpty() || scheduler.agentRunId().isEmpty()) {
            if (scheduler.goalId().isPresent() != scheduler.agentRunId().isPresent()
                    || runtimeState.isPresent() || spoolState.isPresent()) {
                throw inconsistent("uncorrelated cycle cannot carry runtime or spool state");
            }
            return new SchedulerInvocationRecoveryStatus(
                    scheduler,
                    RecoveryPhase.NO_CORRELATED_CYCLE,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    false,
                    false,
                    false);
        }

        String goalId = scheduler.goalId().orElseThrow();
        String agentRunId = scheduler.agentRunId().orElseThrow();
        if (runtimeState.isEmpty()) {
            if (scheduler.runtimeRevision().isPresent() || spoolState.isPresent()) {
                throw inconsistent("unrecorded runtime cannot carry a spool state");
            }
            return new SchedulerInvocationRecoveryStatus(
                    scheduler,
                    RecoveryPhase.RUNTIME_NOT_RECORDED,
                    Optional.of(goalId),
                    Optional.of(agentRunId),
                    Optional.empty(),
                    Optional.empty(),
                    false,
                    false,
                    false);
        }

        AgentRuntimeState runtime = runtimeState.orElseThrow();
        if (!goalId.equals(runtime.goal().goalId())
                || scheduler.runtimeRevision().isEmpty()
                || scheduler.runtimeRevision().orElseThrow() != runtime.revision()
                || runtime.agentRuns().stream().noneMatch(run ->
                        agentRunId.equals(run.agentRunId()))) {
            throw inconsistent("runtime does not match the correlated Scheduler cycle");
        }
        InvocationSpoolState spool = spoolState.orElseThrow(() ->
                inconsistent("recorded runtime requires an invocation spool observation"));
        if (!spool.invocationPresent()) {
            return status(
                    scheduler,
                    RecoveryPhase.INVOCATION_ABSENT,
                    goalId,
                    agentRunId,
                    runtime,
                    spool);
        }
        if (!spool.workMessagePresent()) {
            if (spool.resultMessagePresent()) {
                throw inconsistent("a result message cannot exist without work");
            }
            return status(
                    scheduler,
                    RecoveryPhase.WORK_MESSAGE_ABSENT,
                    goalId,
                    agentRunId,
                    runtime,
                    spool);
        }
        return status(
                scheduler,
                spool.resultMessagePresent()
                        ? RecoveryPhase.RESULT_MESSAGE_PUBLISHED
                        : RecoveryPhase.WORK_MESSAGE_AWAITING_RESULT,
                goalId,
                agentRunId,
                runtime,
                spool);
    }

    private static SchedulerInvocationRecoveryStatus status(
            SchedulerRecoveryStatus scheduler,
            RecoveryPhase phase,
            String goalId,
            String agentRunId,
            AgentRuntimeState runtime,
            InvocationSpoolState spool) {
        return new SchedulerInvocationRecoveryStatus(
                scheduler,
                phase,
                Optional.of(goalId),
                Optional.of(agentRunId),
                Optional.of(runtime.goal().workItem().workItemId()),
                Optional.of(runtime.revision()),
                spool.invocationPresent(),
                spool.workMessagePresent(),
                spool.resultMessagePresent());
    }

    private static IllegalArgumentException inconsistent(String message) {
        return new IllegalArgumentException(
                "Scheduler invocation recovery state is inconsistent: " + message);
    }

    public String queueId() {
        return queueId;
    }

    public long queueRevision() {
        return queueRevision;
    }

    public SchedulerRecoveryStatus.RecoveryPhase schedulerPhase() {
        return schedulerPhase;
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

    public Optional<String> workItemId() {
        return workItemId;
    }

    public Optional<Long> runtimeRevision() {
        return runtimeRevision;
    }

    public boolean invocationPresent() {
        return invocationPresent;
    }

    public boolean workMessagePresent() {
        return workMessagePresent;
    }

    public boolean resultMessagePresent() {
        return resultMessagePresent;
    }

    public enum RecoveryPhase {
        NO_CORRELATED_CYCLE,
        RUNTIME_NOT_RECORDED,
        INVOCATION_ABSENT,
        WORK_MESSAGE_ABSENT,
        WORK_MESSAGE_AWAITING_RESULT,
        RESULT_MESSAGE_PUBLISHED
    }

    /** Snapshot of the one private invocation namespace after successful message validation. */
    public record InvocationSpoolState(
            boolean invocationPresent,
            boolean workMessagePresent,
            boolean resultMessagePresent) {
        public InvocationSpoolState {
            if (!invocationPresent && (workMessagePresent || resultMessagePresent)) {
                throw new IllegalArgumentException(
                        "absent invocation cannot contain messages");
            }
        }
    }
}
