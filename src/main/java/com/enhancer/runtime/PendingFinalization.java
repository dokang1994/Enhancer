package com.enhancer.runtime;

import java.util.Objects;
import java.util.Optional;

/**
 * The worker's durable cycle-intent checkpoint: the caller-owned stable Goal/AgentRun identities
 * of one scheduling cycle, plus the RunRecord reference once execution has produced it. Written
 * before the queue claim so an interrupted cycle can resume with the same identities.
 */
public record PendingFinalization(
        String goalId,
        String agentRunId,
        Optional<String> runRecordReference) {

    public PendingFinalization {
        goalId = RuntimeIdentity.canonicalUuid(goalId, "goalId");
        agentRunId = RuntimeIdentity.canonicalUuid(agentRunId, "agentRunId");
        if (goalId.equals(agentRunId)) {
            throw new IllegalArgumentException(
                    "goalId and agentRunId must be distinct");
        }
        Objects.requireNonNull(
                runRecordReference, "runRecordReference must not be null");
    }
}
