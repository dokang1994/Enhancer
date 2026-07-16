package com.enhancer.bus;

/**
 * The result of delivering one envelope toward one subscription: newly delivered, an idempotent
 * duplicate that invoked no handler, or failed because the subscriber's handler exhausted the
 * bounded retry policy and the envelope was captured as a dead letter.
 *
 * <p>{@code UNROUTED}, {@code CANCELLED}, and {@code ENQUEUED} are scope-level results that
 * describe the envelope's fate at the bus rather than one delivery to one subscription: no
 * subscription received it, its correlation was cancelled and it was refused admission, or it was
 * published from inside a handler and accepted for delivery after the current drain completes.
 */
public enum DeliveryStatus {
    DELIVERED,
    DUPLICATE,
    UNROUTED,
    FAILED,
    CANCELLED,
    ENQUEUED;

    /**
     * Returns whether this status describes the envelope's fate at the bus rather than one
     * delivery to one subscription. A scope-level status names no subscriber.
     */
    public boolean isScopeLevel() {
        return this == UNROUTED || this == CANCELLED || this == ENQUEUED;
    }
}
