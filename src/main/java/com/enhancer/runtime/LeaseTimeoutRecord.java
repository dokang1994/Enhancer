package com.enhancer.runtime;

import java.time.Instant;
import java.util.Objects;

/** Exact expired-lease fact retained atomically with AgentRun reclamation. */
public record LeaseTimeoutRecord(
        String agentRunId,
        String ownerId,
        long fenceToken,
        Instant issuedAt,
        Instant expiresAt,
        Instant observedAt) {

    public LeaseTimeoutRecord {
        agentRunId = RuntimeIdentity.canonicalUuid(agentRunId, "agentRunId");
        ownerId = RuntimeEventContractSupport.bounded(
                ownerId, "ownerId", AgentRunLease.MAX_OWNER_CHARACTERS);
        if (fenceToken <= 0) {
            throw new IllegalArgumentException("fenceToken must be positive");
        }
        Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(observedAt, "observedAt must not be null");
        if (!expiresAt.isAfter(issuedAt) || observedAt.isBefore(expiresAt)) {
            throw new IllegalArgumentException(
                    "lease timeout times must retain issue < expiry <= observation");
        }
    }

    public String reference(String goalId) {
        return "agent-runtime/"
                + RuntimeIdentity.canonicalUuid(goalId, "goalId")
                + "/lease-timeout/"
                + agentRunId
                + "/"
                + fenceToken;
    }
}
