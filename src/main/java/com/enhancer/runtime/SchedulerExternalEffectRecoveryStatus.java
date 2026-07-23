package com.enhancer.runtime;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Pure conservative projection of the external-effect recovery state for one
 * checkpoint-correlated Scheduler Goal.
 */
public final class SchedulerExternalEffectRecoveryStatus {
    private final String queueId;
    private final long queueRevision;
    private final SchedulerRecoveryStatus.RecoveryPhase schedulerPhase;
    private final RecoveryPhase phase;
    private final Optional<String> goalId;
    private final Optional<String> workItemId;
    private final Optional<Long> runtimeRevision;
    private final Optional<Long> ledgerRevision;
    private final List<EffectStatus> effects;
    private final Map<ExternalEffectStatus, Integer> counts;
    private final int verifiedTerminalEvidence;

    private SchedulerExternalEffectRecoveryStatus(
            SchedulerRecoveryStatus scheduler,
            RecoveryPhase phase,
            Optional<String> goalId,
            Optional<String> workItemId,
            Optional<Long> runtimeRevision,
            Optional<Long> ledgerRevision,
            List<EffectStatus> effects,
            Map<ExternalEffectStatus, Integer> counts,
            int verifiedTerminalEvidence) {
        this.queueId = scheduler.queueId();
        this.queueRevision = scheduler.queueRevision();
        this.schedulerPhase = scheduler.phase();
        this.phase = phase;
        this.goalId = goalId;
        this.workItemId = workItemId;
        this.runtimeRevision = runtimeRevision;
        this.ledgerRevision = ledgerRevision;
        this.effects = List.copyOf(effects);
        this.counts = Map.copyOf(counts);
        this.verifiedTerminalEvidence = verifiedTerminalEvidence;
    }

    public static SchedulerExternalEffectRecoveryStatus project(
            SchedulerRecoveryStatus scheduler,
            Optional<AgentRuntimeState> runtimeState,
            Optional<ExternalEffectLedgerState> ledgerState,
            int verifiedTerminalEvidence) {
        Objects.requireNonNull(scheduler, "scheduler must not be null");
        Objects.requireNonNull(runtimeState, "runtimeState must not be null");
        Objects.requireNonNull(ledgerState, "ledgerState must not be null");
        if (verifiedTerminalEvidence < 0) {
            throw new IllegalArgumentException(
                    "verifiedTerminalEvidence must not be negative");
        }
        if (scheduler.goalId().isEmpty()) {
            if (runtimeState.isPresent()
                    || ledgerState.isPresent()
                    || verifiedTerminalEvidence != 0) {
                throw inconsistent(
                        "uncorrelated status cannot carry runtime or effect state");
            }
            return empty(
                    scheduler,
                    RecoveryPhase.NO_CORRELATED_GOAL,
                    Optional.empty(),
                    Optional.empty());
        }

        String goalId = scheduler.goalId().orElseThrow();
        if (runtimeState.isEmpty()) {
            if (scheduler.runtimeRevision().isPresent()
                    || ledgerState.isPresent()
                    || verifiedTerminalEvidence != 0) {
                throw inconsistent(
                        "pre-runtime status cannot carry runtime or effect state");
            }
            if (scheduler.phase()
                    != SchedulerRecoveryStatus.RecoveryPhase.INTENT_RECORDED) {
                throw inconsistent(
                        "missing runtime is valid only for recorded intent");
            }
            return empty(
                    scheduler,
                    RecoveryPhase.LEDGER_NOT_RECORDED,
                    Optional.of(goalId),
                    Optional.empty());
        }

        AgentRuntimeState runtime = runtimeState.orElseThrow();
        requireRuntimeBinding(scheduler, runtime);
        if (ledgerState.isEmpty()) {
            if (verifiedTerminalEvidence != 0) {
                throw inconsistent(
                        "missing ledger cannot have verified evidence");
            }
            if (scheduler.phase()
                    != SchedulerRecoveryStatus.RecoveryPhase.RUNTIME_RECORDED) {
                throw inconsistent(
                        "missing ledger is valid only while runtime is recorded");
            }
            return empty(
                    scheduler,
                    RecoveryPhase.LEDGER_CREATION_PENDING,
                    Optional.of(goalId),
                    Optional.of(runtime.goal().workItem().workItemId()));
        }

        ExternalEffectLedgerState ledger = ledgerState.orElseThrow();
        if (!goalId.equals(ledger.goalId())) {
            throw inconsistent(
                    "effect ledger Goal does not match Scheduler recovery Goal");
        }
        Set<String> agentRunIds = new HashSet<>();
        for (RuntimeAgentRun run : runtime.agentRuns()) {
            agentRunIds.add(run.agentRunId());
        }
        String workItemId = runtime.goal().workItem().workItemId();
        EnumMap<ExternalEffectStatus, Integer> counts =
                new EnumMap<>(ExternalEffectStatus.class);
        for (ExternalEffectStatus status : ExternalEffectStatus.values()) {
            counts.put(status, 0);
        }
        List<EffectStatus> effects = new ArrayList<>();
        int terminalEffects = 0;
        for (ExternalEffectRecord record : ledger.records()) {
            ExternalEffectRequest request = record.request();
            if (!goalId.equals(request.goalId())) {
                throw inconsistent(
                        "effect request Goal does not match correlated Goal");
            }
            if (!workItemId.equals(request.workItemId())) {
                throw inconsistent(
                        "effect request WorkItem does not match runtime work");
            }
            if (!agentRunIds.contains(request.agentRunId())) {
                throw inconsistent(
                        "effect request AgentRun is not retained by runtime");
            }
            counts.compute(record.status(), (ignored, value) -> value + 1);
            if (record.status().isTerminal()) {
                terminalEffects++;
            }
            effects.add(new EffectStatus(
                    request.idempotencyKey(),
                    record.status(),
                    request.agentRunId()));
        }
        if (verifiedTerminalEvidence != terminalEffects) {
            throw inconsistent(
                    "every terminal effect requires verified outcome evidence");
        }
        return new SchedulerExternalEffectRecoveryStatus(
                scheduler,
                phase(counts, effects.size()),
                Optional.of(goalId),
                Optional.of(workItemId),
                Optional.of(runtime.revision()),
                Optional.of(ledger.revision()),
                effects,
                counts,
                verifiedTerminalEvidence);
    }

