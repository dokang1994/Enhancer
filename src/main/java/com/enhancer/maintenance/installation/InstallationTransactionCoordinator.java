package com.enhancer.maintenance.installation;

import java.util.Objects;
import java.util.UUID;

/**
 * Pure one-phase coordinator. It orders injected ports but implements no effect,
 * persistence, automatic retry, or installation-success decision.
 */
public final class InstallationTransactionCoordinator {
    private final InstallationTransactionStore store;
    private final PreflightVerifier preflightVerifier;
    private final PhaseEffectPort phaseEffectPort;
    private final ActivationPort activationPort;

    public InstallationTransactionCoordinator(
            InstallationTransactionStore store,
            PreflightVerifier preflightVerifier,
            PhaseEffectPort phaseEffectPort,
            ActivationPort activationPort) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.preflightVerifier = Objects.requireNonNull(
                preflightVerifier, "preflightVerifier must not be null");
        this.phaseEffectPort = Objects.requireNonNull(
                phaseEffectPort, "phaseEffectPort must not be null");
        this.activationPort = Objects.requireNonNull(
                activationPort, "activationPort must not be null");
    }

    public Outcome start(InstallationTransactionState initial)
            throws InstallationTransactionStoreException,
            InstallationPhaseExecutionException {
        InstallationTransactionState checked = Objects.requireNonNull(
                initial, "initial must not be null");
        if (checked.revision() != 0
                || checked.phase() != InstallationPhase.requiredOrder().get(0)
                || checked.stepStatus() != InstallationTransactionState.StepStatus.PENDING) {
            throw new IllegalArgumentException("initial must be the first pending phase");
        }
        InstallationTransactionStore.Mutation mutation = store.create(checked);
        requireState(mutation, checked);
        return switch (mutation.disposition()) {
            case CREATED -> executeOwnedPending(checked);
            case EXACT_REPLAY -> reconciliation(mutation.state());
            case ADVANCED -> throw corruptStore("create returned an advance receipt");
        };
    }

    public Outcome advance(UUID transactionId)
            throws InstallationTransactionStoreException,
            InstallationPhaseExecutionException {
        UUID checkedId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        InstallationTransactionState current = store.resolve(checkedId);
        if (!checkedId.equals(current.plan().transactionId())) {
            throw corruptStore("resolved transaction identity does not match");
        }
        if (current.isTerminalRecord()) {
            return new Outcome(current, OutcomeStatus.EXACT_TERMINAL_REPLAY);
        }
        if (current.stepStatus() == InstallationTransactionState.StepStatus.PENDING) {
            return reconciliation(current);
        }

        InstallationTransactionState pending = current.beginNext();
        InstallationTransactionStore.Mutation mutation = store.compareAndExchange(
                checkedId, current.revision(), pending);
        requireState(mutation, pending);
        return switch (mutation.disposition()) {
            case ADVANCED -> executeOwnedPending(pending);
            case EXACT_REPLAY -> reconciliation(mutation.state());
            case CREATED -> throw corruptStore("compare-and-exchange returned a create receipt");
        };
    }

    private Outcome executeOwnedPending(InstallationTransactionState pending)
            throws InstallationTransactionStoreException,
            InstallationPhaseExecutionException {
        InstallationPhaseEvidence result = invoke(pending);
        if (result == null) {
            throw new InstallationPhaseExecutionException(
                    InstallationPhaseExecutionException.Reason.RESULT_BINDING_INVALID,
                    pending.phase(), "phase result binding is invalid");
        }
        InstallationTransactionState succeeded;
        try {
            succeeded = pending.markSucceeded(result);
        } catch (IllegalArgumentException exception) {
            throw new InstallationPhaseExecutionException(
                    InstallationPhaseExecutionException.Reason.RESULT_BINDING_INVALID,
                    pending.phase(), "phase result binding is invalid");
        }
        InstallationTransactionStore.Mutation mutation = store.compareAndExchange(
                pending.plan().transactionId(), pending.revision(), succeeded);
        requireState(mutation, succeeded);
        if (mutation.disposition() == InstallationTransactionStore.MutationDisposition.EXACT_REPLAY) {
            return reconciliation(mutation.state());
        }
        if (mutation.disposition() != InstallationTransactionStore.MutationDisposition.ADVANCED) {
            throw corruptStore("phase success returned a non-advance receipt");
        }
        OutcomeStatus status = succeeded.isTerminalRecord()
                ? OutcomeStatus.TERMINAL_RECORDED
                : OutcomeStatus.PHASE_RECORDED;
        return new Outcome(succeeded, status);
    }

    private InstallationPhaseEvidence invoke(InstallationTransactionState pending)
            throws InstallationPhaseExecutionException {
        try {
            return switch (pending.phase()) {
                case RESOLVE_PRINCIPALS ->
                        preflightVerifier.verifyPrincipalsAndEnvironment(pending);
                case VERIFY_SOURCE_AND_TOPOLOGY ->
                        preflightVerifier.verifySourceAndTopology(pending);
                case STAGE_PRIVATE_ARTIFACTS ->
                        phaseEffectPort.stagePrivateArtifacts(pending);
                case APPLY_AND_VERIFY_STAGED_PERMISSIONS ->
                        phaseEffectPort.applyAndVerifyStagedPermissions(pending);
                case PREPARE_TRUST_DIRECTORY_AND_LOCK ->
                        phaseEffectPort.prepareTrustDirectoryAndLock(pending);
                case PUBLISH_POLICY -> phaseEffectPort.publishPolicy(pending);
                case PUBLISH_METADATA -> phaseEffectPort.publishMetadata(pending);
                case VERIFY_FINAL_BYTES_AND_PERMISSIONS ->
                        phaseEffectPort.verifyFinalBytesAndPermissions(pending);
                case PROBE_AS_RUNTIME -> phaseEffectPort.probeAsRuntime(pending);
                case ACTIVATE -> activationPort.activate(pending);
                case RECORD_FINAL_EVIDENCE -> phaseEffectPort.recordFinalEvidence(pending);
            };
        } catch (InstallationPhaseExecutionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new InstallationPhaseExecutionException(
                    InstallationPhaseExecutionException.Reason.UNEXPECTED_PORT_FAILURE,
                    pending.phase(), "phase port failed unexpectedly");
        }
    }

    private static void requireState(
            InstallationTransactionStore.Mutation mutation,
            InstallationTransactionState expected)
            throws InstallationTransactionStoreException {
        if (mutation == null || !expected.equals(mutation.state())) {
            throw corruptStore("store receipt state does not match the requested state");
        }
    }

    private static Outcome reconciliation(InstallationTransactionState state) {
        return new Outcome(state, OutcomeStatus.REQUIRES_RECONCILIATION);
    }

    private static InstallationTransactionStoreException corruptStore(String detail) {
        return new InstallationTransactionStoreException(
                InstallationTransactionStoreException.Reason.CORRUPT_STATE, detail);
    }

    public record Outcome(
            InstallationTransactionState state,
            OutcomeStatus status) {
        public Outcome {
            state = Objects.requireNonNull(state, "state must not be null");
            status = Objects.requireNonNull(status, "status must not be null");
            if ((status == OutcomeStatus.TERMINAL_RECORDED
                    || status == OutcomeStatus.EXACT_TERMINAL_REPLAY)
                    && !state.isTerminalRecord()) {
                throw new IllegalArgumentException("terminal outcome requires terminal record");
            }
            if (status == OutcomeStatus.PHASE_RECORDED
                    && (state.isTerminalRecord()
                    || state.stepStatus()
                    != InstallationTransactionState.StepStatus.SUCCEEDED)) {
                throw new IllegalArgumentException(
                        "phase-recorded outcome requires non-terminal succeeded state");
            }
        }
    }

    public enum OutcomeStatus {
        PHASE_RECORDED,
        TERMINAL_RECORDED,
        EXACT_TERMINAL_REPLAY,
        REQUIRES_RECONCILIATION
    }

    public interface PreflightVerifier {
        InstallationPhaseEvidence verifyPrincipalsAndEnvironment(
                InstallationTransactionState pending)
                throws InstallationPhaseExecutionException;

        InstallationPhaseEvidence verifySourceAndTopology(
                InstallationTransactionState pending)
                throws InstallationPhaseExecutionException;
    }

    public interface PhaseEffectPort {
        InstallationPhaseEvidence stagePrivateArtifacts(InstallationTransactionState pending)
                throws InstallationPhaseExecutionException;

        InstallationPhaseEvidence applyAndVerifyStagedPermissions(
                InstallationTransactionState pending)
                throws InstallationPhaseExecutionException;

        InstallationPhaseEvidence prepareTrustDirectoryAndLock(
                InstallationTransactionState pending)
                throws InstallationPhaseExecutionException;

        InstallationPhaseEvidence publishPolicy(InstallationTransactionState pending)
                throws InstallationPhaseExecutionException;

        InstallationPhaseEvidence publishMetadata(InstallationTransactionState pending)
                throws InstallationPhaseExecutionException;

        InstallationPhaseEvidence verifyFinalBytesAndPermissions(
                InstallationTransactionState pending)
                throws InstallationPhaseExecutionException;

        InstallationPhaseEvidence probeAsRuntime(InstallationTransactionState pending)
                throws InstallationPhaseExecutionException;

        InstallationPhaseEvidence recordFinalEvidence(InstallationTransactionState pending)
                throws InstallationPhaseExecutionException;
    }

    public interface ActivationPort {
        InstallationPhaseEvidence activate(InstallationTransactionState pending)
                throws InstallationPhaseExecutionException;
    }
}
