package com.enhancer.runtime;

import java.util.Objects;

/**
 * Pure, fail-closed decision on whether a further AgentRun may be admitted for one WorkItem
 * after a terminal disposition. It creates, persists, and runs nothing, mutates no store, and
 * grants no authority. An unresolved (PREPARED) or recovery-pending (REQUIRES_USER_RECOVERY)
 * external effect blocks retry regardless of remaining attempt budget, so retry can never hide
 * a replayed effect or proceed past a state that needs a human.
 */
public final class AgentRunRetryDecider {

    public AgentRunRetryDecision decide(
            WorkItemDisposition lastDisposition,
            int completedAttempts,
            AgentRunRetryPolicy policy,
            ExternalEffectLedgerState ledgerState) {
        Objects.requireNonNull(lastDisposition, "lastDisposition must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        Objects.requireNonNull(ledgerState, "ledgerState must not be null");
        if (completedAttempts < 1 || completedAttempts > AgentRunRetryPolicy.MAX_ATTEMPTS) {
            throw new IllegalArgumentException(
                    "completedAttempts must be between 1 and "
                            + AgentRunRetryPolicy.MAX_ATTEMPTS);
        }

        if (lastDisposition != WorkItemDisposition.FAILED) {
            return AgentRunRetryDecision.refused(AgentRunRetryRefusalReason.NOT_FAILED);
        }
        if (hasEffectWithStatus(ledgerState, ExternalEffectStatus.PREPARED)) {
            return AgentRunRetryDecision.refused(
                    AgentRunRetryRefusalReason.UNRESOLVED_EXTERNAL_EFFECT);
        }
        if (hasEffectWithStatus(ledgerState, ExternalEffectStatus.REQUIRES_USER_RECOVERY)) {
            return AgentRunRetryDecision.refused(
                    AgentRunRetryRefusalReason.EFFECT_REQUIRES_USER_RECOVERY);
        }
        if (completedAttempts >= policy.maxAttempts()) {
            return AgentRunRetryDecision.refused(
                    AgentRunRetryRefusalReason.ATTEMPTS_EXHAUSTED);
        }
        return AgentRunRetryDecision.admitted();
    }

    private static boolean hasEffectWithStatus(
            ExternalEffectLedgerState ledgerState, ExternalEffectStatus status) {
        return ledgerState.records().stream()
                .anyMatch(record -> record.status() == status);
    }
}
