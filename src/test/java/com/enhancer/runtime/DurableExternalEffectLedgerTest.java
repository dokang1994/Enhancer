package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DurableExternalEffectLedgerTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000001001";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000001002";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000001003";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000001004";
    private static final String EFFECT_KEY = "publish-artifact-1";
    private static final ExternalEffectOutcomeEvidence OUTCOME_EVIDENCE =
            new ExternalEffectOutcomeEvidence(
                    "evidence/00000000-0000-0000-0000-000000001010/"
                            + "00000000-0000-0000-0000-000000001011",
                    "e".repeat(64));

    @Test
    void preparesAndTerminatesAnEffectWithExactIdempotentReplay()
            throws Exception {
        Fixture fixture = fixture();
        ExternalEffectRequest request = request(EFFECT_KEY, "publish-🚀");

        ExternalEffectRecord prepared = fixture.ledger().prepare(
                request, fixture.lease().ownerId(), fixture.lease().fenceToken());
        ExternalEffectRecord replayedPrepare = fixture.ledger().prepare(
                request, fixture.lease().ownerId(), fixture.lease().fenceToken());

        assertEquals(ExternalEffectStatus.PREPARED, prepared.status());
        assertEquals(prepared, replayedPrepare);
        assertEquals(1, fixture.ledger().revision());

        ExternalEffectRecord applied = fixture.ledger().recordOutcome(
                EFFECT_KEY,
                ExternalEffectStatus.APPLIED,
                OUTCOME_EVIDENCE,
                fixture.lease().ownerId(),
                fixture.lease().fenceToken());
        ExternalEffectRecord replayedOutcome = fixture.ledger().recordOutcome(
                EFFECT_KEY,
                ExternalEffectStatus.APPLIED,
                OUTCOME_EVIDENCE,
                fixture.lease().ownerId(),
                fixture.lease().fenceToken());

        assertEquals(ExternalEffectStatus.APPLIED, applied.status());
        assertEquals(applied, replayedOutcome);
        assertEquals(2, fixture.ledger().revision());
        assertEquals(List.of(applied), fixture.ledger().records());
        assertThrows(IllegalStateException.class, () ->
                fixture.ledger().recordOutcome(
                        EFFECT_KEY,
                        ExternalEffectStatus.APPLIED,
                        new ExternalEffectOutcomeEvidence(
                                OUTCOME_EVIDENCE.reference(), "f".repeat(64)),
                        fixture.lease().ownerId(),
                        fixture.lease().fenceToken()));
        assertThrows(IllegalStateException.class, () ->
                fixture.ledger().recordOutcome(
                        EFFECT_KEY,
                        ExternalEffectStatus.COMPENSATED,
                        OUTCOME_EVIDENCE,
                        fixture.lease().ownerId(),
                        fixture.lease().fenceToken()));
        assertThrows(IllegalArgumentException.class, () ->
                fixture.ledger().recordOutcome(
                        EFFECT_KEY,
                        ExternalEffectStatus.PREPARED,
                        OUTCOME_EVIDENCE,
                        fixture.lease().ownerId(),
                        fixture.lease().fenceToken()));
    }

    @Test
    void supportsEveryExplicitTerminalOutcome() throws Exception {
        for (ExternalEffectStatus outcome : List.of(
                ExternalEffectStatus.APPLIED,
                ExternalEffectStatus.DEDUPLICATED,
                ExternalEffectStatus.COMPENSATED,
                ExternalEffectStatus.REQUIRES_USER_RECOVERY)) {
            Fixture fixture = fixture();
            fixture.ledger().prepare(
                    request(EFFECT_KEY, "publish"),
                    fixture.lease().ownerId(),
                    fixture.lease().fenceToken());

            assertEquals(
                    outcome,
                    fixture.ledger().recordOutcome(
                            EFFECT_KEY,
                            outcome,
                            OUTCOME_EVIDENCE,
                            fixture.lease().ownerId(),
                            fixture.lease().fenceToken()).status());
        }
    }

    @Test
    void rejectsStaleExpiredAndMismatchedRuntimeAuthority()
            throws Exception {
        Fixture fixture = fixture();
        ExternalEffectRequest request = request(EFFECT_KEY, "publish");

        assertThrows(IllegalArgumentException.class, () ->
                fixture.ledger().prepare(
                        request, "another-owner", fixture.lease().fenceToken()));
        assertThrows(IllegalArgumentException.class, () ->
                fixture.ledger().prepare(
                        request,
                        fixture.lease().ownerId(),
                        fixture.lease().fenceToken() + 1));
        assertThrows(IllegalArgumentException.class, () ->
                fixture.ledger().prepare(
                        new ExternalEffectRequest(
                                "wrong-goal",
                                "00000000-0000-0000-0000-000000001099",
                                AGENT_RUN_ID,
                                WORK_ITEM_ID,
                                "test-adapter",
                                "publish",
                                "c".repeat(64)),
                        fixture.lease().ownerId(),
                        fixture.lease().fenceToken()));
        assertThrows(IllegalArgumentException.class, () ->
                fixture.ledger().prepare(
                        new ExternalEffectRequest(
                                "wrong-run",
                                GOAL_ID,
                                "00000000-0000-0000-0000-000000001098",
                                WORK_ITEM_ID,
                                "test-adapter",
                                "publish",
                                "c".repeat(64)),
                        fixture.lease().ownerId(),
                        fixture.lease().fenceToken()));
        assertThrows(IllegalArgumentException.class, () ->
                fixture.ledger().prepare(
                        new ExternalEffectRequest(
                                "wrong-work",
                                GOAL_ID,
                                AGENT_RUN_ID,
                                "00000000-0000-0000-0000-000000001097",
                                "test-adapter",
                                "publish",
                                "c".repeat(64)),
                        fixture.lease().ownerId(),
                        fixture.lease().fenceToken()));

        fixture.clock().advance(Duration.ofMinutes(5));
        assertThrows(IllegalStateException.class, () ->
                fixture.ledger().prepare(
                        request,
                        fixture.lease().ownerId(),
                        fixture.lease().fenceToken()));
        assertEquals(0, fixture.ledger().revision());
    }

    @Test
    void boundsEffectIdentityAndSemanticMetadata() {
        assertThrows(IllegalArgumentException.class, () ->
                request(" ", "publish"));
        assertThrows(IllegalArgumentException.class, () ->
                request(
                        "k".repeat(ExternalEffectRequest.MAX_KEY_CHARACTERS + 1),
                        "publish"));
        assertThrows(IllegalArgumentException.class, () ->
                request(
                        "bounded-key",
                        "o".repeat(
                                ExternalEffectRequest.MAX_OPERATION_CHARACTERS + 1)));
        assertThrows(IllegalArgumentException.class, () ->
                new ExternalEffectRequest(
                        "bounded-key",
                        GOAL_ID,
                        AGENT_RUN_ID,
                        WORK_ITEM_ID,
                        "test-adapter",
                        "publish",
                        "not-a-digest"));
    }

    @Test
    void rejectsIdempotencyKeyReuseAndOverCapacity() throws Exception {
        Fixture fixture = fixture();
        fixture.ledger().prepare(
                request(EFFECT_KEY, "publish"),
                fixture.lease().ownerId(),
                fixture.lease().fenceToken());

        assertThrows(IllegalArgumentException.class, () ->
                fixture.ledger().prepare(
                        new ExternalEffectRequest(
                                EFFECT_KEY,
                                GOAL_ID,
                                AGENT_RUN_ID,
                                WORK_ITEM_ID,
                                "test-adapter",
                                "different-operation",
                                "d".repeat(64)),
                        fixture.lease().ownerId(),
                        fixture.lease().fenceToken()));

        for (int index = 1; index < ExternalEffectLedgerState.MAX_EFFECTS; index++) {
            fixture.ledger().prepare(
                    request("effect-" + index, "operation-" + index),
                    fixture.lease().ownerId(),
                    fixture.lease().fenceToken());
        }
        assertEquals(
                ExternalEffectLedgerState.MAX_EFFECTS,
                fixture.ledger().records().size());
        assertThrows(IllegalStateException.class, () ->
                fixture.ledger().prepare(
                        request("effect-over-capacity", "publish"),
                        fixture.lease().ownerId(),
                        fixture.lease().fenceToken()));
    }

    @Test
    void persistenceFailureLeavesPreviousLedgerRevisionVisible()
            throws Exception {
        Fixture fixture = fixture();
        fixture.effectStore().failNextUpdate();

        assertThrows(IOException.class, () ->
                fixture.ledger().prepare(
                        request(EFFECT_KEY, "publish"),
                        fixture.lease().ownerId(),
                        fixture.lease().fenceToken()));
        assertEquals(0, fixture.ledger().revision());
        assertEquals(List.of(), fixture.ledger().records());
        assertEquals(0, fixture.effectStore().resolve(GOAL_ID).revision());
    }

    private static Fixture fixture() throws Exception {
        MutableClock clock = new MutableClock(
                Instant.parse("2026-07-21T09:00:00Z"));
        MemoryAgentRuntimeStateStore runtimeStore =
                new MemoryAgentRuntimeStateStore();
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), runtimeStore, clock);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        AgentRunLease lease = runtime.acquireLease(
                AGENT_RUN_ID, "effect-worker", Duration.ofMinutes(5));
        MemoryExternalEffectLedgerStore effectStore =
                new MemoryExternalEffectLedgerStore();
        DurableExternalEffectLedger ledger =
                DurableExternalEffectLedger.create(
                        GOAL_ID, runtimeStore, effectStore, clock);
        return new Fixture(clock, lease, effectStore, ledger);
    }

    private static ExternalEffectRequest request(
            String key,
            String operationName) {
        return new ExternalEffectRequest(
                key,
                GOAL_ID,
                AGENT_RUN_ID,
                WORK_ITEM_ID,
                "test-adapter",
                operationName,
                "c".repeat(64));
    }

    private static WorkItem workItem() {
        return new WorkItem(
                WORK_ITEM_ID,
                "effect-worker",
                new MessageEnvelope(
                        WORK_MESSAGE_ID,
                        "correlation-effects",
                        Optional.empty(),
                        "logical-run-effects",
                        "effect-test",
                        Instant.parse("2026-07-21T08:00:00Z"),
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "persist-fence-checked-external-effect-ledger",
                                        "CURRENT_TASK.md",
                                        "a".repeat(64)),
                                "b".repeat(64),
                                Set.of("read-file"))));
    }

    private record Fixture(
            MutableClock clock,
            AgentRunLease lease,
            MemoryExternalEffectLedgerStore effectStore,
            DurableExternalEffectLedger ledger) {
    }

    private static final class MemoryExternalEffectLedgerStore
            implements ExternalEffectLedgerStore {
        private ExternalEffectLedgerState state;
        private boolean failNextUpdate;

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
        public ExternalEffectLedgerState resolve(String goalId)
                throws IOException {
            if (state == null || !state.goalId().equals(goalId)) {
                throw new MissingExternalEffectLedgerException(goalId);
            }
            return state;
        }

        void failNextUpdate() {
            failNextUpdate = true;
        }
    }

    private static final class MemoryAgentRuntimeStateStore
            implements AgentRuntimeStateStore {
        private AgentRuntimeState state;

        @Override
        public void create(AgentRuntimeState initialState) throws IOException {
            if (state != null) {
                throw new IOException("runtime already exists");
            }
            state = initialState;
        }

        @Override
        public void update(AgentRuntimeState nextState) throws IOException {
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
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (!getZone().equals(zone)) {
                throw new IllegalArgumentException("UTC only");
            }
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
