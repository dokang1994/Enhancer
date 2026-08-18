package com.enhancer.maintenance.installation;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure pending-state reconciler. It resolves one evidence point and invokes no phase,
 * permission, activation, filesystem, or native effect.
 */
public final class InstallationTransactionReconciler {
    private final InstallationTransactionStore store;
    private final InstallationPhaseEvidenceResolver evidenceResolver;

    public InstallationTransactionReconciler(
            InstallationTransactionStore store,
            InstallationPhaseEvidenceResolver evidenceResolver) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.evidenceResolver = Objects.requireNonNull(
                evidenceResolver, "evidenceResolver must not be null");
    }

    public Outcome reconcile(UUID transactionId)
            throws InstallationTransactionStoreException,
            InstallationPhaseEvidenceResolutionException {
        UUID checkedId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        InstallationTransactionState current = store.resolve(checkedId);
        if (!checkedId.equals(current.plan().transactionId())) {
            throw corruptStore("resolved transaction identity does not match");
        }
        if (!current.requiresReconciliation()) {
            return new Outcome(current, OutcomeStatus.NO_RECONCILIATION_REQUIRED);
        }

        InstallationPhaseEvidencePoint point = InstallationPhaseEvidencePoint.fromPending(
                current);
        Optional<InstallationPhaseEvidence> resolved = resolve(point);
        if (resolved.isEmpty()) {
            return new Outcome(current, OutcomeStatus.EVIDENCE_ABSENT);
        }

        InstallationTransactionState succeeded;
        try {
            succeeded = current.markSucceeded(resolved.orElseThrow());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InstallationPhaseEvidenceResolutionException(
                    InstallationPhaseEvidenceResolutionException.Reason.FOREIGN_EVIDENCE,
                    current.phase(), "resolved evidence does not bind the pending phase");
        }

        InstallationTransactionStore.Mutation mutation = store.compareAndExchange(
                checkedId, current.revision(), succeeded);
        requireState(mutation, succeeded);
        return switch (mutation.disposition()) {
            case ADVANCED -> new Outcome(succeeded, succeeded.isTerminalRecord()
                    ? OutcomeStatus.TERMINAL_RECONCILED
                    : OutcomeStatus.PHASE_RECONCILED);
            case EXACT_REPLAY ->
                    new Outcome(succeeded, OutcomeStatus.EXACT_RECONCILIATION_REPLAY);
            case CREATED -> throw corruptStore("reconciliation returned a create receipt");
        };
    }

    private Optional<InstallationPhaseEvidence> resolve(
            InstallationPhaseEvidencePoint point)
            throws InstallationPhaseEvidenceResolutionException {
        try {
            Optional<InstallationPhaseEvidence> resolved =
                    evidenceResolver.resolveAndRevalidate(point);
            if (resolved == null) {
                throw new InstallationPhaseEvidenceResolutionException(
                        InstallationPhaseEvidenceResolutionException.Reason.CORRUPT_EVIDENCE,
                        point.phase(), "resolver returned an invalid result");
            }
            return resolved;
        } catch (InstallationPhaseEvidenceResolutionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new InstallationPhaseEvidenceResolutionException(
                    InstallationPhaseEvidenceResolutionException.Reason.RESOLVER_UNAVAILABLE,
                    point.phase(), "resolver failed unexpectedly");
        }
    }

    private static void requireState(
            InstallationTransactionStore.Mutation mutation,
            InstallationTransactionState expected)
            throws InstallationTransactionStoreException {
        if (mutation == null || !expected.equals(mutation.state())) {
            throw corruptStore("store receipt state does not match reconciled state");
        }
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
            boolean pending = state.requiresReconciliation();
            if (status == OutcomeStatus.EVIDENCE_ABSENT && !pending) {
                throw new IllegalArgumentException("absent evidence requires pending state");
            }
            if (status == OutcomeStatus.NO_RECONCILIATION_REQUIRED && pending) {
                throw new IllegalArgumentException(
                        "no-reconciliation outcome requires succeeded state");
            }
            if (status == OutcomeStatus.PHASE_RECONCILED
                    && (pending || state.isTerminalRecord())) {
                throw new IllegalArgumentException(
                        "phase reconciliation requires non-terminal succeeded state");
            }
            if (status == OutcomeStatus.TERMINAL_RECONCILED && !state.isTerminalRecord()) {
                throw new IllegalArgumentException(
                        "terminal reconciliation requires terminal state");
            }
            if (status == OutcomeStatus.EXACT_RECONCILIATION_REPLAY && pending) {
                throw new IllegalArgumentException(
                        "exact reconciliation replay requires succeeded state");
            }
        }
    }

    public enum OutcomeStatus {
        PHASE_RECONCILED,
        TERMINAL_RECONCILED,
        EXACT_RECONCILIATION_REPLAY,
        EVIDENCE_ABSENT,
        NO_RECONCILIATION_REQUIRED
    }
}
