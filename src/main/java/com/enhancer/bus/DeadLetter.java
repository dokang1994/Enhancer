package com.enhancer.bus;

import java.util.Objects;

/**
 * An immutable record that a delivery to one subscription exhausted its bounded attempt policy
 * because its handler threw. It captures the destination, the failed subscriber, the unmodified
 * envelope, a bounded reason derived from the last failure, and how many handler invocations
 * failed. It is the sole authority for explicit re-delivery: a successful re-delivery resolves
 * it, and a renewed exhaustion replaces it with the accumulated attempt count.
 */
public record DeadLetter(
        DeliveryDestination destination,
        String subscriberId,
        MessageEnvelope envelope,
        String reason,
        int attempts) {

    public static final int MAX_REASON_CHARACTERS = 512;

    public DeadLetter {
        Objects.requireNonNull(destination, "destination must not be null");
        subscriberId = BusContractSupport.bounded(
                subscriberId, "subscriberId", BusContractSupport.MAX_IDENTITY_CHARACTERS);
        Objects.requireNonNull(envelope, "envelope must not be null");
        reason = BusContractSupport.bounded(reason, "reason", MAX_REASON_CHARACTERS);
        if (attempts < 1) {
            throw new IllegalArgumentException("attempts must record at least one failure");
        }
    }
}
