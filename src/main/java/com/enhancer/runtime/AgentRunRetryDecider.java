package com.enhancer.runtime;

import java.util.Objects;

/**
 * Pure, fail-closed decision on whether a further AgentRun may be admitted after one failed
 * attempt. It creates, persists, and runs nothing, mutates no store, and grants no authority.
 * Automatic retry is safe only when the exact Goal ledger is empty or every effect is
 * compensated; every other effect state blocks before the attempt-budget decision.
 */
public final class AgentRunRetryDecider {

    public AgentRunRetryDecision decide(
            RuntimeAgentRun lastAttempt,
            int completedAttempts,
            AgentRunRetryPolicy policy,
            ExternalEffectLedgerState ledgerState) {
        Objects.requireNonNull(lastAttempt, "lastAttempt must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        Objects.requireNonNull(ledgerState, "ledgerState must not be null");
        if (completedAttempts < 1 || completedAttempts > AgentRunRetryPolicy.MAX_ATTEMPTS) {
            throw new IllegalArgumentException(
                    "completedAttempts must be between 1 and "
                            + AgentRunRetryPolicy.MAX_ATTEMPTS);
        }
        requireBoundLedger(lastAttempt, ledgerState);

        if (lastAttempt.status() != RuntimeAgentRunStatus.FAILED) {
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
        if (hasEffectWithStatus(ledgerState, ExternalEffectStatus.APPLIED)
                || hasEffectWithStatus(ledgerState, ExternalEffectStatus.DEDUPLICATED)) {
            return AgentRunRetryDecision.refused(
                    AgentRunRetryRefusalReason.NON_COMPENSATED_EXTERNAL_EFFECT);
        }
        if (completedAttempts >= policy.maxAttempts()) {
            return AgentRunRetryDecision.refused(
                    AgentRunRetryRefusalReason.ATTEMPTS_EXHAUSTED);
        }
        return AgentRunRetryDecision.admitted();
    }

    private static void requireBoundLedger(
            RuntimeAgentRun lastAttempt,
            ExternalEffectLedgerState ledgerState) {
        if (!lastAttempt.goalId().equals(ledgerState.goalId())) {
            throw new IllegalArgumentException(
                    "external effect ledger Goal does not match the AgentRun attempt");
        }
        for (ExternalEffectRecord record : ledgerState.records()) {
            ExternalEffectRequest request = record.request();
            if (!lastAttempt.goalId().equals(request.goalId())) {
                throw new IllegalArgumentException(
                        "external effect Goal does not match the AgentRun attempt");
            }
            if (!lastAttempt.workItemId().equals(request.workItemId())) {
                throw new IllegalArgumentException(
                        "external effect WorkItem does not match the AgentRun attempt");
            }
        }
    }

    private static boolean hasEffectWithStatus(
            ExternalEffectLedgerState ledgerState, ExternalEffectStatus status) {
        return ledgerState.records().stream()
                .anyMatch(record -> record.status() == status);
    }
}
