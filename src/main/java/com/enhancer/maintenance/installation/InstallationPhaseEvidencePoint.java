package com.enhancer.maintenance.installation;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Exact read-only identity for one pending phase's separately retained evidence. */
public record InstallationPhaseEvidencePoint(
        UUID transactionId,
        InstallationPhase phase,
        long pendingRevision) {
    private static final List<InstallationPhase> ORDER = InstallationPhase.requiredOrder();

    public InstallationPhaseEvidencePoint {
        transactionId = Objects.requireNonNull(
                transactionId, "transactionId must not be null");
        phase = Objects.requireNonNull(phase, "phase must not be null");
        int phaseIndex = ORDER.indexOf(phase);
        if (phaseIndex < 0 || pendingRevision != (long) phaseIndex * 2) {
            throw new IllegalArgumentException(
                    "pendingRevision must identify the exact pending phase");
        }
    }

    public static InstallationPhaseEvidencePoint fromPending(
            InstallationTransactionState pending) {
        InstallationTransactionState checked = Objects.requireNonNull(
                pending, "pending must not be null");
        if (!checked.requiresReconciliation()) {
            throw new IllegalArgumentException("state must be pending");
        }
        return new InstallationPhaseEvidencePoint(
                checked.plan().transactionId(), checked.phase(), checked.revision());
    }
}
