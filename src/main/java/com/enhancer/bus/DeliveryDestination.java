package com.enhancer.bus;

import java.util.Objects;

/**
 * An in-process delivery destination identified by its kind and a bounded name. A topic and a
 * queue that share a name are distinct destinations; equality requires equal kind and name.
 */
public record DeliveryDestination(DeliveryDestinationKind kind, String name) {

    public DeliveryDestination {
        Objects.requireNonNull(kind, "kind must not be null");
        name = BusContractSupport.bounded(
                name, "name", BusContractSupport.MAX_IDENTITY_CHARACTERS);
    }

    public static DeliveryDestination topic(String name) {
        return new DeliveryDestination(DeliveryDestinationKind.TOPIC, name);
    }

    public static DeliveryDestination queue(String name) {
        return new DeliveryDestination(DeliveryDestinationKind.QUEUE, name);
    }
}
