package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Persist-before-exposure lifecycle wrapper for one Goal and its AgentRun history.
 */
public final class DurableAgentRuntime {
    private static final String EVENT_PRODUCER_ID = "durable-agent-runtime";

    private final AgentRuntimeStateStore store;
    private final Clock clock;
    private final Optional<RuntimeEventRecorder> eventRecorder;
    private AgentRuntimeState state;

    private DurableAgentRuntime(
            AgentRuntimeStateStore store,
            Clock clock,
            AgentRuntimeState state) {
        this(store, clock, state, Optional.empty());
    }

    private DurableAgentRuntime(
            AgentRuntimeStateStore store,
            Clock clock,
            AgentRuntimeState state,
            Optional<RuntimeEventRecorder> eventRecorder) {
        this.store = store;
        this.clock = clock;
        this.state = state;
        this.eventRecorder = eventRecorder;
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
        return recoverLoaded(
                goalId, Optional.empty(), store, clock, true, Optional.empty());
    }

    public static DurableAgentRuntime recover(
            String goalId,
            AgentRuntimeStateStore store,
            Clock clock,
            RuntimeEventRecorder eventRecorder) throws IOException {
        return recoverLoaded(
                goalId,
                Optional.empty(),
                store,
                clock,
                true,
                Optional.of(Objects.requireNonNull(
                        eventRecorder, "eventRecorder must not be null")));
    }

    static DurableAgentRuntime recoverMatching(
            String goalId,
            WorkItem expectedWorkItem,
            AgentRuntimeStateStore store,
            Clock clock) throws IOException {
        return recoverLoaded(
                goalId,
                Optional.of(Objects.requireNonNull(
                        expectedWorkItem,
                        "expectedWorkItem must not be null")),
                store,
                clock,
                true,
                Optional.empty());
    }

    static DurableAgentRuntime recoverMatching(
            String goalId,
            WorkItem expectedWorkItem,
            AgentRuntimeStateStore store,
            Clock clock,
            RuntimeEventRecorder eventRecorder) throws IOException {
        return recoverLoaded(
                goalId,
                Optional.of(Objects.requireNonNull(
                        expectedWorkItem,
                        "expectedWorkItem must not be null")),
                store,
                clock,
                true,
                Optional.of(Objects.requireNonNull(
                        eventRecorder, "eventRecorder must not be null")));
    }

    static DurableAgentRuntime recoverForControlAdmission(
            String goalId,
            AgentRuntimeStateStore store,
            Clock clock) throws IOException {
        return recoverLoaded(
                goalId,
                Optional.empty(),
                store,
                clock,
                false,
                Optional.empty());
    }

    private static DurableAgentRuntime recoverLoaded(
            String goalId,
            Optional<WorkItem> expectedWorkItem,
            AgentRuntimeStateStore store,
            Clock clock,
            boolean reclaimExpiredLease,
            Optional<RuntimeEventRecorder> eventRecorder) throws IOException {
        Objects.requireNonNull(store, "store must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        Objects.requireNonNull(
                expectedWorkItem, "expectedWorkItem must not be null");
        Objects.requireNonNull(eventRecorder, "eventRecorder must not be null");
        String canonicalGoalId =
                AgentRuntimeState.requireCanonicalGoalId(goalId);
        AgentRuntimeState loaded = store.resolve(canonicalGoalId);
        expectedWorkItem.ifPresent(expected -> {
            if (!loaded.goal().workItem().equals(expected)) {
                throw new IllegalStateException(
                        "existing Goal WorkItem does not match expected work");
            }
        });
        DurableAgentRuntime runtime = new DurableAgentRuntime(
                store,
                clock,
                loaded,
                eventRecorder);
        if (reclaimExpiredLease) {
            runtime.reclaimExpiredLease();
        }
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
        if (reclaimed.isPresent()) {
            adoptAfterPersistence(reclaimed.orElseThrow());
        }
        recordLeaseTimeoutEvents();
        return reclaimed.isPresent();
    }

    public void recordResult(
            String agentRunId,
            MessageEnvelope resultMessage) throws IOException {
        adoptAfterPersistence(
                state.recordAttemptResult(agentRunId, resultMessage));
    }

    public boolean recordRetryDecision(
            AgentRunRetryDecisionRecord decision) throws IOException {
        Optional<AgentRuntimeState> next = state.recordRetryDecision(decision);
        if (next.isEmpty()) {
            return false;
        }
        adoptAfterPersistence(next.orElseThrow());
        return true;
    }

    public void beginRetryAgentRun(String agentRunId) throws IOException {
        adoptAfterPersistence(state.beginRetryAgentRun(agentRunId));
    }

    public void abandonGoal() throws IOException {
        adoptAfterPersistence(state.abandonGoal());
    }

    public boolean recordControlRequest(
            MessageEnvelope request) throws IOException {
        Optional<AgentRuntimeState> next =
                state.recordControlRequest(request);
        if (next.isEmpty()) {
            return false;
        }
        adoptAfterPersistence(next.orElseThrow());
        return true;
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

    public List<RuntimeAgentRun> agentRuns() {
        return state.agentRuns();
    }

    public List<AgentRunRetryDecisionRecord> retryDecisions() {
        return state.retryDecisions();
    }

    public List<LeaseTimeoutRecord> leaseTimeouts() {
        return state.leaseTimeouts();
    }

    public Optional<CancellationApplicationRecord> cancellationApplication() {
        return state.cancellationApplication();
    }

    public int completedAttempts() {
        return state.completedAttempts();
    }

    public List<MessageEnvelope> controlRequests() {
        return state.controlRequests();
    }

    void applyCancellation(CancellationApplicationRecord record)
            throws IOException {
        Optional<AgentRuntimeState> next = state.applyCancellation(record);
        if (next.isPresent()) {
            adoptAfterPersistence(next.orElseThrow());
        }
    }

    private void adoptAfterPersistence(
            AgentRuntimeState nextState) throws IOException {
        store.update(nextState);
        state = nextState;
    }

    private void recordLeaseTimeoutEvents() throws IOException {
        if (eventRecorder.isEmpty()) {
            return;
        }
        WorkItem workItem = state.goal().workItem();
        RuntimeEventBinding binding = new RuntimeEventBinding(
                state.goal().goalId(),
                workItem.workItemId(),
                workItem.taskRevision(),
                workItem.snapshotId(),
                workItem.logicalRunId(),
                workItem.workMessage().correlationId());
        for (LeaseTimeoutRecord timeout : state.leaseTimeouts()) {
            RuntimeEvent event = RuntimeEvent.create(
                    timeout.expiresAt(),
                    binding,
                    timeout.agentRunId(),
                    Optional.of(workItem.workMessage().messageId()),
                    EVENT_PRODUCER_ID,
                    new RuntimeEventDetail.TimeoutDetected(
                            RuntimeTimeoutKind.LEASE),
                    List.of(new RuntimeEventReference(
                            RuntimeEventReferenceKind.LEASE_TIMEOUT,
                            timeout.reference(state.goal().goalId()),
                            Optional.empty())));
            eventRecorder.orElseThrow().recordAndPublish(event);
        }
    }
}
