package com.enhancer.maintenance.installation;

import java.util.Objects;

/** Bounded typed refusal while resolving or validating one exact phase-evidence point. */
public final class InstallationPhaseEvidenceResolutionException extends Exception {
    private static final long serialVersionUID = 1L;
    private final Reason reason;
    private final InstallationPhase phase;

    public InstallationPhaseEvidenceResolutionException(
            Reason reason,
            InstallationPhase phase,
            String detail) {
        super(InstallationPrincipal.boundedText(detail, "detail"));
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
        this.phase = Objects.requireNonNull(phase, "phase must not be null");
    }

    public Reason reason() {
        return reason;
    }

    public InstallationPhase phase() {
        return phase;
    }

    public enum Reason {
        UNSUPPORTED_SCHEMA,
        CORRUPT_EVIDENCE,
        FOREIGN_EVIDENCE,
        RESOLVER_UNAVAILABLE
    }
}
