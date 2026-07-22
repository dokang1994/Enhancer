package com.enhancer.runtime;

import java.util.Locale;
import java.util.Objects;

/** Immutable durable evidence of one retry decision for one failed AgentRun attempt. */
public record AgentRunRetryDecisionRecord(
        String agentRunId,
        int completedAttempts,
        int maxAttempts,
        long externalEffectLedgerRevision,
        int externalEffectRecordCount,
        String externalEffectLedgerSemanticSha256,
        AgentRunRetryDecision decision) {

    public AgentRunRetryDecisionRecord {
        agentRunId = RuntimeIdentity.canonicalUuid(agentRunId, "agentRunId");
        if (completedAttempts < 1
                || completedAttempts > AgentRunRetryPolicy.MAX_ATTEMPTS) {
            throw new IllegalArgumentException(
                    "completedAttempts must be between 1 and "
                            + AgentRunRetryPolicy.MAX_ATTEMPTS);
        }
        new AgentRunRetryPolicy(maxAttempts);
        if (externalEffectLedgerRevision < 0) {
            throw new IllegalArgumentException(
                    "externalEffectLedgerRevision must not be negative");
        }
        if (externalEffectRecordCount < 0
                || externalEffectRecordCount > ExternalEffectLedgerState.MAX_EFFECTS) {
            throw new IllegalArgumentException(
                    "externalEffectRecordCount is outside supported bounds");
        }
        Objects.requireNonNull(
                externalEffectLedgerSemanticSha256,
                "externalEffectLedgerSemanticSha256 must not be null");
        if (!externalEffectLedgerSemanticSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "externalEffectLedgerSemanticSha256 must be lowercase SHA-256");
        }
        externalEffectLedgerSemanticSha256 =
                externalEffectLedgerSemanticSha256.toLowerCase(Locale.ROOT);
        Objects.requireNonNull(decision, "decision must not be null");
    }
}
