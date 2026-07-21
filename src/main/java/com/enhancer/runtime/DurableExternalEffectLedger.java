package com.enhancer.runtime;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

/**
 * Persist-before-exposure effect ledger guarded by the exact current AgentRun lease. It records
 * effect intent and adapter-established outcomes but invokes no external system itself.
 */
public final class DurableExternalEffectLedger {
    private final AgentRuntimeStateStore runtimeStore;
    private final ExternalEffectLedgerStore effectStore;
    private final Clock clock;
    private ExternalEffectLedgerState state;

    private DurableExternalEffectLedger(
            AgentRuntimeStateStore runtimeStore,
            ExternalEffectLedgerStore effectStore,
            Clock clock,
            ExternalEffectLedgerState state) {
        this.runtimeStore = runtimeStore;
        this.effectStore = effectStore;
        this.clock = clock;
        this.state = state;
    }

    public static DurableExternalEffectLedger create(
            String goalId,
            AgentRuntimeStateStore runtimeStore,
            ExternalEffectLedgerStore effectStore,
            Clock clock) throws IOException {
        Objects.requireNonNull(runtimeStore, "runtimeStore must not be null");
        Objects.requireNonNull(effectStore, "effectStore must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        String canonicalGoalId = RuntimeIdentity.canonicalUuid(goalId, "goalId");
        AgentRuntimeState runtimeState = runtimeStore.resolve(canonicalGoalId);
        if (!runtimeState.goal().goalId().equals(canonicalGoalId)) {
            throw new IllegalStateException("runtime Goal identity does not match");
        }
        ExternalEffectLedgerState initial =
                ExternalEffectLedgerState.initial(canonicalGoalId);
        effectStore.create(initial);
        return new DurableExternalEffectLedger(
                runtimeStore, effectStore, clock, initial);
    }

    public static DurableExternalEffectLedger recover(
            String goalId,
            AgentRuntimeStateStore runtimeStore,
            ExternalEffectLedgerStore effectStore,
            Clock clock) throws IOException {
        Objects.requireNonNull(runtimeStore, "runtimeStore must not be null");
        Objects.requireNonNull(effectStore, "effectStore must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        String canonicalGoalId = RuntimeIdentity.canonicalUuid(goalId, "goalId");
        AgentRuntimeState runtimeState = runtimeStore.resolve(canonicalGoalId);
        ExternalEffectLedgerState loaded = effectStore.resolve(canonicalGoalId);
        if (!runtimeState.goal().goalId().equals(loaded.goalId())) {
            throw new IllegalStateException(
                    "runtime and external effect ledger Goals do not match");
        }
        return new DurableExternalEffectLedger(
                runtimeStore, effectStore, clock, loaded);
    }

    public ExternalEffectRecord prepare(
            ExternalEffectRequest request,
            String ownerId,
            long fenceToken) throws IOException {
        Objects.requireNonNull(request, "request must not be null");
        requireCurrentLease(request, ownerId, fenceToken);
        var existing = state.find(request.idempotencyKey());
        if (existing.isPresent()) {
            ExternalEffectRecord record = existing.orElseThrow();
            if (!record.request().equals(request)) {
                throw new IllegalArgumentException(
                        "idempotency key is already bound to a different effect");
            }
            return record;
        }
        ExternalEffectLedgerState next = state.prepare(request);
        adoptAfterPersistence(next);
        return next.find(request.idempotencyKey()).orElseThrow();
    }

    public ExternalEffectRecord recordOutcome(
            String idempotencyKey,
            ExternalEffectStatus outcome,
            String ownerId,
            long fenceToken) throws IOException {
        Objects.requireNonNull(outcome, "outcome must not be null");
        if (!outcome.isTerminal()) {
            throw new IllegalArgumentException(
                    "external effect outcome must be terminal");
        }
        ExternalEffectRecord current = state.find(idempotencyKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        "external effect is not prepared: " + idempotencyKey));
        requireCurrentLease(current.request(), ownerId, fenceToken);
        if (current.status().isTerminal()) {
            if (current.status() != outcome) {
                throw new IllegalStateException(
                        "external effect already has a different terminal outcome");
            }
            return current;
        }
        ExternalEffectLedgerState next =
                state.recordOutcome(idempotencyKey, outcome);
        adoptAfterPersistence(next);
        return next.find(idempotencyKey).orElseThrow();
    }

    public long revision() {
        return state.revision();
    }

    public List<ExternalEffectRecord> records() {
        return state.records();
    }

    private void requireCurrentLease(
            ExternalEffectRequest request,
            String ownerId,
            long fenceToken) throws IOException {
        if (!state.goalId().equals(request.goalId())) {
            throw new IllegalArgumentException(
                    "external effect Goal does not match ledger Goal");
        }
        AgentRuntimeState runtime = runtimeStore.resolve(state.goalId());
        if (!runtime.goal().workItem().workItemId().equals(request.workItemId())) {
            throw new IllegalArgumentException(
                    "external effect WorkItem does not match runtime work");
        }
        RuntimeAgentRun run = runtime.agentRun().orElseThrow(() ->
                new IllegalStateException("runtime has no AgentRun"));
        if (!run.agentRunId().equals(request.agentRunId())) {
            throw new IllegalArgumentException(
                    "external effect AgentRun does not match runtime");
        }
        if (run.status() != RuntimeAgentRunStatus.EXECUTING) {
            throw new IllegalStateException(
                    "external effects require an executing AgentRun");
        }
        AgentRunLease lease = run.lease().orElseThrow(() ->
                new IllegalStateException(
                        "executing AgentRun has no current lease"));
        lease.requireCurrent(ownerId, fenceToken, clock.instant());
    }

    private void adoptAfterPersistence(ExternalEffectLedgerState next)
            throws IOException {
        effectStore.update(next);
        state = next;
    }
}
