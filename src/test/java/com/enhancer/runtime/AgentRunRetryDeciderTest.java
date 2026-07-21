package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgentRunRetryDeciderTest {

    private static final String GOAL_ID = "00000000-0000-0000-0000-000000001001";
    private static final String AGENT_RUN_ID = "00000000-0000-0000-0000-000000001002";
    private static final String WORK_ITEM_ID = "00000000-0000-0000-0000-000000001003";

    private final AgentRunRetryDecider decider = new AgentRunRetryDecider();

    @Test
    void admitsFailedWithResolvedLedgerAndRemainingBudget() {
        AgentRunRetryDecision decision = decider.decide(
                WorkItemDisposition.FAILED,
                1,
                AgentRunRetryPolicy.of(3),
                ledgerWith(ExternalEffectStatus.APPLIED));
        assertTrue(decision.isAdmitted());
    }

    @Test
    void admitsFailedWithEmptyLedgerAndRemainingBudget() {
        AgentRunRetryDecision decision = decider.decide(
                WorkItemDisposition.FAILED, 1, AgentRunRetryPolicy.of(3), emptyLedger());
        assertTrue(decision.isAdmitted());
    }

    @Test
    void refusesNonFailedDisposition() {
        AgentRunRetryDecision decision = decider.decide(
                WorkItemDisposition.VERIFIED_COMPLETED,
                1,
                AgentRunRetryPolicy.of(3),
                emptyLedger());
        assertEquals(
                Optional.of(AgentRunRetryRefusalReason.NOT_FAILED),
                decision.refusalReason());
    }

    @Test
    void refusesWhenAnEffectIsPrepared() {
        AgentRunRetryDecision decision = decider.decide(
                WorkItemDisposition.FAILED,
                1,
                AgentRunRetryPolicy.of(3),
                ledgerWith(ExternalEffectStatus.PREPARED));
        assertEquals(
                Optional.of(AgentRunRetryRefusalReason.UNRESOLVED_EXTERNAL_EFFECT),
                decision.refusalReason());
    }

    @Test
    void refusesWhenAnEffectRequiresUserRecovery() {
        AgentRunRetryDecision decision = decider.decide(
                WorkItemDisposition.FAILED,
                1,
                AgentRunRetryPolicy.of(3),
                ledgerWith(ExternalEffectStatus.REQUIRES_USER_RECOVERY));
        assertEquals(
                Optional.of(AgentRunRetryRefusalReason.EFFECT_REQUIRES_USER_RECOVERY),
                decision.refusalReason());
    }

    @Test
    void refusesWhenAttemptsExhausted() {
        AgentRunRetryDecision decision = decider.decide(
                WorkItemDisposition.FAILED, 3, AgentRunRetryPolicy.of(3), emptyLedger());
        assertEquals(
                Optional.of(AgentRunRetryRefusalReason.ATTEMPTS_EXHAUSTED),
                decision.refusalReason());
    }

    @Test
    void preparedEffectBeatsExhaustedBudget() {
        AgentRunRetryDecision decision = decider.decide(
                WorkItemDisposition.FAILED,
                3,
                AgentRunRetryPolicy.of(3),
                ledgerWith(ExternalEffectStatus.PREPARED));
        assertEquals(
                Optional.of(AgentRunRetryRefusalReason.UNRESOLVED_EXTERNAL_EFFECT),
                decision.refusalReason());
    }

    @Test
    void preparedEffectBeatsUserRecovery() {
        AgentRunRetryDecision decision = decider.decide(
                WorkItemDisposition.FAILED,
                1,
                AgentRunRetryPolicy.of(3),
                ledgerWithPreparedAndRecovery());
        assertEquals(
                Optional.of(AgentRunRetryRefusalReason.UNRESOLVED_EXTERNAL_EFFECT),
                decision.refusalReason());
    }

    @Test
    void rejectsCompletedAttemptsBelowOne() {
        assertThrows(
                IllegalArgumentException.class,
                () -> decider.decide(
                        WorkItemDisposition.FAILED, 0, AgentRunRetryPolicy.of(3),
                        emptyLedger()));
    }

    @Test
    void rejectsCompletedAttemptsAboveMaximum() {
        assertThrows(
                IllegalArgumentException.class,
                () -> decider.decide(
                        WorkItemDisposition.FAILED,
                        AgentRunRetryPolicy.MAX_ATTEMPTS + 1,
                        AgentRunRetryPolicy.of(3),
                        emptyLedger()));
    }

    @Test
    void rejectsNullDisposition() {
        assertThrows(
                NullPointerException.class,
                () -> decider.decide(null, 1, AgentRunRetryPolicy.of(3), emptyLedger()));
    }

    // --- Ledger states built only through real validated transitions. ---

    private static ExternalEffectLedgerState emptyLedger() {
        return ExternalEffectLedgerState.initial(GOAL_ID);
    }

    private static ExternalEffectLedgerState ledgerWith(ExternalEffectStatus status) {
        ExternalEffectLedgerState prepared =
                ExternalEffectLedgerState.initial(GOAL_ID).prepare(request("effect-1"));
        if (status == ExternalEffectStatus.PREPARED) {
            return prepared;
        }
        return prepared.recordOutcome("effect-1", status);
    }

    private static ExternalEffectLedgerState ledgerWithPreparedAndRecovery() {
        return ExternalEffectLedgerState.initial(GOAL_ID)
                .prepare(request("effect-1"))
                .prepare(request("effect-2"))
                .recordOutcome("effect-2", ExternalEffectStatus.REQUIRES_USER_RECOVERY);
    }

    private static ExternalEffectRequest request(String idempotencyKey) {
        return new ExternalEffectRequest(
                idempotencyKey,
                GOAL_ID,
                AGENT_RUN_ID,
                WORK_ITEM_ID,
                "publish",
                "c".repeat(64));
    }
}
