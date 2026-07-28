package com.enhancer.bus;

import java.util.Objects;
import java.util.Optional;

/**
 * Concrete file-spool result that preserves the transport-neutral outcome while exposing the
 * single accepted point name needed by a separately invoked point receiver.
 */
public record FileSpoolPublicationOutcome(
        TransportOutcome outcome,
        Optional<String> messageFile) {

    public FileSpoolPublicationOutcome {
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(messageFile, "messageFile must not be null");
        if (outcome.status().isAccepted() != messageFile.isPresent()) {
            throw new IllegalArgumentException(
                    "exactly an accepted file-spool outcome must carry a message file");
        }
    }
}
