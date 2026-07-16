package com.enhancer.bus;

import java.util.Objects;

/**
 * One existing delivery route and envelope presented to a transport adapter. The transport
 * carries both values unchanged and creates no authority; receiving code must apply the same
 * repository and Message Bus rules as an in-process publication.
 */
public record TransportMessage(
        DeliveryDestination destination, MessageEnvelope envelope) {

    public TransportMessage {
        Objects.requireNonNull(destination, "destination must not be null");
        Objects.requireNonNull(envelope, "envelope must not be null");
    }
}
