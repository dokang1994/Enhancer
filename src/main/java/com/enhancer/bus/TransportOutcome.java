package com.enhancer.bus;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result of offering one {@link TransportMessage} to a transport adapter. Acceptance
 * carries no reason. A non-acceptance carries one bounded diagnostic reason and consumes no
 * Message Bus delivery state.
 */
public record TransportOutcome(TransportStatus status, Optional<String> reason) {
    public static final int MAX_REASON_CHARACTERS = 1024;

    public TransportOutcome {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        if (status.isAccepted()) {
            if (reason.isPresent()) {
                throw new IllegalArgumentException(
                        "an accepted transport outcome must not carry a reason");
            }
        } else {
            if (reason.isEmpty()) {
                throw new IllegalArgumentException(
                        "a non-accepted transport outcome must carry a reason");
            }
            reason = reason.map(value -> BusContractSupport.bounded(
                    value, "reason", MAX_REASON_CHARACTERS));
        }
    }

    public static TransportOutcome accepted() {
        return new TransportOutcome(TransportStatus.ACCEPTED, Optional.empty());
    }

    public static TransportOutcome backpressured(String reason) {
        return new TransportOutcome(TransportStatus.BACKPRESSURED, Optional.ofNullable(reason));
    }

    public static TransportOutcome unavailable(String reason) {
        return new TransportOutcome(TransportStatus.UNAVAILABLE, Optional.ofNullable(reason));
    }
}
