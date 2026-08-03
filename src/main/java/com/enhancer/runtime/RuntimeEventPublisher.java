package com.enhancer.runtime;

import java.io.IOException;

/** Publishes only an opaque durable runtime-event reference to a later adapter. */
@FunctionalInterface
public interface RuntimeEventPublisher {
    void publish(RuntimeEventPublicationReference reference) throws IOException;
}
