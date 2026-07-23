package com.enhancer.runtime;

import java.util.Objects;

/** Typed terminal adapter claim plus redacted complete evidence content. */
public record ExternalEffectAdapterResult(
        ExternalEffectStatus status,
        String evidenceContent) {

    public ExternalEffectAdapterResult {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(
                evidenceContent, "evidenceContent must not be null");
        if (!status.isTerminal()) {
            throw new IllegalArgumentException(
                    "adapter result status must be terminal");
        }
        if (evidenceContent.isBlank()) {
            throw new IllegalArgumentException(
                    "adapter result evidence must not be blank");
        }
    }
}
