package com.enhancer.maintenance.installation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InstallationTransactionReconcilerTest {
    private static final String EVIDENCE_SHA256 = "e".repeat(64);
    private static final String PERMISSION_POLICY_SHA256 = "f".repeat(64);

    @Test
    void exactPointEvidenceReconcilesPendingStateWithOneSucceededCas() throws Exception {
        InstallationTransactionState pending = initialState();
        InMemoryStore store = new InMemoryStore(pending);
        RecordingResolver resolver = new RecordingResolver();
        resolver.evidence = Optional.of(evidence(pending));

        InstallationTransactionReconciler.Outcome outcome =
                new InstallationTransactionReconciler(store, resolver)
                        .reconcile(pending.plan().transactionId());

        assertEquals(InstallationTransactionReconciler.OutcomeStatus.PHASE_RECONCILED,
                outcome.status());
        assertEquals(InstallationTransactionState.StepStatus.SUCCEEDED,
                outcome.state().stepStatus());
        assertEquals(1, store.mutations);
        assertEquals(1, resolver.calls);
        assertEquals(InstallationPhaseEvidencePoint.fromPending(pending), resolver.lastPoint);
    }

    @Test
    void missingEvidencePointLeavesPendingStateUnchanged() throws Exception {
        InstallationTransactionState pending = initialState();
        InMemoryStore store = new InMemoryStore(pending);
        RecordingResolver resolver = new RecordingResolver();

        InstallationTransactionReconciler.Outcome outcome =
                new InstallationTransactionReconciler(store, resolver)
                        .reconcile(pending.plan().transactionId());

        assertEquals(InstallationTransactionReconciler.OutcomeStatus.EVIDENCE_ABSENT,
                outcome.status());
        assertEquals(pending, outcome.state());
        assertEquals(0, store.mutations);
        assertEquals(1, resolver.calls);
    }

    @Test
    void foreignEvidenceFailsBeforeStoreMutation() {
        InstallationTransactionState pending = initialState();
        InMemoryStore store = new InMemoryStore(pending);
        RecordingResolver resolver = new RecordingResolver();
        resolver.evidence = Optional.of(new InstallationPhaseEvidence(
                InstallationPhaseEvidence.SCHEMA_VERSION,
                UUID.randomUUID(),
                pending.phase(),
                pending.revision(),
                EVIDENCE_SHA256,
                Optional.empty()));

        InstallationPhaseEvidenceResolutionException failure = assertThrows(
                InstallationPhaseEvidenceResolutionException.class,
                () -> new InstallationTransactionReconciler(store, resolver)
                        .reconcile(pending.plan().transactionId()));

        assertEquals(InstallationPhaseEvidenceResolutionException.Reason.FOREIGN_EVIDENCE,
                failure.reason());
        assertEquals(0, store.mutations);
        assertEquals(pending, store.state(pending.plan().transactionId()));
    }

    @Test
    void resolverFailureLeavesPendingStateUnchanged() {
        InstallationTransactionState pending = initialState();
        InMemoryStore store = new InMemoryStore(pending);
        RecordingResolver resolver = new RecordingResolver();
        resolver.failure = new InstallationPhaseEvidenceResolutionException(
                InstallationPhaseEvidenceResolutionException.Reason.RESOLVER_UNAVAILABLE,
                pending.phase(), "fake resolver unavailable");

        InstallationPhaseEvidenceResolutionException failure = assertThrows(
                InstallationPhaseEvidenceResolutionException.class,
                () -> new InstallationTransactionReconciler(store, resolver)
                        .reconcile(pending.plan().transactionId()));

        assertEquals(InstallationPhaseEvidenceResolutionException.Reason.RESOLVER_UNAVAILABLE,
                failure.reason());
        assertEquals(0, store.mutations);
        assertEquals(pending, store.state(pending.plan().transactionId()));
    }

    @Test
    void succeededStateRequiresNoEvidenceResolutionOrAutomaticAdvance() throws Exception {
        InstallationTransactionState succeeded = initialState().markSucceeded(
                evidence(initialState()));
        InMemoryStore store = new InMemoryStore(succeeded);
        RecordingResolver resolver = new RecordingResolver();

        InstallationTransactionReconciler.Outcome outcome =
                new InstallationTransactionReconciler(store, resolver)
                        .reconcile(succeeded.plan().transactionId());

        assertEquals(InstallationTransactionReconciler.OutcomeStatus.NO_RECONCILIATION_REQUIRED,
                outcome.status());
        assertEquals(succeeded, outcome.state());
        assertEquals(0, resolver.calls);
        assertEquals(0, store.mutations);
    }

    @Test
    void exactSucceededCasReplayIsClassifiedWithoutDuplicateMutation() throws Exception {
        InstallationTransactionState pending = initialState();
        InMemoryStore store = new InMemoryStore(pending);
        store.replaySucceededCas = true;
        RecordingResolver resolver = new RecordingResolver();
        resolver.evidence = Optional.of(evidence(pending));

        InstallationTransactionReconciler.Outcome outcome =
                new InstallationTransactionReconciler(store, resolver)
                        .reconcile(pending.plan().transactionId());

        assertEquals(
                InstallationTransactionReconciler.OutcomeStatus.EXACT_RECONCILIATION_REPLAY,
                outcome.status());
        assertEquals(InstallationTransactionState.StepStatus.SUCCEEDED,
                outcome.state().stepStatus());
        assertEquals(1, store.mutations);
        assertEquals(1, resolver.calls);
    }

    @Test
    void finalPendingPhaseReconcilesTerminalAndTerminalReplayDoesNothing() throws Exception {
        InstallationTransactionState pending = pendingAt(
                InstallationPhase.RECORD_FINAL_EVIDENCE);
        InMemoryStore store = new InMemoryStore(pending);
        RecordingResolver resolver = new RecordingResolver();
        resolver.evidence = Optional.of(evidence(pending));
        InstallationTransactionReconciler reconciler =
                new InstallationTransactionReconciler(store, resolver);

        InstallationTransactionReconciler.Outcome reconciled = reconciler.reconcile(
                pending.plan().transactionId());
        InstallationTransactionReconciler.Outcome replay = reconciler.reconcile(
                pending.plan().transactionId());

        assertEquals(InstallationTransactionReconciler.OutcomeStatus.TERMINAL_RECONCILED,
                reconciled.status());
        assertEquals(InstallationTransactionReconciler.OutcomeStatus.NO_RECONCILIATION_REQUIRED,
                replay.status());
        assertTrue(replay.state().isTerminalRecord());
        assertEquals(1, resolver.calls);
        assertEquals(1, store.mutations);
    }

    @Test
    void malformedStoreReceiptFailsClosed() {
        InstallationTransactionState pending = initialState();
        InMemoryStore store = new InMemoryStore(pending);
        store.malformedReceipt = true;
        RecordingResolver resolver = new RecordingResolver();
        resolver.evidence = Optional.of(evidence(pending));

        InstallationTransactionStoreException failure = assertThrows(
                InstallationTransactionStoreException.class,
                () -> new InstallationTransactionReconciler(store, resolver)
                        .reconcile(pending.plan().transactionId()));

        assertEquals(InstallationTransactionStoreException.Reason.CORRUPT_STATE,
                failure.reason());
        assertEquals(pending, store.state(pending.plan().transactionId()));
        assertEquals(0, store.mutations);
    }

    @Test
    void unexpectedResolverFailureBecomesBoundedUnavailableFailure() {
        InstallationTransactionState pending = initialState();
        InMemoryStore store = new InMemoryStore(pending);
        RecordingResolver resolver = new RecordingResolver();
        resolver.failUnexpectedly = true;

        InstallationPhaseEvidenceResolutionException failure = assertThrows(
                InstallationPhaseEvidenceResolutionException.class,
                () -> new InstallationTransactionReconciler(store, resolver)
                        .reconcile(pending.plan().transactionId()));

        assertEquals(InstallationPhaseEvidenceResolutionException.Reason.RESOLVER_UNAVAILABLE,
                failure.reason());
        assertEquals(pending.phase(), failure.phase());
        assertEquals(0, store.mutations);
    }

    @Test
    void pointRejectsNonCanonicalPendingRevision() {
        InstallationTransactionState pending = initialState();

        assertThrows(IllegalArgumentException.class, () -> new InstallationPhaseEvidencePoint(
                pending.plan().transactionId(), pending.phase(), pending.revision() + 1));
    }

    private static InstallationTransactionState initialState() {
        CancellationTrustInstallationPlan plan = CancellationTrustInstallationPlanTest.validPlan();
        InstallationEnvironmentEvidence environment = new InstallationEnvironmentEvidence(
                plan.transactionId(), "fake-adapter", "fake-v1", plan.principals(),
                "fake-filesystem", true, true);
        return InstallationTransactionState.start(
                plan, environment, "release-v1", PERMISSION_POLICY_SHA256,
                Optional.of("activation-old"), "activation-new");
    }

    private static InstallationTransactionState pendingAt(InstallationPhase target) {
        InstallationTransactionState state = initialState();
        while (state.phase() != target) {
            state = state.markSucceeded(evidence(state)).beginNext();
        }
        return state;
    }

    private static InstallationPhaseEvidence evidence(InstallationTransactionState pending) {
        Optional<String> activation = pending.phase() == InstallationPhase.ACTIVATE
                ? Optional.of(pending.requestedActivationIdentity())
                : Optional.empty();
        return new InstallationPhaseEvidence(
                InstallationPhaseEvidence.SCHEMA_VERSION,
                pending.plan().transactionId(),
                pending.phase(),
                pending.revision(),
                EVIDENCE_SHA256,
                activation);
    }

    private static final class RecordingResolver implements InstallationPhaseEvidenceResolver {
        private Optional<InstallationPhaseEvidence> evidence = Optional.empty();
        private InstallationPhaseEvidenceResolutionException failure;
        private InstallationPhaseEvidencePoint lastPoint;
        private int calls;
        private boolean failUnexpectedly;

        @Override
        public Optional<InstallationPhaseEvidence> resolveAndRevalidate(
                InstallationPhaseEvidencePoint point)
                throws InstallationPhaseEvidenceResolutionException {
            calls++;
            lastPoint = point;
            if (failure != null) {
                throw failure;
            }
            if (failUnexpectedly) {
                throw new IllegalStateException("fake unexpected resolver failure");
            }
            return evidence;
        }
    }

    private static final class InMemoryStore implements InstallationTransactionStore {
        private final Map<UUID, InstallationTransactionState> states = new HashMap<>();
        private int mutations;
        private boolean replaySucceededCas;
        private boolean malformedReceipt;

        private InMemoryStore(InstallationTransactionState state) {
            states.put(state.plan().transactionId(), state);
        }

        @Override
        public Mutation create(InstallationTransactionState initial)
                throws InstallationTransactionStoreException {
            throw failure(InstallationTransactionStoreException.Reason.TRANSACTION_CONFLICT);
        }

        @Override
        public InstallationTransactionState resolve(UUID transactionId)
                throws InstallationTransactionStoreException {
            InstallationTransactionState state = states.get(transactionId);
            if (state == null) {
                throw failure(InstallationTransactionStoreException.Reason.NOT_FOUND);
            }
            return state;
        }

        @Override
        public Mutation compareAndExchange(
                UUID transactionId,
                long expectedRevision,
                InstallationTransactionState replacement)
                throws InstallationTransactionStoreException {
            InstallationTransactionState current = resolve(transactionId);
            if (current.revision() != expectedRevision) {
                throw failure(InstallationTransactionStoreException.Reason.REVISION_CONFLICT);
            }
            if (!current.isImmediateSuccessor(replacement)) {
                throw failure(InstallationTransactionStoreException.Reason.INVALID_TRANSITION);
            }
            if (malformedReceipt) {
                return new Mutation(current, MutationDisposition.ADVANCED);
            }
            states.put(transactionId, replacement);
            mutations++;
            return new Mutation(replacement, replaySucceededCas
                    ? MutationDisposition.EXACT_REPLAY
                    : MutationDisposition.ADVANCED);
        }

        private InstallationTransactionState state(UUID transactionId) {
            return states.get(transactionId);
        }

        private static InstallationTransactionStoreException failure(
                InstallationTransactionStoreException.Reason reason) {
            return new InstallationTransactionStoreException(reason, "fake store refusal");
        }
    }
}
