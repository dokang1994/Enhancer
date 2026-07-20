package com.enhancer.bus;

/**
 * Immutable finite admission bound, used by the in-process bus for publications waiting in its
 * pending queue and by {@link FileSpoolMessageTransport} for messages waiting in its spool.
 * Capacity exhaustion never blocks: the in-process bus refuses the attempted publication with
 * {@link DeliveryStatus#BACKPRESSURED} rather than stalling its single-threaded drain, and the
 * transport refuses the hop with {@link TransportStatus#BACKPRESSURED} without spooling it.
 */
public record BackpressurePolicy(int maxPendingPublications) {

    public static final int MAX_PENDING_PUBLICATIONS = 4096;

    public BackpressurePolicy {
        if (maxPendingPublications < 1
                || maxPendingPublications > MAX_PENDING_PUBLICATIONS) {
            throw new IllegalArgumentException(
                    "maxPendingPublications must be between 1 and "
                            + MAX_PENDING_PUBLICATIONS);
        }
    }

    public static BackpressurePolicy of(int maxPendingPublications) {
        return new BackpressurePolicy(maxPendingPublications);
    }

    /** Returns the finite default admission bound. */
    public static BackpressurePolicy standard() {
        return new BackpressurePolicy(MAX_PENDING_PUBLICATIONS);
    }
}
