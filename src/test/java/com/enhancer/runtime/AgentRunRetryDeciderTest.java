package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ResultPayload;
import com.enhancer.kernel.VerificationStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AgentRunRetryDeciderTest {

    private static final String GOAL_ID = "00000000-0000-0000-0000-000000001001";
    private static final String AGENT_RUN_ID = "00000000-0000-0000-0000-000000001002";
    private static final String WORK_ITEM_ID = "00000000-0000-0000-0000-000000001003";
    private static final String OTHER_GOAL_ID = "00000000-0000-0000-0000-000000001004";
    private static final String OTHER_WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000001005";

    private final AgentRunRetryDecider decider = new AgentRunRetryDecider();

    @Test
    void admitsFailedAttemptWithEmptyLedgerAndRemainingBudget() {
        AgentRunRetryDecision decision = decider.decide(
                attempt(RuntimeAgentRunStatus.FAILED),
                1,
                AgentRunRetryPolicy.of(3),
                emptyLedger());

        assertTrue(decision.isAdmitted());
    }

    @Test
    void admitsFailedAttemptWhenEveryEffectIsCompensated() {
        AgentRunRetryDecision decision = decider.decide(
                attempt(RuntimeAgentRunStatus.FAILED),
                1,
                AgentRunRetryPolicy.of(3),
                ledgerWith(ExternalEffectStatus.COMPENSATED));

        assertTrue(decision.isAdmitted());
    }

    @ParameterizedTest
    @EnumSource(value = RuntimeAgentRunStatus.class, names = "FAILED", mode = EnumSource.Mode.EXCLUDE)
    void refusesEveryAttemptStatusExceptFailed(RuntimeAgentRunStatus status) {
        AgentRunRetryDecision decision = decider.decide(
                attempt(status),
                1,
                AgentRunRetryPolicy.of(3),
                emptyLedger());

        assertEquals(
                Optional.of(AgentRunRetryRefusalReason.NOT_FAILED),
                decision.refusalReason());
    }

    @Test
    void refusesPreparedEffect() {
        assertReason(
                ledgerWith(ExternalEffectStatus.PREPARED),
                AgentRunRetryRefusalReason.UNRESOLVED_EXTERNAL_EFFECT);
    }

    @Test
    void refusesEffectRequiringUserRecovery() {
        assertReason(
                ledgerWith(ExternalEffectStatus.REQUIRES_USER_RECOVERY),
                AgentRunRetryRefusalReason.EFFECT_REQUIRES_USER_RECOVERY);
    }

    @ParameterizedTest
    @EnumSource(value = ExternalEffectStatus.class, names = {"APPLIED", "DEDUPLICATED"})
    void refusesKnownButNonCompensatedEffect(ExternalEffectStatus status) {
        assertReason(
                ledgerWith(status),
                AgentRunRetryRefusalReason.NON_COMPENSATED_EXTERNAL_EFFECT);
    }

    @Test
    void refusesWhenAttemptsAreExhausted() {
        AgentRunRetryDecision decision = decider.decide(
                attempt(RuntimeAgentRunStatus.FAILED),
                3,
                AgentRunRetryPolicy.of(3),
                emptyLedger());

        assertEquals(
                Optional.of(AgentRunRetryRefusalReason.ATTEMPTS_EXHAUSTED),
                decision.refusalReason());
    }

    @Test
    void preparedEffectPrecedesRecoveryAppliedAndBudgetReasons() {
        AgentRunRetryDecision decision = decider.decide(
                attempt(RuntimeAgentRunStatus.FAILED),
                3,
                AgentRunRetryPolicy.of(3),
                ledgerWith(
                        ExternalEffectStatus.APPLIED,
                        ExternalEffectStatus.REQUIRES_USER_RECOVERY,
                        ExternalEffectStatus.PREPARED));

        assertEquals(
                Optional.of(AgentRunRetryRefusalReason.UNRESOLVED_EXTERNAL_EFFECT),
                decision.refusalReason());
    }

    @Test
    void userRecoveryPrecedesAppliedAndBudgetReasons() {
        AgentRunRetryDecision decision = decider.decide(
                attempt(RuntimeAgentRunStatus.FAILED),
                3,
                AgentRunRetryPolicy.of(3),
                ledgerWith(
                        ExternalEffectStatus.APPLIED,
                        ExternalEffectStatus.REQUIRES_USER_RECOVERY));

        assertEquals(
                Optional.of(AgentRunRetryRefusalReason.EFFECT_REQUIRES_USER_RECOVERY),
                decision.refusalReason());
    }

    @Test
    void appliedEffectPrecedesExhaustedBudget() {
        AgentRunRetryDecision decision = decider.decide(
                attempt(RuntimeAgentRunStatus.FAILED),
                3,
                AgentRunRetryPolicy.of(3),
                ledgerWith(ExternalEffectStatus.APPLIED));

        assertEquals(
                Optional.of(AgentRunRetryRefusalReason.NON_COMPENSATED_EXTERNAL_EFFECT),
                decision.refusalReason());
    }

    @Test
    void rejectsLedgerForAnotherGoal() {
        assertThrows(
                IllegalArgumentException.class,
                () -> decider.decide(
                        attempt(RuntimeAgentRunStatus.FAILED),
                        1,
                        AgentRunRetryPolicy.of(3),
                        ExternalEffectLedgerState.initial(OTHER_GOAL_ID)));
    }

    @Test
    void rejectsEffectForAnotherWorkItem() {
        ExternalEffectLedgerState mismatched = ExternalEffectLedgerState.initial(GOAL_ID)
                .prepare(request("effect-1", OTHER_WORK_ITEM_ID));

        assertThrows(
                IllegalArgumentException.class,
                () -> decider.decide(
                        attempt(RuntimeAgentRunStatus.FAILED),
                        1,
                        AgentRunRetryPolicy.of(3),
                        mismatched));
    }

    @Test
    void rejectsCompletedAttemptsBelowOne() {
        assertThrows(
                IllegalArgumentException.class,
                () -> decider.decide(
                        attempt(RuntimeAgentRunStatus.FAILED),
                        0,
                        AgentRunRetryPolicy.of(3),
                        emptyLedger()));
    }

    @Test
    void rejectsCompletedAttemptsAboveMaximum() {
        assertThrows(
                IllegalArgumentException.class,
                () -> decider.decide(
                        attempt(RuntimeAgentRunStatus.FAILED),
                        AgentRunRetryPolicy.MAX_ATTEMPTS + 1,
                        AgentRunRetryPolicy.of(3),
                        emptyLedger()));
    }

    @Test
    void rejectsNullAttempt() {
        assertThrows(
                NullPointerException.class,
                () -> decider.decide(null, 1, AgentRunRetryPolicy.of(3), emptyLedger()));
    }

    @Test
    void rejectsNullPolicy() {
        assertThrows(
                NullPointerException.class,
                () -> decider.decide(
                        attempt(RuntimeAgentRunStatus.FAILED), 1, null, emptyLedger()));
    }

    @Test
    void rejectsNullLedger() {
        assertThrows(
                NullPointerException.class,
                () -> decider.decide(
                        attempt(RuntimeAgentRunStatus.FAILED),
                        1,
                        AgentRunRetryPolicy.of(3),
                        null));
    }

    private void assertReason(
            ExternalEffectLedgerState ledger,
            AgentRunRetryRefusalReason expectedReason) {
        AgentRunRetryDecision decision = decider.decide(
                attempt(RuntimeAgentRunStatus.FAILED),
                1,
                AgentRunRetryPolicy.of(3),
                ledger);
        assertEquals(Optional.of(expectedReason), decision.refusalReason());
    }

    private static RuntimeAgentRun attempt(RuntimeAgentRunStatus status) {
        RuntimeAgentRun planning = new RuntimeAgentRun(
                AGENT_RUN_ID,
                GOAL_ID,
                WORK_ITEM_ID,
                RuntimeAgentRunStatus.PLANNING,
                Optional.empty(),
                Optional.empty());
        return switch (status) {
            case PLANNING -> planning;
            case READY -> planning.transition(RuntimeAgentRunStatus.READY);
            case EXECUTING -> planning.executeWith(AgentRunLease.issue(
                    "retry-test-worker",
                    1,
                    Instant.parse("2026-07-22T02:00:00Z"),
                    Duration.ofMinutes(5)));
            case AWAITING_VERIFICATION ->
                    planning.transition(RuntimeAgentRunStatus.AWAITING_VERIFICATION);
            case COMPLETED -> planning.terminate(
                    RuntimeAgentRunStatus.COMPLETED,
                    resultMessage(VerificationStatus.VERIFIED));
            case FAILED -> planning.terminate(
                    RuntimeAgentRunStatus.FAILED,
                    resultMessage(VerificationStatus.REJECTED));
        };
    }

    private static MessageEnvelope resultMessage(VerificationStatus status) {
        return new MessageEnvelope(
                "00000000-0000-0000-0000-000000001006",
                "correlation-retry-decision",
                Optional.empty(),
                "logical-run-retry-decision",
                "retry-decision-test",
                Instant.parse("2026-07-22T02:05:00Z"),
                new ResultPayload(
                        "correct-attempt-level-agentrun-retry-decision",
                        "run-record/retry-decision",
                        status));
    }

    private static ExternalEffectLedgerState emptyLedger() {
        return ExternalEffectLedgerState.initial(GOAL_ID);
    }

    private static ExternalEffectLedgerState ledgerWith(
            ExternalEffectStatus... statuses) {
        ExternalEffectLedgerState state = ExternalEffectLedgerState.initial(GOAL_ID);
        List<ExternalEffectStatus> ordered = List.of(statuses);
        for (int index = 0; index < ordered.size(); index++) {
            String key = "effect-" + index;
            state = state.prepare(request(key, WORK_ITEM_ID));
            ExternalEffectStatus status = ordered.get(index);
            if (status != ExternalEffectStatus.PREPARED) {
                state = state.recordOutcome(
                        key, status, outcomeEvidence(index));
            }
        }
        return state;
    }

    private static ExternalEffectRequest request(
            String idempotencyKey,
            String workItemId) {
        return new ExternalEffectRequest(
                idempotencyKey,
                GOAL_ID,
                AGENT_RUN_ID,
                workItemId,
                "retry-test-adapter",
                "publish",
                "c".repeat(64));
    }

    private static ExternalEffectOutcomeEvidence outcomeEvidence(int index) {
        String suffix = String.format("%012d", index + 1);
        return new ExternalEffectOutcomeEvidence(
                "evidence/00000000-0000-0000-0000-000000001210/"
                        + "00000000-0000-0000-0000-" + suffix,
                "e".repeat(64));
    }
}
