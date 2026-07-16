package com.enhancer.runtime;

import java.util.Objects;

/**
 * One immutable Goal over an exact admitted WorkItem. The WorkItem remains the source of
 * task, snapshot, logical-run, capability, and Tool-scope provenance.
 */
public record RuntimeGoal(
        String goalId,
        WorkItem workItem,
        RuntimeGoalStatus status) {

    public RuntimeGoal {
        goalId = RuntimeIdentity.canonicalUuid(goalId, "goalId");
        Objects.requireNonNull(workItem, "workItem must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (goalId.equals(workItem.workItemId())
                || goalId.equals(workItem.workMessage().messageId())) {
            throw new IllegalArgumentException(
                    "goalId must be distinct from work and message identities");
        }
    }

    RuntimeGoal withStatus(RuntimeGoalStatus nextStatus) {
        return new RuntimeGoal(goalId, workItem, nextStatus);
    }
}
