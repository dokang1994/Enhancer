package com.enhancer.maintenance.installation;

import java.util.Objects;

/** Bounded typed refusal from a pure coordinator phase port or result validation. */
public final class InstallationPhaseExecutionException extends Exception {
    private static final long serialVersionUID = 1L;
    private final Reason reason;
    private final InstallationPhase phase;

    public InstallationPhaseExecutionException(
            Reason reason, InstallationPhase phase, String detail) {
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
        PORT_REFUSED,
        RESULT_BINDING_INVALID,
        UNEXPECTED_PORT_FAILURE
    }
}
