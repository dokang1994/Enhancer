package com.enhancer.runtime;

import java.util.Objects;
import java.util.Optional;

/** One immutable request and its current durable effect status. */
public record ExternalEffectRecord(
        ExternalEffectRequest request,
        ExternalEffectStatus status,
        Optional<ExternalEffectOutcomeEvidence> outcomeEvidence) {

    public ExternalEffectRecord {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(
                outcomeEvidence, "outcomeEvidence must not be null");
        if (status == ExternalEffectStatus.PREPARED
                && outcomeEvidence.isPresent()) {
            throw new IllegalArgumentException(
                    "prepared external effect cannot carry outcome evidence");
        }
        if (status.isTerminal() && outcomeEvidence.isEmpty()) {
            throw new IllegalArgumentException(
                    "terminal external effect requires outcome evidence");
        }
    }

    public ExternalEffectRecord(
            ExternalEffectRequest request,
            ExternalEffectStatus status) {
        this(request, status, Optional.empty());
    }

    ExternalEffectRecord terminate(
            ExternalEffectStatus terminalStatus,
            ExternalEffectOutcomeEvidence evidence) {
        Objects.requireNonNull(
                terminalStatus, "terminalStatus must not be null");
        Objects.requireNonNull(evidence, "evidence must not be null");
        if (!terminalStatus.isTerminal()) {
            throw new IllegalArgumentException(
                    "external effect outcome must be terminal");
        }
        if (status != ExternalEffectStatus.PREPARED) {
            throw new IllegalStateException(
                    "external effect already has a terminal outcome");
        }
        return new ExternalEffectRecord(
                request, terminalStatus, Optional.of(evidence));
    }
}
