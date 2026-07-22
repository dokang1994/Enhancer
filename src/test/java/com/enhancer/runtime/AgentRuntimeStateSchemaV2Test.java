package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ResultPayload;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AgentRuntimeStateSchemaV2Test {
    private static final String GOAL_ID = "00000000-0000-0000-0000-000000002001";
    private static final String FIRST_RUN_ID = "00000000-0000-0000-0000-000000002002";
    private static final String SECOND_RUN_ID = "00000000-0000-0000-0000-000000002003";
    private static final String WORK_ITEM_ID = "00000000-0000-0000-0000-000000002004";
    private static final String WORK_MESSAGE_ID = "00000000-0000-0000-0000-000000002005";

    @Test
    void failedAttemptBecomesRetryPendingWithoutLosingItsExactHistory() {
        AgentRuntimeState pending = failedFirstAttempt();

        assertEquals(2, pending.schemaVersion());
        assertEquals(RuntimeGoalStatus.RETRY_PENDING, pending.goal().status());
        assertEquals(1, pending.completedAttempts());
        assertEquals(1, pending.agentRuns().size());
        assertEquals(FIRST_RUN_ID, pending.agentRun().orElseThrow().agentRunId());
        assertEquals(RuntimeAgentRunStatus.FAILED,
                pending.agentRun().orElseThrow().status());
        assertEquals(result(FIRST_RUN_ID, VerificationStatus.REJECTED),
                pending.agentRun().orElseThrow().resultMessage().orElseThrow());
        assertTrue(pending.retryDecisions().isEmpty());
    }

    @Test
    void admittedDecisionPersistsBeforeAppendingAReplacementAttempt() {
        AgentRuntimeState pending = failedFirstAttempt();
        AgentRunRetryDecisionRecord admitted = decision(
                FIRST_RUN_ID, AgentRunRetryDecision.admitted());

        AgentRuntimeState decided = pending.recordRetryDecision(admitted)
                .orElseThrow();
        Optional<AgentRuntimeState> replay = decided.recordRetryDecision(admitted);
        AgentRuntimeState retried = decided.beginRetryAgentRun(SECOND_RUN_ID);

        assertTrue(replay.isEmpty());
        assertEquals(List.of(admitted), retried.retryDecisions());
        assertEquals(2, retried.agentRuns().size());
        assertEquals(pending.agentRuns(), retried.agentRuns().subList(0, 1));
        assertEquals(SECOND_RUN_ID, retried.agentRun().orElseThrow().agentRunId());
        assertEquals(RuntimeAgentRunStatus.PLANNING,
                retried.agentRun().orElseThrow().status());
        assertEquals(RuntimeGoalStatus.ACTIVE, retried.goal().status());
        assertEquals(1, retried.completedAttempts());
        assertEquals(pending.lastIssuedFenceToken(), retried.lastIssuedFenceToken());
    }

    @Test
    void refusedDecisionIsRequiredBeforeTerminalAbandonment() {
        AgentRuntimeState pending = failedFirstAttempt();
        assertThrows(IllegalStateException.class, pending::abandonGoal);

        AgentRunRetryDecisionRecord refused = decision(
                FIRST_RUN_ID,
                AgentRunRetryDecision.refused(
                        AgentRunRetryRefusalReason.ATTEMPTS_EXHAUSTED));
        AgentRuntimeState abandoned = pending.recordRetryDecision(refused)
                .orElseThrow()
                .abandonGoal();

        assertEquals(RuntimeGoalStatus.FAILED, abandoned.goal().status());
        assertEquals(pending.agentRuns(), abandoned.agentRuns());
        assertEquals(List.of(refused), abandoned.retryDecisions());
    }

    @Test
    void changedDecisionForTheSameAttemptFailsClosed() {
        AgentRuntimeState decided = failedFirstAttempt()
                .recordRetryDecision(decision(
                        FIRST_RUN_ID, AgentRunRetryDecision.admitted()))
                .orElseThrow();

        assertThrows(IllegalArgumentException.class, () ->
                decided.recordRetryDecision(decision(
                        FIRST_RUN_ID,
                        AgentRunRetryDecision.refused(
                                AgentRunRetryRefusalReason.ATTEMPTS_EXHAUSTED))));
    }

    @Test
    void retryDecisionRecordValidatesBoundedTypedInputs() {
        assertThrows(IllegalArgumentException.class, () ->
                new AgentRunRetryDecisionRecord(
                        FIRST_RUN_ID,
                        0,
                        3,
                        0,
                        0,
                        "a".repeat(64),
                        AgentRunRetryDecision.admitted()));
        assertThrows(IllegalArgumentException.class, () ->
                new AgentRunRetryDecisionRecord(
                        FIRST_RUN_ID,
                        1,
                        3,
                        0,
                        0,
                        "not-a-digest",
                        AgentRunRetryDecision.admitted()));
    }

    private static AgentRuntimeState failedFirstAttempt() {
        AgentRuntimeState state = AgentRuntimeState.initial(GOAL_ID, workItem());
        state = state.beginAgentRun(FIRST_RUN_ID);
        state = state.markReady(FIRST_RUN_ID);
        state = state.acquireLease(
                FIRST_RUN_ID,
                "schema-v2-owner",
                Instant.parse("2026-07-22T03:00:00Z"),
                Duration.ofMinutes(5));
        state = state.completeExecution(
                FIRST_RUN_ID,
                "schema-v2-owner",
                1,
                Instant.parse("2026-07-22T03:01:00Z"));
        return state.recordAttemptResult(
                FIRST_RUN_ID,
                result(FIRST_RUN_ID, VerificationStatus.REJECTED));
    }

    private static AgentRunRetryDecisionRecord decision(
            String agentRunId,
            AgentRunRetryDecision decision) {
        return new AgentRunRetryDecisionRecord(
                agentRunId,
                1,
                3,
                0,
                0,
                "c".repeat(64),
                decision);
    }

    private static WorkItem workItem() {
        return new WorkItem(
                WORK_ITEM_ID,
                "schema-v2-runtime",
                new MessageEnvelope(
                        WORK_MESSAGE_ID,
                        "correlation-schema-v2",
                        Optional.empty(),
                        "logical-run-schema-v2",
                        "schema-v2-test",
                        Instant.parse("2026-07-22T02:55:00Z"),
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "add-schema-v2-history-and-park-retry-pending",
                                        "CURRENT_TASK.md",
                                        "a".repeat(64)),
                                "b".repeat(64),
                                Set.of("read-file"))));
    }

    private static MessageEnvelope result(
            String agentRunId,
            VerificationStatus status) {
        String messageId = agentRunId.equals(FIRST_RUN_ID)
                ? "00000000-0000-0000-0000-000000002006"
                : "00000000-0000-0000-0000-000000002007";
        return new MessageEnvelope(
                messageId,
                "correlation-schema-v2",
                Optional.of(WORK_MESSAGE_ID),
                "logical-run-schema-v2",
                "schema-v2-result",
                Instant.parse("2026-07-22T03:02:00Z"),
                new ResultPayload(
                        "add-schema-v2-history-and-park-retry-pending",
                        "run-record/" + agentRunId,
                        status));
    }
}
