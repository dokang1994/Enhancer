package com.enhancer.runtime;

import java.util.Objects;

/**
 * Immutable result of connecting one durable queue claim to one currently leased AgentRun.
 */
public record AgentRunDispatch(
        String queueId,
        WorkItem workItem,
        String goalId,
        String agentRunId,
        AgentRunLease lease) {

    public AgentRunDispatch {
        queueId = SchedulerQueueState.requireCanonicalQueueId(queueId);
        Objects.requireNonNull(workItem, "workItem must not be null");
        goalId = RuntimeIdentity.canonicalUuid(goalId, "goalId");
        agentRunId = RuntimeIdentity.canonicalUuid(
                agentRunId, "agentRunId");
        Objects.requireNonNull(lease, "lease must not be null");
        if (goalId.equals(workItem.workItemId())
                || goalId.equals(workItem.workMessage().messageId())) {
            throw new IllegalArgumentException(
                    "goalId must be distinct from work identities");
        }
        if (agentRunId.equals(goalId)
                || agentRunId.equals(workItem.workItemId())
                || agentRunId.equals(
                        workItem.workMessage().messageId())) {
            throw new IllegalArgumentException(
                    "agentRunId must be distinct from Goal and work identities");
        }
    }
}
