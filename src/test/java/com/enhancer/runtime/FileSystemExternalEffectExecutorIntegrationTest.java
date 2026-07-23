package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.EvidenceStore;
import com.enhancer.tool.FileSystemEvidenceStore;
import com.enhancer.tool.ResolvedEvidence;
import com.enhancer.tool.StoredEvidence;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemExternalEffectExecutorIntegrationTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000001301";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000001302";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000001303";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000001304";
    private static final String ADAPTER_ID = "deterministic-publisher";
    private static final String OPERATION_SHA256 = "c".repeat(64);

    @TempDir
    Path temporaryRoot;

    @Test
    void persistsPreparedBeforeInvocationAndReplaysBoundTerminalEvidence()
            throws Exception {
        Fixture fixture = fixture("success", false, false);
        ExternalEffectRequest request = request("effect-success");
        DeterministicAdapter adapter = new DeterministicAdapter(
                ADAPTER_ID,
                OPERATION_SHA256,
                new ExternalEffectAdapterResult(
                        ExternalEffectStatus.APPLIED,
                        "remote receipt 42"),
                () -> {
                    ExternalEffectLedgerState visible = fixture.effectStore()
                            .resolve(GOAL_ID);
                    assertEquals(1, visible.revision());
                    assertEquals(
                            ExternalEffectStatus.PREPARED,
                            visible.records().get(0).status());
                    assertTrue(visible.records().get(0).outcomeEvidence().isEmpty());
                });

        ExternalEffectExecutionResult executed = fixture.executor().execute(
                request,
                adapter,
                fixture.lease().ownerId(),
                fixture.lease().fenceToken());

        assertTrue(executed.adapterInvoked());
        assertEquals(1, adapter.invocations());
        assertEquals(ExternalEffectStatus.APPLIED, executed.record().status());
        assertEquals("remote receipt 42", executed.evidence().content());
        ExternalEffectOutcomeEvidence binding = executed.record()
                .outcomeEvidence()
                .orElseThrow();
        assertEquals(executed.evidence().metadata().reference(), binding.reference());
        assertEquals(executed.evidence().metadata().sha256(), binding.sha256());
        assertEquals(2, fixture.ledger().revision());

        DurableExternalEffectLedger recoveredLedger =
                DurableExternalEffectLedger.recover(
                        GOAL_ID,
                        new FileSystemAgentRuntimeStateStore(fixture.runtimeRoot()),
                        new FileSystemExternalEffectLedgerStore(fixture.effectRoot()),
                        fixture.clock());
        DurableExternalEffectExecutor recoveredExecutor =
                new DurableExternalEffectExecutor(
                        recoveredLedger,
                        new FileSystemEvidenceStore(
                                fixture.evidenceRoot(),
                                new EvidenceStoragePolicy(4096)));

        ExternalEffectExecutionResult replayed = recoveredExecutor.execute(
                request,
                adapter,
                "expired-or-different-owner-is-irrelevant-for-read-only-replay",
                fixture.lease().fenceToken() + 100);

        assertFalse(replayed.adapterInvoked());
        assertEquals(1, adapter.invocations());
        assertEquals(executed.record(), replayed.record());
        assertEquals(executed.evidence(), replayed.evidence());
        assertEquals(2, recoveredLedger.revision());
    }

    @Test
    void rejectsAdapterIdentityAndDigestBeforePreparation() throws Exception {
        Fixture fixture = fixture("identity", false, false);
        ExternalEffectRequest request = request("effect-identity");

        assertThrows(IllegalArgumentException.class, () ->
                fixture.executor().execute(
                        request,
                        adapter("different-adapter", OPERATION_SHA256),
                        fixture.lease().ownerId(),
                        fixture.lease().fenceToken()));
        assertThrows(IllegalArgumentException.class, () ->
                fixture.executor().execute(
                        request,
                        adapter(ADAPTER_ID, "d".repeat(64)),
                        fixture.lease().ownerId(),
                        fixture.lease().fenceToken()));

        assertEquals(0, fixture.ledger().revision());
        assertTrue(fixture.ledger().records().isEmpty());
    }

    @Test
    void refusesAutomaticExecutionFromAPreexistingPreparedRecord()
            throws Exception {
        Fixture fixture = fixture("prepared", false, false);
        ExternalEffectRequest request = request("effect-prepared");
        fixture.ledger().prepare(
                request,
                fixture.lease().ownerId(),
                fixture.lease().fenceToken());
        DeterministicAdapter adapter = adapter(ADAPTER_ID, OPERATION_SHA256);

        assertThrows(PreparedExternalEffectRequiresRecoveryException.class, () ->
                fixture.executor().execute(
                        request,
                        adapter,
                        fixture.lease().ownerId(),
                        fixture.lease().fenceToken()));

        assertEquals(0, adapter.invocations());
        assertEquals(1, fixture.ledger().revision());
        assertEquals(
                ExternalEffectStatus.PREPARED,
                fixture.ledger().records().get(0).status());
    }

    @Test
    void leavesPreparedAcrossAdapterEvidenceTerminalAndLeaseFailures()
            throws Exception {
        Fixture adapterFailure = fixture("adapter-failure", false, false);
        DeterministicAdapter throwingAdapter = adapter(ADAPTER_ID, OPERATION_SHA256);
        throwingAdapter.failInvocation();
        assertThrows(ExternalEffectAdapterException.class, () ->
                adapterFailure.executor().execute(
                        request("effect-adapter-failure"),
                        throwingAdapter,
                        adapterFailure.lease().ownerId(),
                        adapterFailure.lease().fenceToken()));
        assertPrepared(adapterFailure);

        Fixture evidenceFailure = fixture("evidence-failure", true, false);
        assertThrows(IOException.class, () ->
                evidenceFailure.executor().execute(
                        request("effect-evidence-failure"),
                        adapter(ADAPTER_ID, OPERATION_SHA256),
                        evidenceFailure.lease().ownerId(),
                        evidenceFailure.lease().fenceToken()));
        assertPrepared(evidenceFailure);

        Fixture terminalFailure = fixture("terminal-failure", false, true);
        assertThrows(IOException.class, () ->
                terminalFailure.executor().execute(
                        request("effect-terminal-failure"),
                        adapter(ADAPTER_ID, OPERATION_SHA256),
                        terminalFailure.lease().ownerId(),
                        terminalFailure.lease().fenceToken()));
        assertPrepared(terminalFailure);

        Fixture expiredLease = fixture("expired-lease", false, false);
        DeterministicAdapter expiringAdapter = new DeterministicAdapter(
                ADAPTER_ID,
                OPERATION_SHA256,
                new ExternalEffectAdapterResult(
                        ExternalEffectStatus.APPLIED,
                        "remote may have applied"),
                () -> expiredLease.clock().advance(Duration.ofMinutes(6)));
        assertThrows(IllegalStateException.class, () ->
                expiredLease.executor().execute(
                        request("effect-expired-lease"),
                        expiringAdapter,
                        expiredLease.lease().ownerId(),
                        expiredLease.lease().fenceToken()));
        assertPrepared(expiredLease);
    }

    private Fixture fixture(
            String name,
            boolean failEvidence,
            boolean failTerminalUpdate) throws Exception {
        Path runtimeRoot = temporaryRoot.resolve(name).resolve("runtime");
        Path effectRoot = temporaryRoot.resolve(name).resolve("effects");
        Path evidenceRoot = temporaryRoot.resolve(name).resolve("evidence");
        MutableClock clock = new MutableClock(
                Instant.parse("2026-07-23T08:00:00Z"));
        FileSystemAgentRuntimeStateStore runtimeStore =
                new FileSystemAgentRuntimeStateStore(runtimeRoot);
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), runtimeStore, clock);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        AgentRunLease lease = runtime.acquireLease(
                AGENT_RUN_ID, "effect-executor", Duration.ofMinutes(5));
        ExternalEffectLedgerStore baseEffectStore =
                new FileSystemExternalEffectLedgerStore(effectRoot);
        ExternalEffectLedgerStore effectStore = failTerminalUpdate
                ? new FailingTerminalEffectStore(baseEffectStore)
                : baseEffectStore;
        DurableExternalEffectLedger ledger = DurableExternalEffectLedger.create(
                GOAL_ID, runtimeStore, effectStore, clock);
        EvidenceStore baseEvidenceStore = new FileSystemEvidenceStore(
                evidenceRoot, new EvidenceStoragePolicy(4096));
        EvidenceStore evidenceStore = failEvidence
                ? new FailingEvidenceStore(baseEvidenceStore)
                : baseEvidenceStore;
        return new Fixture(
                runtimeRoot,
                effectRoot,
                evidenceRoot,
                clock,
                lease,
                effectStore,
                ledger,
                new DurableExternalEffectExecutor(ledger, evidenceStore));
    }

    private static void assertPrepared(Fixture fixture) throws IOException {
        ExternalEffectLedgerState durable = fixture.effectStore().resolve(GOAL_ID);
        assertEquals(1, durable.revision());
        assertEquals(1, durable.records().size());
        assertEquals(ExternalEffectStatus.PREPARED, durable.records().get(0).status());
        assertTrue(durable.records().get(0).outcomeEvidence().isEmpty());
    }

    private static ExternalEffectRequest request(String idempotencyKey) {
        return new ExternalEffectRequest(
                idempotencyKey,
                GOAL_ID,
                AGENT_RUN_ID,
                WORK_ITEM_ID,
                ADAPTER_ID,
                "publish-artifact",
                OPERATION_SHA256);
    }

    private static DeterministicAdapter adapter(
            String adapterId,
            String operationSha256) {
        return new DeterministicAdapter(
                adapterId,
                operationSha256,
                new ExternalEffectAdapterResult(
                        ExternalEffectStatus.APPLIED,
                        "deterministic receipt"),
                () -> { });
    }

    private static WorkItem workItem() {
        return new WorkItem(
                WORK_ITEM_ID,
                "external-effect-executor",
                new MessageEnvelope(
                        WORK_MESSAGE_ID,
                        "correlation-effect-executor",
                        Optional.empty(),
                        "logical-run-effect-executor",
                        "effect-executor-test",
                        Instant.parse("2026-07-23T07:00:00Z"),
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "implement-evidence-bound-external-effect-executor",
                                        "CURRENT_TASK.md",
                                        "a".repeat(64)),
                                "b".repeat(64),
                                Set.of("read-file"))));
    }

    private record Fixture(
            Path runtimeRoot,
            Path effectRoot,
            Path evidenceRoot,
            MutableClock clock,
            AgentRunLease lease,
            ExternalEffectLedgerStore effectStore,
            DurableExternalEffectLedger ledger,
            DurableExternalEffectExecutor executor) {
    }

    private static final class DeterministicAdapter
            implements ExternalEffectAdapter {
        private final String adapterId;
        private final String operationSha256;
        private final ExternalEffectAdapterResult result;
        private final CheckedAction beforeResult;
        private int invocations;
        private boolean failInvocation;

        private DeterministicAdapter(
                String adapterId,
                String operationSha256,
                ExternalEffectAdapterResult result,
                CheckedAction beforeResult) {
            this.adapterId = adapterId;
            this.operationSha256 = operationSha256;
            this.result = result;
            this.beforeResult = beforeResult;
        }

        @Override
        public String adapterId() {
            return adapterId;
        }

        @Override
        public String operationSha256() {
            return operationSha256;
        }

        @Override
        public ExternalEffectAdapterResult invoke(String idempotencyKey)
                throws ExternalEffectAdapterException {
            invocations++;
            if (failInvocation) {
                throw new ExternalEffectAdapterException(
                        "simulated adapter failure");
            }
            try {
                beforeResult.run();
            } catch (Exception exception) {
                throw new ExternalEffectAdapterException(
                        "adapter assertion failed", exception);
            }
            return result;
        }

        int invocations() {
            return invocations;
        }

        void failInvocation() {
            failInvocation = true;
        }
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    private static final class FailingEvidenceStore implements EvidenceStore {
        private final EvidenceStore delegate;

        private FailingEvidenceStore(EvidenceStore delegate) {
            this.delegate = delegate;
        }

        @Override
        public String createRun() throws IOException {
            throw new IOException("simulated evidence allocation failure");
        }

        @Override
        public StoredEvidence persist(String runId, String content)
                throws IOException {
            return delegate.persist(runId, content);
        }

        @Override
        public ResolvedEvidence resolve(String reference) throws IOException {
            return delegate.resolve(reference);
        }

        @Override
        public EvidenceStoragePolicy storagePolicy() {
            return delegate.storagePolicy();
        }
    }

    private static final class FailingTerminalEffectStore
            implements ExternalEffectLedgerStore {
        private final ExternalEffectLedgerStore delegate;

        private FailingTerminalEffectStore(ExternalEffectLedgerStore delegate) {
            this.delegate = delegate;
        }

        @Override
        public void create(ExternalEffectLedgerState initialState)
                throws IOException {
            delegate.create(initialState);
        }

        @Override
        public void update(ExternalEffectLedgerState nextState)
                throws IOException {
            boolean hasTerminal = nextState.records().stream()
                    .anyMatch(record -> record.status().isTerminal());
            if (hasTerminal) {
                throw new IOException("simulated terminal publication failure");
            }
            delegate.update(nextState);
        }

        @Override
        public ExternalEffectLedgerState resolve(String goalId)
                throws IOException {
            return delegate.resolve(goalId);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
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
            return current;
        }

        void advance(Duration duration) {
            current = current.plus(duration);
        }
    }
}
