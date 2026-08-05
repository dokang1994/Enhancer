package com.enhancer.runtime;

import java.util.Objects;

/** Exact read-only resolution of one retained publication point and its canonical event. */
public record RuntimeEventPointResolution(
        RuntimeEventPublicationReference reference,
        RuntimeEvent event,
        long streamRevision) {

    public RuntimeEventPointResolution {
        Objects.requireNonNull(reference, "reference must not be null");
        Objects.requireNonNull(event, "event must not be null");
        if (streamRevision <= 0) {
            throw new IllegalArgumentException(
                    "streamRevision must be positive");
        }
        if (!RuntimeEventPublicationReference.from(event).equals(reference)) {
            throw new IllegalArgumentException(
                    "event does not match its publication reference");
        }
    }
}
