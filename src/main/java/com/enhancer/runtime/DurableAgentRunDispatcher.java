package com.enhancer.runtime;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Connects one durable Scheduler claim to the recoverable prefix of one fenced AgentRun.
 * The queue and runtime stores remain separate persisted boundaries.
 */
public final class DurableAgentRunDispatcher {
    private final DurableSingleWorkerSchedulerQueue queue;
    private final AgentRuntimeStateStore runtimeStore;
    private final Clock clock;

    public DurableAgentRunDispatcher(
            DurableSingleWorkerSchedulerQueue queue,
            AgentRuntimeStateStore runtimeStore,
            Clock clock) {
        this.queue = Objects.requireNonNull(
                queue, "queue must not be null");
        this.runtimeStore = Objects.requireNonNull(
                runtimeStore, "runtimeStore must not be null");
        this.clock = Objects.requireNonNull(
                clock, "clock must not be null");
    }

    public Optional<AgentRunDispatch> claimAndLease(
            String goalId,
            String agentRunId,
            String ownerId,
            Duration leaseDuration) throws IOException {
        String canonicalGoalId = RuntimeIdentity.canonicalUuid(
                goalId, "goalId");
        String canonicalAgentRunId = RuntimeIdentity.canonicalUuid(
                agentRunId, "agentRunId");
        if (canonicalGoalId.equals(canonicalAgentRunId)) {
            throw new IllegalArgumentException(
                    "goalId and agentRunId must be distinct");
        }
        AgentRunLease.validateRequest(ownerId, leaseDuration);

        Optional<WorkItem> selected = queue.activeWork();
        if (selected.isEmpty()) {
            selected = queue.claimNext();
        }
        if (selected.isEmpty()) {
            return Optional.empty();
        }
        WorkItem workItem = selected.orElseThrow();
        DurableAgentRuntime runtime = recoverOrCreate(
                canonicalGoalId,
                workItem);
        AgentRunLease lease = advanceToLease(
                runtime,
                canonicalAgentRunId,
                ownerId,
                leaseDuration);
        return Optional.of(new AgentRunDispatch(
                queue.queueId(),
                workItem,
                canonicalGoalId,
                canonicalAgentRunId,
                lease));
    }

    private DurableAgentRuntime recoverOrCreate(
            String goalId,
            WorkItem workItem) throws IOException {
        try {
            return DurableAgentRuntime.recoverMatching(
                    goalId,
                    workItem,
                    runtimeStore,
                    clock);
        } catch (MissingAgentRuntimeStateException exception) {
            return DurableAgentRuntime.create(
                    goalId,
                    workItem,
                    runtimeStore,
                    clock);
        }
    }

    private AgentRunLease advanceToLease(
            DurableAgentRuntime runtime,
            String agentRunId,
            String ownerId,
            Duration leaseDuration) throws IOException {
        while (true) {
            Optional<RuntimeAgentRun> optionalRun =
                    runtime.agentRun();
            if (optionalRun.isEmpty()) {
                runtime.beginAgentRun(agentRunId);
                continue;
            }
            RuntimeAgentRun run = optionalRun.orElseThrow();
            if (!run.agentRunId().equals(agentRunId)) {
                throw new IllegalStateException(
                        "existing AgentRun identity does not match the requested identity");
            }
            switch (run.status()) {
                case PLANNING -> runtime.markReady(agentRunId);
                case READY -> {
                    return runtime.acquireLease(
                            agentRunId,
                            ownerId,
                            leaseDuration);
                }
                case EXECUTING -> {
                    AgentRunLease lease = run.lease().orElseThrow();
                    if (!lease.ownerId().equals(ownerId)) {
                        throw new IllegalStateException(
                                "existing unexpired lease belongs to another owner");
                    }
                    return lease;
                }
                case AWAITING_VERIFICATION, COMPLETED, FAILED ->
                    throw new IllegalStateException(
                            "AgentRun has advanced beyond lease acquisition");
            }
        }
    }
}
