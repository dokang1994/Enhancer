package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Persist-before-exposure lifecycle wrapper for one Goal and one AgentRun.
 */
public final class DurableAgentRuntime {
    private final AgentRuntimeStateStore store;
    private final Clock clock;
    private AgentRuntimeState state;

    private DurableAgentRuntime(
            AgentRuntimeStateStore store,
            Clock clock,
            AgentRuntimeState state) {
        this.store = store;
        this.clock = clock;
        this.state = state;
    }

    public static DurableAgentRuntime create(
            String goalId,
            WorkItem workItem,
            AgentRuntimeStateStore store) throws IOException {
        return create(goalId, workItem, store, Clock.systemUTC());
    }

    public static DurableAgentRuntime create(
            String goalId,
            WorkItem workItem,
            AgentRuntimeStateStore store,
            Clock clock) throws IOException {
        Objects.requireNonNull(store, "store must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        AgentRuntimeState initial =
                AgentRuntimeState.initial(goalId, workItem);
        store.create(initial);
        return new DurableAgentRuntime(store, clock, initial);
    }

    public static DurableAgentRuntime recover(
            String goalId,
            AgentRuntimeStateStore store) throws IOException {
        return recover(goalId, store, Clock.systemUTC());
    }

    public static DurableAgentRuntime recover(
            String goalId,
            AgentRuntimeStateStore store,
            Clock clock) throws IOException {
        Objects.requireNonNull(store, "store must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        String canonicalGoalId =
                AgentRuntimeState.requireCanonicalGoalId(goalId);
        DurableAgentRuntime runtime = new DurableAgentRuntime(
                store,
                clock,
                store.resolve(canonicalGoalId));
        runtime.reclaimExpiredLease();
        return runtime;
    }

    public void beginAgentRun(String agentRunId) throws IOException {
        adoptAfterPersistence(state.beginAgentRun(agentRunId));
    }

    public void markReady(String agentRunId) throws IOException {
        adoptAfterPersistence(state.markReady(agentRunId));
    }

    public AgentRunLease acquireLease(
            String agentRunId,
            String ownerId,
            Duration duration) throws IOException {
        AgentRuntimeState next = state.acquireLease(
                agentRunId,
                ownerId,
                clock.instant(),
                duration);
        adoptAfterPersistence(next);
        return next.agentRun().orElseThrow().lease().orElseThrow();
    }

    public AgentRunLease renewLease(
            String agentRunId,
            String ownerId,
            long fenceToken,
            Duration duration) throws IOException {
        AgentRuntimeState next = state.renewLease(
                agentRunId,
                ownerId,
                fenceToken,
                clock.instant(),
                duration);
        adoptAfterPersistence(next);
        return next.agentRun().orElseThrow().lease().orElseThrow();
    }

    public void completeExecution(
            String agentRunId,
            String ownerId,
            long fenceToken) throws IOException {
        adoptAfterPersistence(state.completeExecution(
                agentRunId,
                ownerId,
                fenceToken,
                clock.instant()));
    }

    public boolean reclaimExpiredLease() throws IOException {
        Optional<AgentRuntimeState> reclaimed =
                state.reclaimExpiredLease(clock.instant());
        if (reclaimed.isEmpty()) {
            return false;
        }
        adoptAfterPersistence(reclaimed.orElseThrow());
        return true;
    }

    public void recordResult(
            String agentRunId,
            MessageEnvelope resultMessage) throws IOException {
        adoptAfterPersistence(
                state.recordResult(agentRunId, resultMessage));
    }

    public long revision() {
        return state.revision();
    }

    public RuntimeGoal goal() {
        return state.goal();
    }

    public long lastIssuedFenceToken() {
        return state.lastIssuedFenceToken();
    }

    public Optional<RuntimeAgentRun> agentRun() {
        return state.agentRun();
    }

    private void adoptAfterPersistence(
            AgentRuntimeState nextState) throws IOException {
        store.update(nextState);
        state = nextState;
    }
}
