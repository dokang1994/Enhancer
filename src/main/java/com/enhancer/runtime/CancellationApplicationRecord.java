package com.enhancer.runtime;

import java.time.Instant;
import java.util.Objects;

/** Immutable authorization-bound fact for one terminal AgentRuntime cancellation. */
public record CancellationApplicationRecord(
        String authorizationId,
        String actorId,
        String goalId,
        String controlMessageId,
        String agentRunId,
        Instant authorizedAt,
        Instant appliedAt) {
    public CancellationApplicationRecord {
        authorizationId = RuntimeIdentity.canonicalUuid(
                authorizationId, "authorizationId");
        actorId = bounded(actorId, "actorId");
        goalId = RuntimeIdentity.canonicalUuid(goalId, "goalId");
        controlMessageId = RuntimeIdentity.canonicalUuid(
                controlMessageId, "controlMessageId");
        agentRunId = RuntimeIdentity.canonicalUuid(agentRunId, "agentRunId");
        Objects.requireNonNull(authorizedAt, "authorizedAt must not be null");
        Objects.requireNonNull(appliedAt, "appliedAt must not be null");
        if (appliedAt.isBefore(authorizedAt)) {
            throw new IllegalArgumentException(
                    "cancellation application cannot precede authorization");
        }
    }

    public String reference() {
        return "agent-runtime/" + goalId + "/cancellation/" + controlMessageId;
    }

    private static String bounded(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        String checked = value.strip();
        if (checked.isEmpty()
                || checked.length() > ControlAuthorizationDecision.MAX_ACTOR_CHARACTERS) {
            throw new IllegalArgumentException(
                    field + " must be non-blank and at most 256 characters");
        }
        return checked;
    }
}
