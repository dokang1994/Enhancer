package com.enhancer.bus;

import java.util.Objects;

/**
 * An ordered journal entry recording that one envelope was published to one destination.
 * Replaying journal entries re-dispatches them deterministically without appending to the journal.
 */
public record JournaledMessage(DeliveryDestination destination, MessageEnvelope envelope) {

    public JournaledMessage {
        Objects.requireNonNull(destination, "destination must not be null");
        Objects.requireNonNull(envelope, "envelope must not be null");
    }
}
