package com.enhancer.maintenance.installation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class InstallationTransactionCoordinatorTest {
    private static final String EVIDENCE_SHA256 = "e".repeat(64);
    private static final String PERMISSION_POLICY_SHA256 = "f".repeat(64);

    @Test
    void startPersistsPendingBeforeOnePreflightCallAndRecordsSuccess() throws Exception {
        InMemoryStore store = new InMemoryStore();
        RecordingPorts ports = new RecordingPorts(store);
        InstallationTransactionCoordinator coordinator = coordinator(store, ports);
        InstallationTransactionState initial = initialState();

        InstallationTransactionCoordinator.Outcome outcome = coordinator.start(initial);

        assertEquals(InstallationTransactionCoordinator.OutcomeStatus.PHASE_RECORDED,
                outcome.status());
        assertEquals(InstallationPhase.RESOLVE_PRINCIPALS, outcome.state().phase());
        assertEquals(InstallationTransactionState.StepStatus.SUCCEEDED,
                outcome.state().stepStatus());
        assertEquals(List.of(EVIDENCE_SHA256), outcome.state().succeededPhaseEvidencePrefix()
                .stream().map(InstallationPhaseEvidence::semanticEvidenceSha256).toList());
        assertEquals(List.of(InstallationPhase.RESOLVE_PRINCIPALS), ports.calls);
        assertEquals(List.of(InstallationTransactionState.StepStatus.PENDING),
                ports.observedStatuses);
        assertEquals(2, store.mutations);
    }

    @Test
    void explicitCallsRouteAllElevenPhasesInRequiredOrderAndTerminalReplayIsIdle()
            throws Exception {
        InMemoryStore store = new InMemoryStore();
        RecordingPorts ports = new RecordingPorts(store);
        InstallationTransactionCoordinator coordinator = coordinator(store, ports);
        InstallationTransactionCoordinator.Outcome outcome = coordinator.start(initialState());

        while (!outcome.state().isTerminalRecord()) {
            outcome = coordinator.advance(outcome.state().plan().transactionId());
        }

        assertEquals(InstallationTransactionCoordinator.OutcomeStatus.TERMINAL_RECORDED,
                outcome.status());
        assertEquals(InstallationPhase.requiredOrder(), ports.calls);
        assertEquals(List.of(
                "preflight:RESOLVE_PRINCIPALS",
                "preflight:VERIFY_SOURCE_AND_TOPOLOGY",
                "effect:STAGE_PRIVATE_ARTIFACTS",
                "effect:APPLY_AND_VERIFY_STAGED_PERMISSIONS",
                "effect:PREPARE_TRUST_DIRECTORY_AND_LOCK",
                "effect:PUBLISH_POLICY",
                "effect:PUBLISH_METADATA",
                "effect:VERIFY_FINAL_BYTES_AND_PERMISSIONS",
                "effect:PROBE_AS_RUNTIME",
                "activation:ACTIVATE",
                "effect:RECORD_FINAL_EVIDENCE"), ports.routes);
        assertEquals(21, outcome.state().revision());
        assertEquals(InstallationPhase.requiredOrder(), outcome.state()
                .succeededPhaseEvidencePrefix().stream()
                .map(InstallationPhaseEvidence::phase).toList());
        int mutations = store.mutations;
        int calls = ports.calls.size();

        InstallationTransactionCoordinator.Outcome replay = coordinator.advance(
                outcome.state().plan().transactionId());

        assertEquals(InstallationTransactionCoordinator.OutcomeStatus.EXACT_TERMINAL_REPLAY,
                replay.status());
        assertEquals(mutations, store.mutations);
        assertEquals(calls, ports.calls.size());
    }

    @Test
    void existingOrExactReplayedPendingRequiresReconciliationWithoutPortCall()
            throws Exception {
        InstallationTransactionState initial = initialState();
        InMemoryStore existingStore = new InMemoryStore();
        existingStore.create(initial);
        RecordingPorts existingPorts = new RecordingPorts(existingStore);

        InstallationTransactionCoordinator.Outcome existing = coordinator(
                existingStore, existingPorts).start(initial);

        assertEquals(InstallationTransactionCoordinator.OutcomeStatus.REQUIRES_RECONCILIATION,
                existing.status());
        assertEquals(List.of(), existingPorts.calls);

        InMemoryStore replayStore = new InMemoryStore();
        InstallationTransactionState firstSucceeded = succeed(initial);
        replayStore.seed(firstSucceeded);
        replayStore.replayNextAdvance = true;
        RecordingPorts replayPorts = new RecordingPorts(replayStore);

        InstallationTransactionCoordinator.Outcome replay = coordinator(
                replayStore, replayPorts).advance(initial.plan().transactionId());

        assertEquals(InstallationTransactionCoordinator.OutcomeStatus.REQUIRES_RECONCILIATION,
                replay.status());
        assertEquals(List.of(), replayPorts.calls);
        assertEquals(InstallationTransactionState.StepStatus.PENDING,
                replayStore.resolve(initial.plan().transactionId()).stepStatus());
    }

    @Test
    void everyPhasePortFailureLeavesThatPendingPhaseAndStops() throws Exception {
        for (InstallationPhase phase : InstallationPhase.requiredOrder()) {
            InMemoryStore store = new InMemoryStore();
            RecordingPorts ports = new RecordingPorts(store);
            ports.failPhase = phase;
            InstallationTransactionCoordinator coordinator = coordinator(store, ports);

            if (phase == InstallationPhase.RESOLVE_PRINCIPALS) {
                assertThrows(InstallationPhaseExecutionException.class,
                        () -> coordinator.start(initialState()));
            } else {
                InstallationTransactionState previous = succeededBefore(phase);
                store.seed(previous);
                assertThrows(InstallationPhaseExecutionException.class,
                        () -> coordinator.advance(previous.plan().transactionId()));
            }

            InstallationTransactionState retained = store.resolve(
                    initialState().plan().transactionId());
            assertEquals(phase, retained.phase());
            assertEquals(InstallationTransactionState.StepStatus.PENDING,
                    retained.stepStatus());
            assertEquals(List.of(phase), ports.calls);
        }
    }

    @Test
    void mismatchedPhaseOrActivationResultFailsBeforeSucceededCas() throws Exception {
        InMemoryStore phaseStore = new InMemoryStore();
        RecordingPorts phasePorts = new RecordingPorts(phaseStore);
        phasePorts.resultOverride = state -> result(
                state.plan().transactionId(), InstallationPhase.ACTIVATE, Optional.empty());

        InstallationPhaseExecutionException phaseFailure = assertThrows(
                InstallationPhaseExecutionException.class,
                () -> coordinator(phaseStore, phasePorts).start(initialState()));
        assertEquals(InstallationPhaseExecutionException.Reason.RESULT_BINDING_INVALID,
                phaseFailure.reason());
        assertEquals(InstallationTransactionState.StepStatus.PENDING,
                phaseStore.resolve(initialState().plan().transactionId()).stepStatus());

        InMemoryStore activationStore = new InMemoryStore();
        InstallationTransactionState beforeActivation = succeededBefore(
                InstallationPhase.ACTIVATE);
        activationStore.seed(beforeActivation);
        RecordingPorts activationPorts = new RecordingPorts(activationStore);
        activationPorts.resultOverride = state -> result(
                state.plan().transactionId(), state.phase(), Optional.of("wrong-activation"));

        InstallationPhaseExecutionException activationFailure = assertThrows(
                InstallationPhaseExecutionException.class,
                () -> coordinator(activationStore, activationPorts).advance(
                        beforeActivation.plan().transactionId()));
        assertEquals(InstallationPhaseExecutionException.Reason.RESULT_BINDING_INVALID,
                activationFailure.reason());
        assertEquals(InstallationTransactionState.StepStatus.PENDING,
                activationStore.resolve(beforeActivation.plan().transactionId()).stepStatus());
    }

    @Test
    void storeFailureBeforePortCallsNothingAndFailureAfterPortLeavesPending() throws Exception {
        InstallationTransactionState initial = initialState();
        InMemoryStore beforeStore = new InMemoryStore();
        InstallationTransactionState firstSucceeded = succeed(initial);
        beforeStore.seed(firstSucceeded);
        beforeStore.failNextAdvance = true;
        RecordingPorts beforePorts = new RecordingPorts(beforeStore);

        assertThrows(InstallationTransactionStoreException.class,
                () -> coordinator(beforeStore, beforePorts).advance(
                        initial.plan().transactionId()));
        assertEquals(List.of(), beforePorts.calls);
        assertEquals(firstSucceeded, beforeStore.resolve(initial.plan().transactionId()));

        InMemoryStore afterStore = new InMemoryStore();
        afterStore.failSucceededAdvance = true;
        RecordingPorts afterPorts = new RecordingPorts(afterStore);

        assertThrows(InstallationTransactionStoreException.class,
                () -> coordinator(afterStore, afterPorts).start(initial));
        assertEquals(List.of(InstallationPhase.RESOLVE_PRINCIPALS), afterPorts.calls);
        assertEquals(InstallationTransactionState.StepStatus.PENDING,
                afterStore.resolve(initial.plan().transactionId()).stepStatus());
    }

    private static InstallationTransactionCoordinator coordinator(
            InMemoryStore store, RecordingPorts ports) {
        return new InstallationTransactionCoordinator(store, ports, ports, ports);
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

    private static InstallationTransactionState succeededBefore(InstallationPhase target) {
        InstallationTransactionState state = succeed(initialState());
        while (state.beginNext().phase() != target) {
            state = succeed(state.beginNext());
        }
        return state;
    }

    private static InstallationPhaseEvidence result(
            UUID transactionId, InstallationPhase phase, Optional<String> activationIdentity) {
        long pendingRevision = (long) InstallationPhase.requiredOrder().indexOf(phase) * 2;
        return new InstallationPhaseEvidence(
                InstallationPhaseEvidence.SCHEMA_VERSION,
                transactionId, phase, pendingRevision, EVIDENCE_SHA256, activationIdentity);
    }

    private static InstallationTransactionState succeed(InstallationTransactionState pending) {
        Optional<String> activation = pending.phase() == InstallationPhase.ACTIVATE
                ? Optional.of(pending.requestedActivationIdentity())
                : Optional.empty();
        return pending.markSucceeded(result(
                pending.plan().transactionId(), pending.phase(), activation));
    }

    private static final class RecordingPorts implements
            InstallationTransactionCoordinator.PreflightVerifier,
            InstallationTransactionCoordinator.PhaseEffectPort,
            InstallationTransactionCoordinator.ActivationPort {
        private final InMemoryStore store;
        private final List<InstallationPhase> calls = new ArrayList<>();
        private final List<String> routes = new ArrayList<>();
        private final List<InstallationTransactionState.StepStatus> observedStatuses =
                new ArrayList<>();
        private InstallationPhase failPhase;
        private Function<InstallationTransactionState,
                InstallationPhaseEvidence> resultOverride;

        private RecordingPorts(InMemoryStore store) {
            this.store = store;
        }

        @Override
        public InstallationPhaseEvidence verifyPrincipalsAndEnvironment(
                InstallationTransactionState pending)
                throws InstallationPhaseExecutionException {
            return call("preflight", pending);
        }

        @Override
        public InstallationPhaseEvidence verifySourceAndTopology(
                InstallationTransactionState pending)
                throws InstallationPhaseExecutionException {
            return call("preflight", pending);
        }

        @Override
        public InstallationPhaseEvidence stagePrivateArtifacts(
                InstallationTransactionState pending)
                throws InstallationPhaseExecutionException {
            return call("effect", pending);
        }

        @Override
        public InstallationPhaseEvidence applyAndVerifyStagedPermissions(
                InstallationTransactionState pending)
                throws InstallationPhaseExecutionException {
            return call("effect", pending);
        }

        @Override
        public InstallationPhaseEvidence prepareTrustDirectoryAndLock(
                InstallationTransactionState pending)
                throws InstallationPhaseExecutionException {
            return call("effect", pending);
        }

        @Override
        public InstallationPhaseEvidence publishPolicy(
                InstallationTransactionState pending)
                throws InstallationPhaseExecutionException {
            return call("effect", pending);
        }

        @Override
        public InstallationPhaseEvidence publishMetadata(
                InstallationTransactionState pending)
                throws InstallationPhaseExecutionException {
            return call("effect", pending);
        }

        @Override
        public InstallationPhaseEvidence verifyFinalBytesAndPermissions(
                InstallationTransactionState pending)
                throws InstallationPhaseExecutionException {
            return call("effect", pending);
        }

        @Override
        public InstallationPhaseEvidence probeAsRuntime(
                InstallationTransactionState pending)
                throws InstallationPhaseExecutionException {
            return call("effect", pending);
        }

        @Override
        public InstallationPhaseEvidence recordFinalEvidence(
                InstallationTransactionState pending)
                throws InstallationPhaseExecutionException {
            return call("effect", pending);
        }

        @Override
        public InstallationPhaseEvidence activate(
                InstallationTransactionState pending)
                throws InstallationPhaseExecutionException {
            return call("activation", pending);
        }

        private InstallationPhaseEvidence call(
                String route, InstallationTransactionState pending)
                throws InstallationPhaseExecutionException {
            InstallationTransactionState persisted = store.peek(
                    pending.plan().transactionId());
            calls.add(pending.phase());
            routes.add(route + ":" + pending.phase());
            observedStatuses.add(persisted.stepStatus());
            assertEquals(pending, persisted);
            if (pending.phase() == failPhase) {
                throw new InstallationPhaseExecutionException(
                        InstallationPhaseExecutionException.Reason.PORT_REFUSED,
                        pending.phase(), "fake port refusal");
            }
            if (resultOverride != null) {
                return resultOverride.apply(pending);
            }
            Optional<String> activation = pending.phase() == InstallationPhase.ACTIVATE
                    ? Optional.of(pending.requestedActivationIdentity())
                    : Optional.empty();
            return result(pending.plan().transactionId(), pending.phase(), activation);
        }
    }

    private static final class InMemoryStore implements InstallationTransactionStore {
        private final Map<UUID, InstallationTransactionState> states = new HashMap<>();
        private int mutations;
        private boolean replayNextAdvance;
        private boolean failNextAdvance;
        private boolean failSucceededAdvance;

        @Override
        public Mutation create(InstallationTransactionState initial)
                throws InstallationTransactionStoreException {
            InstallationTransactionState current = states.get(initial.plan().transactionId());
            if (current == null) {
                states.put(initial.plan().transactionId(), initial);
                mutations++;
                return new Mutation(initial, MutationDisposition.CREATED);
            }
            if (current.equals(initial)) {
                return new Mutation(current, MutationDisposition.EXACT_REPLAY);
            }
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
            if (current.equals(replacement)) {
                return new Mutation(current, MutationDisposition.EXACT_REPLAY);
            }
            if (current.revision() != expectedRevision) {
                throw failure(InstallationTransactionStoreException.Reason.REVISION_CONFLICT);
            }
            if (!current.isImmediateSuccessor(replacement)) {
                throw failure(InstallationTransactionStoreException.Reason.INVALID_TRANSITION);
            }
            if (failNextAdvance
                    || (failSucceededAdvance
                    && replacement.stepStatus()
                    == InstallationTransactionState.StepStatus.SUCCEEDED)) {
                failNextAdvance = false;
                throw failure(InstallationTransactionStoreException.Reason.STORE_UNAVAILABLE);
            }
            states.put(transactionId, replacement);
            mutations++;
            if (replayNextAdvance) {
                replayNextAdvance = false;
                return new Mutation(replacement, MutationDisposition.EXACT_REPLAY);
            }
            return new Mutation(replacement, MutationDisposition.ADVANCED);
        }

        private void seed(InstallationTransactionState state) {
            states.put(state.plan().transactionId(), state);
        }

        private InstallationTransactionState peek(UUID transactionId) {
            return states.get(transactionId);
        }

        private static InstallationTransactionStoreException failure(
                InstallationTransactionStoreException.Reason reason) {
            return new InstallationTransactionStoreException(reason, "fake store refusal");
        }
    }
}
