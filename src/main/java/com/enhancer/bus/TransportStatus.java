package com.enhancer.bus;

/**
 * Admission result for one transport hop. These values are deliberately distinct from {@link
 * DeliveryStatus}, which describes behavior owned by a Message Bus after receipt.
 */
public enum TransportStatus {
    ACCEPTED,
    BACKPRESSURED,
    UNAVAILABLE;

    /** Returns whether the transport accepted responsibility for attempting the hop. */
    public boolean isAccepted() {
        return this == ACCEPTED;
    }
}
