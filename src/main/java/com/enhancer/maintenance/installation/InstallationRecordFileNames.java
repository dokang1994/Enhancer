package com.enhancer.maintenance.installation;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Pure bounded leaf-name derivation; it accepts and resolves no filesystem root. */
final class InstallationRecordFileNames {
    private static final String TRANSACTION_SUFFIX = ".installation-transaction-v1";
    private static final String TRANSACTION_LOCK_SUFFIX =
            ".installation-transaction-lock-v1";
    private static final String EVIDENCE_SUFFIX = ".installation-phase-evidence-v1";

    private InstallationRecordFileNames() {}

    static String transaction(UUID transactionId) {
        return Objects.requireNonNull(
                transactionId, "transactionId must not be null") + TRANSACTION_SUFFIX;
    }

    static String transactionLock(UUID transactionId) {
        return Objects.requireNonNull(
                transactionId, "transactionId must not be null")
                + TRANSACTION_LOCK_SUFFIX;
    }

    static String evidence(InstallationPhaseEvidencePoint point) {
        InstallationPhaseEvidencePoint checked = Objects.requireNonNull(
                point, "point must not be null");
        String phase = checked.phase().name()
                .toLowerCase(Locale.ROOT)
                .replace('_', '-');
        return checked.transactionId()
                + "."
                + checked.pendingRevision()
                + "."
                + phase
                + EVIDENCE_SUFFIX;
    }
}
