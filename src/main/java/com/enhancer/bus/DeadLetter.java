package com.enhancer.bus;

import java.util.Objects;

/**
 * An immutable record that a delivery to one subscription failed because its handler threw. It
 * captures the destination, the failed subscriber, the unmodified envelope, and a bounded reason
 * derived from the handler's exception. It is a terminal record for this increment: automatic
 * retry and re-delivery from the dead-letter record arrive in a later increment.
 */
public record DeadLetter(
        DeliveryDestination destination,
        String subscriberId,
        MessageEnvelope envelope,
        String reason) {

    public static final int MAX_REASON_CHARACTERS = 512;

    public DeadLetter {
        Objects.requireNonNull(destination, "destination must not be null");
        subscriberId = BusContractSupport.bounded(
                subscriberId, "subscriberId", BusContractSupport.MAX_IDENTITY_CHARACTERS);
        Objects.requireNonNull(envelope, "envelope must not be null");
        reason = BusContractSupport.bounded(reason, "reason", MAX_REASON_CHARACTERS);
    }
}
