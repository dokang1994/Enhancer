package com.enhancer.runtime;

import java.util.Objects;

/** One immutable request and its current durable effect status. */
public record ExternalEffectRecord(
        ExternalEffectRequest request,
        ExternalEffectStatus status) {

    public ExternalEffectRecord {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }

    ExternalEffectRecord terminate(ExternalEffectStatus terminalStatus) {
        Objects.requireNonNull(
                terminalStatus, "terminalStatus must not be null");
        if (!terminalStatus.isTerminal()) {
            throw new IllegalArgumentException(
                    "external effect outcome must be terminal");
        }
        if (status != ExternalEffectStatus.PREPARED) {
            throw new IllegalStateException(
                    "external effect already has a terminal outcome");
        }
        return new ExternalEffectRecord(request, terminalStatus);
    }
}
