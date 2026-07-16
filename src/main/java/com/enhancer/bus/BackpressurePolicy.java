package com.enhancer.bus;

/**
 * Immutable admission bound for publications waiting in the in-process bus's pending queue.
 * Capacity exhaustion never blocks the single-threaded drain; the attempted publication is
 * refused with {@link DeliveryStatus#BACKPRESSURED} instead.
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
