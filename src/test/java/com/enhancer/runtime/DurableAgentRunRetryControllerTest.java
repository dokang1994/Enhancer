package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ResultPayload;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DurableAgentRunRetryControllerTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000003001";
    private static final String FIRST_RUN_ID =
            "00000000-0000-0000-0000-000000003002";
    private static final String SECOND_RUN_ID =
            "00000000-0000-0000-0000-000000003003";
    private static final String OTHER_RUN_ID =
            "00000000-0000-0000-0000-000000003004";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000003005";
    private static final String OTHER_WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000003006";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000003007";

    @Test
    void recordsTheExactAdmittedDecisionBeforeApplyingAnyRetryAction()
            throws Exception {
        Fixture fixture = fixture(ExternalEffectLedgerState.initial(GOAL_ID));
        long before = fixture.runtimeStore().state().revision();

        AgentRunRetryDecisionRecord recorded = fixture.controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3));

        assertTrue(recorded.decision().isAdmitted());
        assertEquals(FIRST_RUN_ID, recorded.agentRunId());
        assertEquals(1, recorded.completedAttempts());
        assertEquals(3, recorded.maxAttempts());
        assertEquals(0, recorded.externalEffectLedgerRevision());
        assertEquals(0, recorded.externalEffectRecordCount());
        assertTrue(recorded.externalEffectLedgerSemanticSha256()
                .matches("[0-9a-f]{64}"));
        assertEquals(before + 1, fixture.runtimeStore().state().revision());
        assertEquals(RuntimeGoalStatus.RETRY_PENDING,
                fixture.runtimeStore().state().goal().status());
        assertEquals(1, fixture.runtimeStore().state().agentRuns().size());

        AgentRunRetryDecisionRecord replayed = fixture.controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3));

        assertEquals(recorded, replayed);
        assertEquals(before + 1, fixture.runtimeStore().state().revision());
    }

    @ParameterizedTest
    @EnumSource(ExternalEffectStatus.class)
    void preservesEveryExternalEffectDecisionOutcome(ExternalEffectStatus status)
            throws Exception {
        ExternalEffectLedgerState ledger = ledgerWith(status);
        AgentRunRetryDecisionRecord recorded = fixture(ledger).controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3));

        Optional<AgentRunRetryRefusalReason> expected = switch (status) {
            case COMPENSATED -> Optional.empty();
            case PREPARED -> Optional.of(
                    AgentRunRetryRefusalReason.UNRESOLVED_EXTERNAL_EFFECT);
            case REQUIRES_USER_RECOVERY -> Optional.of(
                    AgentRunRetryRefusalReason.EFFECT_REQUIRES_USER_RECOVERY);
            case APPLIED, DEDUPLICATED -> Optional.of(
                    AgentRunRetryRefusalReason.NON_COMPENSATED_EXTERNAL_EFFECT);
        };
        assertEquals(expected, recorded.decision().refusalReason());
    }

    @Test
    void recordsAttemptsExhaustedAndRejectsChangedPolicyOnReplay()
            throws Exception {
        Fixture fixture = fixture(ExternalEffectLedgerState.initial(GOAL_ID));
        AgentRunRetryDecisionRecord recorded = fixture.controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(1));

        assertEquals(
                Optional.of(AgentRunRetryRefusalReason.ATTEMPTS_EXHAUSTED),
                recorded.decision().refusalReason());
        long decidedRevision = fixture.runtimeStore().state().revision();
        assertThrows(IllegalArgumentException.class, () -> fixture.controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(2)));
        assertEquals(decidedRevision, fixture.runtimeStore().state().revision());
    }

    @Test
    void failsClosedForMissingOrMismatchedLedgerWithoutWritingRuntime()
            throws Exception {
        MemoryAgentRuntimeStateStore runtimeStore =
                new MemoryAgentRuntimeStateStore(failedFirstAttempt());
        DurableAgentRunRetryController missingController = controller(
                runtimeStore, new MemoryExternalEffectLedgerStore(null));
        long before = runtimeStore.state().revision();

        assertThrows(MissingExternalEffectLedgerException.class, () ->
                missingController.recordDecision(
                        GOAL_ID, AgentRunRetryPolicy.of(3)));
        assertEquals(before, runtimeStore.state().revision());

        ExternalEffectLedgerState wrongWorkItem =
                ExternalEffectLedgerState.initial(GOAL_ID)
                        .prepare(request("effect-1", WORK_ITEM_ID))
                        .recordOutcome(
                                "effect-1",
                                ExternalEffectStatus.COMPENSATED,
                                outcomeEvidence())
                        .prepare(request("effect-2", OTHER_WORK_ITEM_ID));
        DurableAgentRunRetryController mismatchedController = controller(
                runtimeStore,
                new MemoryExternalEffectLedgerStore(wrongWorkItem));
        assertThrows(IllegalArgumentException.class, () ->
                mismatchedController.recordDecision(
                        GOAL_ID, AgentRunRetryPolicy.of(3)));
        assertEquals(before, runtimeStore.state().revision());
    }

    @Test
    void rejectsNonRetryPendingRuntimeInsteadOfInventingANonFailedDecision()
            throws Exception {
        MemoryAgentRuntimeStateStore runtimeStore =
                new MemoryAgentRuntimeStateStore(AgentRuntimeState.initial(
                        GOAL_ID, workItem()));
        DurableAgentRunRetryController controller = controller(
                runtimeStore,
                new MemoryExternalEffectLedgerStore(
                        ExternalEffectLedgerState.initial(GOAL_ID)));

        assertThrows(IllegalStateException.class, () -> controller.recordDecision(
                GOAL_ID, AgentRunRetryPolicy.of(3)));
        assertTrue(runtimeStore.state().retryDecisions().isEmpty());
    }

    @Test
    void semanticDigestIsDeterministicAndSensitiveToOrderedRecordSemantics()
            throws Exception {
        ExternalEffectLedgerState firstOrder = ExternalEffectLedgerState.initial(GOAL_ID)
                .prepare(request("effect-a", WORK_ITEM_ID))
                .recordOutcome(
                        "effect-a", ExternalEffectStatus.COMPENSATED, outcomeEvidence())
                .prepare(request("effect-b", WORK_ITEM_ID))
                .recordOutcome(
                        "effect-b", ExternalEffectStatus.COMPENSATED, outcomeEvidence());
        ExternalEffectLedgerState secondOrder = ExternalEffectLedgerState.initial(GOAL_ID)
                .prepare(request("effect-b", WORK_ITEM_ID))
                .recordOutcome(
                        "effect-b", ExternalEffectStatus.COMPENSATED, outcomeEvidence())
                .prepare(request("effect-a", WORK_ITEM_ID))
                .recordOutcome(
                        "effect-a", ExternalEffectStatus.COMPENSATED, outcomeEvidence());

        String first = fixture(firstOrder).controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3))
                .externalEffectLedgerSemanticSha256();
        String restarted = fixture(firstOrder).controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3))
                .externalEffectLedgerSemanticSha256();
        String reordered = fixture(secondOrder).controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3))
                .externalEffectLedgerSemanticSha256();

        assertEquals(first, restarted);
        assertNotEquals(first, reordered);
    }

    @Test
    void appliesAnAdmittedRetryWithTheCheckpointedIdentifierIdempotently()
            throws Exception {
        Fixture fixture = fixture(ExternalEffectLedgerState.initial(GOAL_ID));
        fixture.controller().recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3));

        fixture.controller().beginAdmittedRetry(GOAL_ID, SECOND_RUN_ID);
        long appliedRevision = fixture.runtimeStore().state().revision();
        fixture.controller().beginAdmittedRetry(GOAL_ID, SECOND_RUN_ID);

        assertEquals(appliedRevision, fixture.runtimeStore().state().revision());
        assertEquals(RuntimeGoalStatus.ACTIVE,
                fixture.runtimeStore().state().goal().status());
        assertEquals(List.of(FIRST_RUN_ID, SECOND_RUN_ID),
                fixture.runtimeStore().state().agentRuns().stream()
                        .map(RuntimeAgentRun::agentRunId)
                        .toList());
        assertThrows(IllegalArgumentException.class, () -> fixture.controller()
                .beginAdmittedRetry(GOAL_ID, OTHER_RUN_ID));
        assertEquals(appliedRevision, fixture.runtimeStore().state().revision());
    }

    @Test
    void appliesARefusedDecisionAsIdempotentTerminalAbandonment()
            throws Exception {
        Fixture fixture = fixture(ExternalEffectLedgerState.initial(GOAL_ID));
        fixture.controller().recordDecision(GOAL_ID, AgentRunRetryPolicy.of(1));

        fixture.controller().abandonRefusedRetry(GOAL_ID);
        long abandonedRevision = fixture.runtimeStore().state().revision();
        fixture.controller().abandonRefusedRetry(GOAL_ID);

        assertEquals(abandonedRevision, fixture.runtimeStore().state().revision());
        assertEquals(RuntimeGoalStatus.FAILED,
                fixture.runtimeStore().state().goal().status());
        assertThrows(IllegalStateException.class, () -> fixture.controller()
                .beginAdmittedRetry(GOAL_ID, SECOND_RUN_ID));
    }

    @Test
    void recoversAfterActionPersistenceFailureWithoutRevisingDecision()
            throws Exception {
        Fixture fixture = fixture(ExternalEffectLedgerState.initial(GOAL_ID));
        AgentRunRetryDecisionRecord decision = fixture.controller()
                .recordDecision(GOAL_ID, AgentRunRetryPolicy.of(3));
        fixture.runtimeStore().failNextUpdate();

        assertThrows(IOException.class, () -> fixture.controller()
                .beginAdmittedRetry(GOAL_ID, SECOND_RUN_ID));
        assertEquals(List.of(decision), fixture.runtimeStore().state().retryDecisions());
        assertEquals(RuntimeGoalStatus.RETRY_PENDING,
                fixture.runtimeStore().state().goal().status());

        DurableAgentRunRetryController restarted = controller(
                fixture.runtimeStore(), fixture.effectStore());
        restarted.beginAdmittedRetry(GOAL_ID, SECOND_RUN_ID);
        assertEquals(RuntimeGoalStatus.ACTIVE,
                fixture.runtimeStore().state().goal().status());
    }

    private static Fixture fixture(ExternalEffectLedgerState ledger) {
        MemoryAgentRuntimeStateStore runtimeStore =
                new MemoryAgentRuntimeStateStore(failedFirstAttempt());
        MemoryExternalEffectLedgerStore effectStore =
                new MemoryExternalEffectLedgerStore(ledger);
        return new Fixture(
                runtimeStore,
                effectStore,
                controller(runtimeStore, effectStore));
    }

    private static DurableAgentRunRetryController controller(
            AgentRuntimeStateStore runtimeStore,
            ExternalEffectLedgerStore effectStore) {
        return new DurableAgentRunRetryController(
                runtimeStore, effectStore, new AgentRunRetryDecider());
    }

    private static AgentRuntimeState failedFirstAttempt() {
        AgentRuntimeState state = AgentRuntimeState.initial(GOAL_ID, workItem());
        state = state.beginAgentRun(FIRST_RUN_ID);
        state = state.markReady(FIRST_RUN_ID);
        state = state.acquireLease(
                FIRST_RUN_ID,
                "retry-controller-worker",
                Instant.parse("2026-07-22T04:00:00Z"),
                Duration.ofMinutes(5));
        state = state.completeExecution(
                FIRST_RUN_ID,
                "retry-controller-worker",
                1,
                Instant.parse("2026-07-22T04:01:00Z"));
        return state.recordAttemptResult(FIRST_RUN_ID, failedResult());
    }

    private static ExternalEffectLedgerState ledgerWith(
            ExternalEffectStatus status) {
        ExternalEffectLedgerState ledger = ExternalEffectLedgerState.initial(GOAL_ID)
                .prepare(request("effect-1", WORK_ITEM_ID));
        return status == ExternalEffectStatus.PREPARED
                ? ledger
                : ledger.recordOutcome(
                        "effect-1", status, outcomeEvidence());
    }

    private static ExternalEffectRequest request(String key, String workItemId) {
        return new ExternalEffectRequest(
                key,
                GOAL_ID,
                FIRST_RUN_ID,
                workItemId,
                "retry-controller-adapter",
                "publish-artifact",
                "c".repeat(64));
    }

    private static ExternalEffectOutcomeEvidence outcomeEvidence() {
        return new ExternalEffectOutcomeEvidence(
                "evidence/00000000-0000-0000-0000-000000001220/"
                        + "00000000-0000-0000-0000-000000001221",
                "e".repeat(64));
    }

    private static WorkItem workItem() {
        return new WorkItem(
                WORK_ITEM_ID,
                "retry-controller",
                new MessageEnvelope(
                        WORK_MESSAGE_ID,
                        "correlation-retry-controller",
                        Optional.empty(),
                        "logical-run-retry-controller",
                        "retry-controller-test",
                        Instant.parse("2026-07-22T03:55:00Z"),
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "add-durable-agentrun-retry-controller",
                                        "CURRENT_TASK.md",
                                        "a".repeat(64)),
                                "b".repeat(64),
                                Set.of("read-file"))));
    }

    private static MessageEnvelope failedResult() {
        return new MessageEnvelope(
                "00000000-0000-0000-0000-000000003008",
                "correlation-retry-controller",
                Optional.of(WORK_MESSAGE_ID),
                "logical-run-retry-controller",
                "retry-controller-result",
                Instant.parse("2026-07-22T04:02:00Z"),
                new ResultPayload(
                        "add-durable-agentrun-retry-controller",
                        "run-record/retry-controller",
                        VerificationStatus.REJECTED));
    }

    private record Fixture(
            MemoryAgentRuntimeStateStore runtimeStore,
            MemoryExternalEffectLedgerStore effectStore,
            DurableAgentRunRetryController controller) {
    }

    private static final class MemoryAgentRuntimeStateStore
            implements AgentRuntimeStateStore {
        private AgentRuntimeState state;
        private boolean failNextUpdate;

        private MemoryAgentRuntimeStateStore(AgentRuntimeState state) {
            this.state = state;
        }

        @Override
        public void create(AgentRuntimeState initialState) throws IOException {
            if (state != null) {
                throw new IOException("runtime already exists");
            }
            state = initialState;
        }

        @Override
        public void update(AgentRuntimeState nextState) throws IOException {
            if (failNextUpdate) {
                failNextUpdate = false;
                throw new IOException("simulated persistence failure");
            }
            if (state == null || nextState.revision() != state.revision() + 1) {
                throw new IOException("revision does not advance by one");
            }
            state = nextState;
        }

        @Override
        public AgentRuntimeState resolve(String goalId) throws IOException {
            if (state == null || !state.goal().goalId().equals(goalId)) {
                throw new MissingAgentRuntimeStateException(goalId);
            }
            return state;
        }

        AgentRuntimeState state() {
            return state;
        }

        void failNextUpdate() {
            failNextUpdate = true;
        }
    }

    private static final class MemoryExternalEffectLedgerStore
            implements ExternalEffectLedgerStore {
        private ExternalEffectLedgerState state;

        private MemoryExternalEffectLedgerStore(ExternalEffectLedgerState state) {
            this.state = state;
        }

        @Override
        public void create(ExternalEffectLedgerState initialState)
                throws IOException {
            if (state != null) {
                throw new IOException("ledger already exists");
            }
            state = initialState;
        }

        @Override
        public void update(ExternalEffectLedgerState nextState)
                throws IOException {
            if (state == null || nextState.revision() != state.revision() + 1) {
                throw new IOException("revision does not advance by one");
            }
            state = nextState;
        }

        @Override
        public ExternalEffectLedgerState resolve(String goalId)
                throws IOException {
            if (state == null) {
                throw new MissingExternalEffectLedgerException(goalId);
            }
            return state;
        }
    }
}
