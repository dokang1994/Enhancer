package com.enhancer.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable bounded append-only stream for one Goal and retained WorkItem binding. */
public final class RuntimeEventStream {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int MAX_EVENTS = 4096;

    private final int schemaVersion;
    private final RuntimeEventBinding binding;
    private final long revision;
    private final List<RuntimeEvent> events;

    RuntimeEventStream(
            int schemaVersion,
            RuntimeEventBinding binding,
            long revision,
            List<RuntimeEvent> events) {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "runtime event stream schema version is unsupported");
        }
        this.schemaVersion = schemaVersion;
        this.binding = Objects.requireNonNull(
                binding,
                "binding must not be null");
        Objects.requireNonNull(events, "events must not be null");
        if (events.size() > MAX_EVENTS) {
            throw new IllegalArgumentException(
                    "runtime event stream exceeds its capacity");
        }
        if (revision != events.size()) {
            throw new IllegalArgumentException(
                    "runtime event stream revision must equal its event count");
        }
        List<RuntimeEvent> checked = new ArrayList<>();
        Set<String> identities = new HashSet<>();
        for (RuntimeEvent event : events) {
            RuntimeEvent value = Objects.requireNonNull(
                    event,
                    "events must not contain null");
            if (!binding.equals(value.binding())) {
                throw new IllegalArgumentException(
                        "runtime event binding does not match its stream");
            }
            if (!identities.add(value.eventId())) {
                throw new IllegalArgumentException(
                        "runtime event identities must be unique in a stream");
            }
            checked.add(value);
        }
        this.revision = revision;
        this.events = List.copyOf(checked);
    }

    public static RuntimeEventStream initial(RuntimeEventBinding binding) {
        return new RuntimeEventStream(
                CURRENT_SCHEMA_VERSION,
                binding,
                0,
                List.of());
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public RuntimeEventBinding binding() {
        return binding;
    }

    public long revision() {
        return revision;
    }

    public List<RuntimeEvent> events() {
        return events;
    }

    public RuntimeEventStream append(RuntimeEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        if (!binding.equals(event.binding())) {
            throw new IllegalArgumentException(
                    "runtime event binding does not match its stream");
        }
        for (RuntimeEvent existing : events) {
            if (!existing.eventId().equals(event.eventId())) {
                continue;
            }
            if (existing.equals(event)) {
                return this;
            }
            throw new IllegalArgumentException(
                    "runtime event identity was reused with changed content");
        }
        if (events.size() >= MAX_EVENTS) {
            throw new IllegalStateException(
                    "runtime event stream is at capacity");
        }
        List<RuntimeEvent> next = new ArrayList<>(events);
        next.add(event);
        return new RuntimeEventStream(
                schemaVersion,
                binding,
                revision + 1,
                next);
    }

    boolean isValidSuccessorOf(RuntimeEventStream current) {
        Objects.requireNonNull(current, "current must not be null");
        return binding.equals(current.binding)
                && revision == current.revision + 1
                && events.size() == current.events.size() + 1
                && events.subList(0, current.events.size())
                        .equals(current.events);
    }
}
