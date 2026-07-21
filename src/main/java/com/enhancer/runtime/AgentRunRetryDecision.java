package com.enhancer.runtime;

import java.util.Objects;
import java.util.Optional;

/** Immutable outcome of a bounded AgentRun retry decision. */
public final class AgentRunRetryDecision {

    private static final AgentRunRetryDecision ADMITTED = new AgentRunRetryDecision(null);

    private final AgentRunRetryRefusalReason refusalReason;

    private AgentRunRetryDecision(AgentRunRetryRefusalReason refusalReason) {
        this.refusalReason = refusalReason;
    }

    public static AgentRunRetryDecision admitted() {
        return ADMITTED;
    }

    public static AgentRunRetryDecision refused(AgentRunRetryRefusalReason reason) {
        Objects.requireNonNull(reason, "reason must not be null");
        return new AgentRunRetryDecision(reason);
    }

    public boolean isAdmitted() {
        return refusalReason == null;
    }

    public Optional<AgentRunRetryRefusalReason> refusalReason() {
        return Optional.ofNullable(refusalReason);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AgentRunRetryDecision)) {
            return false;
        }
        return refusalReason == ((AgentRunRetryDecision) other).refusalReason;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(refusalReason);
    }

    @Override
    public String toString() {
        return isAdmitted() ? "AgentRunRetryDecision[ADMITTED]"
                : "AgentRunRetryDecision[REFUSED:" + refusalReason + "]";
    }
}
