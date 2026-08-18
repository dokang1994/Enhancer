package com.enhancer.maintenance.installation;

import java.util.Objects;

/** Bounded point-bound refusal from immutable semantic-evidence storage. */
public final class InstallationPhaseEvidenceStoreException extends Exception {
    private static final long serialVersionUID = 1L;
    private final Reason reason;
    private final InstallationPhaseEvidencePoint point;

    public InstallationPhaseEvidenceStoreException(
            Reason reason,
            InstallationPhaseEvidencePoint point,
            String detail) {
        super(InstallationPrincipal.boundedText(detail, "detail"));
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
        this.point = Objects.requireNonNull(point, "point must not be null");
    }

    public Reason reason() {
        return reason;
    }

    public InstallationPhaseEvidencePoint point() {
        return point;
    }

    public enum Reason {
        EVIDENCE_CONFLICT,
        UNSUPPORTED_SCHEMA,
        CORRUPT_EVIDENCE,
        FOREIGN_EVIDENCE,
        CAPACITY_EXCEEDED,
        STORE_UNAVAILABLE
    }
}
