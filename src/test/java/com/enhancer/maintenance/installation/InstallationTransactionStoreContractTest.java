package com.enhancer.maintenance.installation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InstallationTransactionStoreContractTest {
    private static final String PERMISSION_POLICY_SHA256 = "f".repeat(64);

    @Test
    void stateBindsExactIntentAndAlternatesPendingAndSucceeded() {
        InstallationTransactionState initial = initialState("activation-new");

        assertEquals(2, InstallationTransactionState.SCHEMA_VERSION);
        assertEquals(InstallationTransactionState.SCHEMA_VERSION, initial.schemaVersion());
        assertEquals(InstallationPhase.RESOLVE_PRINCIPALS, initial.phase());
        assertEquals(InstallationTransactionState.StepStatus.PENDING, initial.stepStatus());
        assertEquals(0, initial.revision());
        assertEquals(List.of(), initial.succeededPhaseEvidencePrefix());
        assertTrue(initial.requiresReconciliation());

        InstallationPhaseEvidence firstEvidence = evidence(initial, "a".repeat(64));
        InstallationTransactionState succeeded = initial.markSucceeded(firstEvidence);
        InstallationTransactionState next = succeeded.beginNext();

        assertEquals(1, succeeded.revision());
        assertEquals(List.of(firstEvidence), succeeded.succeededPhaseEvidencePrefix());
        assertEquals(InstallationTransactionState.StepStatus.SUCCEEDED,
                succeeded.stepStatus());
        assertFalse(succeeded.requiresReconciliation());
        assertEquals(2, next.revision());
        assertEquals(InstallationPhase.VERIFY_SOURCE_AND_TOPOLOGY, next.phase());
        assertEquals(InstallationTransactionState.StepStatus.PENDING, next.stepStatus());
        assertEquals(succeeded.succeededPhaseEvidencePrefix(),
                next.succeededPhaseEvidencePrefix());
        assertEquals(initial.plan(), next.plan());
        assertEquals(initial.environment(), next.environment());
        assertEquals(initial.requestedActivationIdentity(),
                next.requestedActivationIdentity());
        assertThrows(UnsupportedOperationException.class,
                () -> next.succeededPhaseEvidencePrefix().add(firstEvidence));
        assertThrows(IllegalStateException.class, initial::beginNext);
        assertThrows(IllegalStateException.class,
                () -> succeeded.markSucceeded(firstEvidence));
    }

    @Test
    void stateRejectsMismatchedEnvironmentAndInvalidActivationBinding() {
        CancellationTrustInstallationPlan plan = CancellationTrustInstallationPlanTest.validPlan();
        InstallationEnvironmentEvidence wrongEnvironment = new InstallationEnvironmentEvidence(
                UUID.fromString("00000000-0000-0000-0000-000000000999"),
                "fake-adapter", "fake-v1", plan.principals(), "fake-filesystem", true, true);

        assertThrows(IllegalArgumentException.class, () -> InstallationTransactionState.start(
                plan, wrongEnvironment, "release-v1", PERMISSION_POLICY_SHA256,
                Optional.of("activation-old"), "activation-new"));
        assertThrows(IllegalArgumentException.class, () -> InstallationTransactionState.start(
                plan, environment(plan), "release-v1", PERMISSION_POLICY_SHA256,
                Optional.empty(), "activation-new"));
        assertThrows(IllegalArgumentException.class, () -> InstallationTransactionState.start(
                plan, environment(plan), "release-v1", "not-a-digest",
                Optional.of("activation-old"), "activation-new"));
    }

    @Test
    void stateRejectsForeignPhaseRevisionAndActivationEvidence() {
        InstallationTransactionState initial = initialState("activation-new");
        UUID foreign = UUID.fromString("00000000-0000-0000-0000-000000000999");

        assertThrows(IllegalArgumentException.class, () -> initial.markSucceeded(
                binding(foreign, initial.phase(), initial.revision(), "a".repeat(64),
                        Optional.empty())));
        assertThrows(IllegalArgumentException.class, () -> initial.markSucceeded(
                binding(initial.plan().transactionId(), InstallationPhase.ACTIVATE,
                        initial.revision(), "a".repeat(64),
                        Optional.of("activation-new"))));
        assertThrows(IllegalArgumentException.class, () -> initial.markSucceeded(
                binding(initial.plan().transactionId(), initial.phase(), 2,
                        "a".repeat(64), Optional.empty())));
        assertThrows(IllegalArgumentException.class, () -> initial.markSucceeded(
                binding(initial.plan().transactionId(), initial.phase(), initial.revision(),
                        "a".repeat(64), Optional.of("activation-new"))));

        InstallationTransactionState activation = advanceToPending(
                initial, InstallationPhase.ACTIVATE);
        assertThrows(IllegalArgumentException.class, () -> activation.markSucceeded(
                binding(activation.plan().transactionId(), activation.phase(),
                        activation.revision(), "a".repeat(64), Optional.empty())));
        assertThrows(IllegalArgumentException.class, () -> activation.markSucceeded(
                binding(activation.plan().transactionId(), activation.phase(),
                        activation.revision(), "a".repeat(64),
                        Optional.of("activation-other"))));
    }

    @Test
    void constructorAndSuccessorRejectPrefixRewriteOrLengthDrift() {
        InstallationTransactionState initial = initialState("activation-new");
        InstallationTransactionState succeeded = succeed(initial);
        List<InstallationPhaseEvidence> changed = new ArrayList<>(
                succeeded.succeededPhaseEvidencePrefix());
        changed.set(0, evidence(initial, "b".repeat(64)));

        assertFalse(succeeded.isImmediateSuccessor(copyWithPrefix(succeeded, changed)));
        assertThrows(IllegalArgumentException.class,
                () -> copyWithPrefix(succeeded, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> copyWithPrefix(initial, List.of(evidence(initial, "a".repeat(64)))));
    }

    @Test
    void recoveryRegionChangesOnlyAfterBoundMetadataAndActivationEvidence() {
        InstallationTransactionState state = initialState("activation-new");
        assertEquals(InstallationTransactionState.RecoveryRegion.BEFORE_FINAL_METADATA,
                state.recoveryRegion());

        state = advanceToPending(state, InstallationPhase.PUBLISH_METADATA);
        assertEquals(InstallationTransactionState.RecoveryRegion.BEFORE_FINAL_METADATA,
                state.recoveryRegion());
        state = succeed(state);
        assertEquals(
                InstallationTransactionState.RecoveryRegion.AFTER_METADATA_BEFORE_ACTIVATION,
                state.recoveryRegion());

        state = advanceToPending(state, InstallationPhase.ACTIVATE);
        assertEquals(
                InstallationTransactionState.RecoveryRegion.AFTER_METADATA_BEFORE_ACTIVATION,
                state.recoveryRegion());
        state = succeed(state);
        assertEquals(InstallationTransactionState.RecoveryRegion.AFTER_ACTIVATION_EXACT_REPLAY,
                state.recoveryRegion());
    }

    @Test
    void finalSucceededStateRetainsAllElevenBindingsAndCannotAdvance() {
        InstallationTransactionState pending = advanceToPending(
                initialState("activation-new"), InstallationPhase.RECORD_FINAL_EVIDENCE);
        InstallationTransactionState state = succeed(pending);

        assertTrue(state.isTerminalRecord());
        assertEquals(21, state.revision());
        assertEquals(InstallationPhase.requiredOrder(), state.succeededPhaseEvidencePrefix()
                .stream().map(InstallationPhaseEvidence::phase).toList());
        assertThrows(IllegalStateException.class,
                () -> state.markSucceeded(state.succeededPhaseEvidencePrefix().get(0)));
        assertThrows(IllegalStateException.class, state::beginNext);
    }

    @Test
    void fakeStoreProvidesExactReplayConflictAndRevisionCas() throws Exception {
        InMemoryFakeStore store = new InMemoryFakeStore();
        InstallationTransactionState initial = initialState("activation-new");

        InstallationTransactionStore.Mutation created = store.create(initial);
        InstallationTransactionStore.Mutation createReplay = store.create(initial);
        assertEquals(initial, created.state());
        assertEquals(InstallationTransactionStore.MutationDisposition.CREATED,
                created.disposition());
        assertEquals(initial, createReplay.state());
        assertEquals(InstallationTransactionStore.MutationDisposition.EXACT_REPLAY,
                createReplay.disposition());
        assertEquals(1, store.mutations);
        InstallationTransactionStoreException changed = assertThrows(
                InstallationTransactionStoreException.class,
                () -> store.create(initialState("activation-other")));
        assertEquals(InstallationTransactionStoreException.Reason.TRANSACTION_CONFLICT,
                changed.reason());

        InstallationTransactionState succeeded = succeed(initial);
        InstallationTransactionStore.Mutation advanced = store.compareAndExchange(
                initial.plan().transactionId(), 0, succeeded);
        InstallationTransactionStore.Mutation advanceReplay = store.compareAndExchange(
                initial.plan().transactionId(), 0, succeeded);
        assertEquals(InstallationTransactionStore.MutationDisposition.ADVANCED,
                advanced.disposition());
        assertEquals(InstallationTransactionStore.MutationDisposition.EXACT_REPLAY,
                advanceReplay.disposition());
        assertEquals(succeeded, store.resolve(initial.plan().transactionId()));
        assertEquals(2, store.mutations);

        InstallationTransactionState changedEvidence = initial.markSucceeded(
                evidence(initial, "b".repeat(64)));
        InstallationTransactionStoreException stale = assertThrows(
                InstallationTransactionStoreException.class,
                () -> store.compareAndExchange(
                        initial.plan().transactionId(), 0, changedEvidence));
        assertEquals(InstallationTransactionStoreException.Reason.REVISION_CONFLICT,
                stale.reason());
        assertEquals(succeeded, store.resolve(initial.plan().transactionId()));
    }

    @Test
    void fakeStoreRefusesInvalidTransitionAndAbsentResolution() {
        InMemoryFakeStore store = new InMemoryFakeStore();
        InstallationTransactionState initial = initialState("activation-new");

        InstallationTransactionStoreException absent = assertThrows(
                InstallationTransactionStoreException.class,
                () -> store.resolve(UUID.fromString(
                        "00000000-0000-0000-0000-000000000999")));
        assertEquals(InstallationTransactionStoreException.Reason.NOT_FOUND, absent.reason());

        InstallationTransactionStoreException invalid = assertThrows(
                InstallationTransactionStoreException.class, () -> {
            store.create(initial);
            store.compareAndExchange(initial.plan().transactionId(), 0,
                    succeed(initial).beginNext());
        });
        assertEquals(InstallationTransactionStoreException.Reason.INVALID_TRANSITION,
                invalid.reason());
    }

    private static InstallationTransactionState initialState(String requestedActivation) {
        CancellationTrustInstallationPlan plan = CancellationTrustInstallationPlanTest.validPlan();
        return InstallationTransactionState.start(
                plan, environment(plan), "release-v1", PERMISSION_POLICY_SHA256,
                Optional.of("activation-old"), requestedActivation);
    }

    private static InstallationEnvironmentEvidence environment(
            CancellationTrustInstallationPlan plan) {
        return new InstallationEnvironmentEvidence(plan.transactionId(), "fake-adapter",
                "fake-v1", plan.principals(), "fake-filesystem", true, true);
    }

    private static InstallationTransactionState advanceToPending(
            InstallationTransactionState state, InstallationPhase target) {
        InstallationTransactionState current = state;
        while (current.phase() != target) {
            if (current.stepStatus() == InstallationTransactionState.StepStatus.PENDING) {
                current = succeed(current);
            }
            current = current.beginNext();
        }
        return current;
    }

    private static InstallationTransactionState succeed(
            InstallationTransactionState pending) {
        return pending.markSucceeded(evidence(pending, "a".repeat(64)));
    }

    private static InstallationPhaseEvidence evidence(
            InstallationTransactionState pending, String digest) {
        Optional<String> activation = pending.phase() == InstallationPhase.ACTIVATE
                ? Optional.of(pending.requestedActivationIdentity())
                : Optional.empty();
        return binding(pending.plan().transactionId(), pending.phase(),
                pending.revision(), digest, activation);
    }

    private static InstallationPhaseEvidence binding(
            UUID transactionId,
            InstallationPhase phase,
            long pendingRevision,
            String digest,
            Optional<String> activationIdentity) {
        return new InstallationPhaseEvidence(
                InstallationPhaseEvidence.SCHEMA_VERSION,
                transactionId, phase, pendingRevision, digest, activationIdentity);
    }

    private static InstallationTransactionState copyWithPrefix(
            InstallationTransactionState state,
            List<InstallationPhaseEvidence> prefix) {
        return new InstallationTransactionState(
                state.schemaVersion(), state.plan(), state.environment(),
                state.sourceReleaseVersion(), state.permissionPolicySha256(),
                state.expectedCurrentActivationIdentity(),
                state.requestedActivationIdentity(), prefix, state.revision(),
                state.phase(), state.stepStatus());
    }

    private static final class InMemoryFakeStore implements InstallationTransactionStore {
        private final Map<UUID, InstallationTransactionState> states = new HashMap<>();
        private int mutations;

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
            states.put(transactionId, replacement);
            mutations++;
            return new Mutation(replacement, MutationDisposition.ADVANCED);
        }

        private static InstallationTransactionStoreException failure(
                InstallationTransactionStoreException.Reason reason) {
            return new InstallationTransactionStoreException(reason, "fake store refusal");
        }
    }
}
