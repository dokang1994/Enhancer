package com.enhancer.runtime;

import java.util.Objects;

/** Opaque deterministic reference published only after its runtime event is durable. */
public record RuntimeEventPublicationReference(String reference) {
    public static final int MAX_REFERENCE_CHARACTERS = 1024;
    private static final String PREFIX = "runtime-event/";

    public RuntimeEventPublicationReference {
        reference = RuntimeEventContractSupport.bounded(
                reference,
                "reference",
                MAX_REFERENCE_CHARACTERS);
        if (!reference.startsWith(PREFIX)) {
            throw new IllegalArgumentException(
                    "runtime event publication reference has an invalid prefix");
        }
    }

    public static RuntimeEventPublicationReference from(RuntimeEvent event) {
        RuntimeEvent checked = Objects.requireNonNull(
                event, "event must not be null");
        return new RuntimeEventPublicationReference(
                PREFIX
                        + checked.binding().goalId()
                        + "/"
                        + checked.eventId());
    }
}
