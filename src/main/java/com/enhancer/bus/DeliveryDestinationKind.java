package com.enhancer.bus;

/**
 * The delivery semantics of a destination: topic fan-out to every subscriber, or queue
 * point-to-point delivery to a single consumer.
 */
public enum DeliveryDestinationKind {
    TOPIC,
    QUEUE
}
