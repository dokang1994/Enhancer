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
        Optional<String> runRecordReference,
        Optional<String> replacementAgentRunId) {

    public PendingFinalization(
            String goalId,
            String agentRunId,
            Optional<String> runRecordReference) {
        this(goalId, agentRunId, runRecordReference, Optional.empty());
    }

    public PendingFinalization {
        goalId = RuntimeIdentity.canonicalUuid(goalId, "goalId");
        agentRunId = RuntimeIdentity.canonicalUuid(agentRunId, "agentRunId");
        if (goalId.equals(agentRunId)) {
            throw new IllegalArgumentException(
                    "goalId and agentRunId must be distinct");
        }
        Objects.requireNonNull(
                runRecordReference, "runRecordReference must not be null");
        Objects.requireNonNull(
                replacementAgentRunId,
                "replacementAgentRunId must not be null");
        if (replacementAgentRunId.isPresent()) {
            if (runRecordReference.isEmpty()) {
                throw new IllegalArgumentException(
                        "replacementAgentRunId requires a RunRecord reference");
            }
            String replacement = RuntimeIdentity.canonicalUuid(
                    replacementAgentRunId.orElseThrow(),
                    "replacementAgentRunId");
            if (replacement.equals(goalId) || replacement.equals(agentRunId)) {
                throw new IllegalArgumentException(
                        "replacementAgentRunId must be distinct from current identities");
            }
            replacementAgentRunId = Optional.of(replacement);
        }
    }
}
