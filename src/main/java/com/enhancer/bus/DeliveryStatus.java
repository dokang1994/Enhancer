package com.enhancer.bus;

/**
 * The result of delivering one envelope toward one subscription: newly delivered, an idempotent
 * duplicate that invoked no handler, unrouted because no subscription received it, or failed
 * because the subscriber's handler threw and the envelope was captured as a dead letter.
 */
public enum DeliveryStatus {
    DELIVERED,
    DUPLICATE,
    UNROUTED,
    FAILED
}