    private static void requireRuntimeBinding(
            SchedulerRecoveryStatus scheduler,
            AgentRuntimeState runtime) {
        if (!scheduler.goalId().orElseThrow()
                .equals(runtime.goal().goalId())) {
            throw inconsistent(
                    "runtime Goal does not match Scheduler recovery Goal");
        }
        if (scheduler.runtimeRevision().isEmpty()
                || scheduler.runtimeRevision().orElseThrow()
                        != runtime.revision()) {
            throw inconsistent(
                    "runtime revision does not match Scheduler recovery");
        }
    }

    private static RecoveryPhase phase(
            Map<ExternalEffectStatus, Integer> counts,
            int total) {
        if (total == 0) {
            return RecoveryPhase.EMPTY_LEDGER;
        }
        if (counts.get(ExternalEffectStatus.PREPARED) > 0) {
            return RecoveryPhase.PREPARED_EFFECT_REQUIRES_RECOVERY;
        }
        if (counts.get(ExternalEffectStatus.REQUIRES_USER_RECOVERY) > 0) {
            return RecoveryPhase.USER_RECOVERY_REQUIRED;
        }
        if (counts.get(ExternalEffectStatus.APPLIED) > 0
                || counts.get(ExternalEffectStatus.DEDUPLICATED) > 0) {
            return RecoveryPhase.NON_COMPENSATED_EFFECT_RECORDED;
        }
        return RecoveryPhase.ALL_EFFECTS_COMPENSATED;
    }

    private static SchedulerExternalEffectRecoveryStatus empty(
            SchedulerRecoveryStatus scheduler,
            RecoveryPhase phase,
            Optional<String> goalId,
            Optional<String> workItemId) {
        EnumMap<ExternalEffectStatus, Integer> counts =
                new EnumMap<>(ExternalEffectStatus.class);
        for (ExternalEffectStatus status : ExternalEffectStatus.values()) {
            counts.put(status, 0);
        }
        return new SchedulerExternalEffectRecoveryStatus(
                scheduler,
                phase,
                goalId,
                workItemId,
                scheduler.runtimeRevision(),
                Optional.empty(),
                List.of(),
                counts,
                0);
    }

    private static IllegalArgumentException inconsistent(String message) {
        return new IllegalArgumentException(
                "Scheduler external-effect recovery state is inconsistent: "
                        + message);
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

    public Optional<String> workItemId() {
        return workItemId;
    }

    public Optional<Long> runtimeRevision() {
        return runtimeRevision;
    }

    public Optional<Long> ledgerRevision() {
        return ledgerRevision;
    }

    public List<EffectStatus> effects() {
        return effects;
    }

    public int count(ExternalEffectStatus status) {
        return counts.get(Objects.requireNonNull(
                status, "status must not be null"));
    }

    public int verifiedTerminalEvidence() {
        return verifiedTerminalEvidence;
    }

    public enum RecoveryPhase {
        NO_CORRELATED_GOAL,
        LEDGER_NOT_RECORDED,
        LEDGER_CREATION_PENDING,
        EMPTY_LEDGER,
        PREPARED_EFFECT_REQUIRES_RECOVERY,
        USER_RECOVERY_REQUIRED,
        NON_COMPENSATED_EFFECT_RECORDED,
        ALL_EFFECTS_COMPENSATED
    }

    public record EffectStatus(
            String idempotencyKey,
            ExternalEffectStatus status,
            String agentRunId) {
        public EffectStatus {
            Objects.requireNonNull(
                    idempotencyKey, "idempotencyKey must not be null");
            Objects.requireNonNull(status, "status must not be null");
            Objects.requireNonNull(
                    agentRunId, "agentRunId must not be null");
        }
    }
}
