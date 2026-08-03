package com.enhancer.runtime;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/** Persists a derived runtime fact before exposing its opaque reference for publication. */
public final class RuntimeEventRecorder {
    private final RuntimeEventStore store;
    private final RuntimeEventPublisher publisher;

    public RuntimeEventRecorder(
            RuntimeEventStore store,
            RuntimeEventPublisher publisher) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.publisher = Objects.requireNonNull(
                publisher, "publisher must not be null");
    }

    public RuntimeEventAppendResult recordAndPublish(RuntimeEvent event)
            throws IOException {
        RuntimeEvent checked = Objects.requireNonNull(
                event, "event must not be null");
        RuntimeEventAppendResult result = store.append(checked);
        publisher.publish(RuntimeEventPublicationReference.from(checked));
        return result;
    }

    /**
     * Uses the first persisted occurrence time when a stable event identity is re-entered.
     * All other content still passes through the store's exact-replay validation.
     */
    public RuntimeEventAppendResult recordAndPublishUsingFirstOccurrence(
            RuntimeEvent event) throws IOException {
        RuntimeEvent checked = Objects.requireNonNull(
                event, "event must not be null");
        RuntimeEvent replay = existingIdentity(checked)
                .map(existing -> new RuntimeEvent(
                        checked.schemaVersion(),
                        checked.eventId(),
                        checked.kind(),
                        existing.occurredAt(),
                        checked.binding(),
                        checked.agentRunId(),
                        checked.causationId(),
                        checked.producerId(),
                        checked.detail(),
                        checked.authoritativeReferences()))
                .orElse(checked);
        return recordAndPublish(replay);
    }

    private Optional<RuntimeEvent> existingIdentity(RuntimeEvent event)
            throws IOException {
        RuntimeEventStream stream;
        try {
            stream = store.resolve(event.binding().goalId());
        } catch (MissingRuntimeEventStreamException exception) {
            return Optional.empty();
        }
        return stream.events().stream()
                .filter(existing -> existing.eventId().equals(event.eventId()))
                .findFirst();
    }
}
