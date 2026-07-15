package com.enhancer.bus;

import java.util.Objects;
import java.util.Optional;

/**
 * The immutable result of delivering one envelope toward one subscription. An {@code UNROUTED}
 * outcome carries no subscriber identity because no subscription received the envelope; every
 * other status must name the subscription it targeted.
 */
public record DeliveryOutcome(
        DeliveryDestination destination,
        Optional<String> subscriberId,
        String messageId,
        DeliveryStatus status) {

    public DeliveryOutcome {
        Objects.requireNonNull(destination, "destination must not be null");
        Objects.requireNonNull(subscriberId, "subscriberId must not be null");
        subscriberId = subscriberId.map(id -> BusContractSupport.bounded(
                id, "subscriberId", BusContractSupport.MAX_IDENTITY_CHARACTERS));
        messageId = BusContractSupport.canonicalUuid(messageId, "messageId");
        Objects.requireNonNull(status, "status must not be null");
        if (status == DeliveryStatus.UNROUTED && subscriberId.isPresent()) {
            throw new IllegalArgumentException(
                    "an UNROUTED outcome must not carry a subscriberId");
        }
        if (status != DeliveryStatus.UNROUTED && subscriberId.isEmpty()) {
            throw new IllegalArgumentException(
                    status + " outcome must carry a subscriberId");
        }
    }
}
