package com.enhancer.bus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A synchronous, single-threaded, deterministic in-process message bus over {@link
 * MessageEnvelope}. A topic publication fans out to every subscriber in registration order; a
 * queue publication is delivered point-to-point to a single consumer. Delivery is idempotent per
 * subscription and message identity, and an ordered journal supports deterministic replay without
 * duplicate side effects.
 *
 * <p>The bus carries whole envelopes without mutation, so authorization and provenance survive
 * every hop; it never creates authority. It provides no retry, cancellation propagation,
 * dead-letter, ordering beyond registration, backpressure, threading, persistence, or transport;
 * those remain later Delivery Gate 7 increments.
 */
public final class InProcessMessageBus {

    private record Subscription(String subscriberId, MessageHandler handler) {
    }

    private record DeliveryKey(
            DeliveryDestination destination, String subscriberId, String messageId) {
    }

    private final Map<DeliveryDestination, List<Subscription>> subscriptions =
            new LinkedHashMap<>();
    private final List<JournaledMessage> journal = new ArrayList<>();
    private final List<DeadLetter> deadLetters = new ArrayList<>();
    private final Set<DeliveryKey> delivered = new HashSet<>();

    /**
     * Registers a handler for a destination. A queue accepts only one consumer, and a subscriber
     * identity is unique per destination.
     */
    public void subscribe(
            DeliveryDestination destination, String subscriberId, MessageHandler handler) {
        Objects.requireNonNull(destination, "destination must not be null");
        String id = BusContractSupport.bounded(
                subscriberId, "subscriberId", BusContractSupport.MAX_IDENTITY_CHARACTERS);
        Objects.requireNonNull(handler, "handler must not be null");
        List<Subscription> registered =
                subscriptions.computeIfAbsent(destination, key -> new ArrayList<>());
        if (destination.kind() == DeliveryDestinationKind.QUEUE && !registered.isEmpty()) {
            throw new IllegalStateException(
                    "a queue accepts only one consumer: " + destination.name());
        }
        for (Subscription existing : registered) {
            if (existing.subscriberId().equals(id)) {
                throw new IllegalStateException(
                        "subscriberId already registered for destination: " + id);
            }
        }
        registered.add(new Subscription(id, handler));
    }

    /**
     * Publishes an envelope to a destination, records it in the journal, and dispatches it to the
     * destination's subscriptions in registration order.
     */
    public List<DeliveryOutcome> publish(DeliveryDestination destination, MessageEnvelope envelope) {
        Objects.requireNonNull(destination, "destination must not be null");
        Objects.requireNonNull(envelope, "envelope must not be null");
        journal.add(new JournaledMessage(destination, envelope));
        return dispatch(destination, envelope);
    }

    /**
     * Re-dispatches journal entries deterministically without appending to the journal. Idempotency
     * guarantees that entries already processed by this bus invoke no handler and report {@link
     * DeliveryStatus#DUPLICATE}.
     */
    public List<DeliveryOutcome> replay(List<JournaledMessage> entries) {
        Objects.requireNonNull(entries, "entries must not be null");
        List<DeliveryOutcome> outcomes = new ArrayList<>();
        for (JournaledMessage entry : entries) {
            Objects.requireNonNull(entry, "entries must not contain null");
            outcomes.addAll(dispatch(entry.destination(), entry.envelope()));
        }
        return List.copyOf(outcomes);
    }

    /** Returns the ordered, immutable journal of published messages. */
    public List<JournaledMessage> journal() {
        return List.copyOf(journal);
    }

    /** Returns the ordered, immutable record of deliveries whose handler threw. */
    public List<DeadLetter> deadLetters() {
        return List.copyOf(deadLetters);
    }

    private List<DeliveryOutcome> dispatch(
            DeliveryDestination destination, MessageEnvelope envelope) {
        List<Subscription> registered = subscriptions.getOrDefault(destination, List.of());
        if (registered.isEmpty()) {
            return List.of(new DeliveryOutcome(
                    destination, Optional.empty(), envelope.messageId(), DeliveryStatus.UNROUTED));
        }
        List<DeliveryOutcome> outcomes = new ArrayList<>();
        for (Subscription subscription : registered) {
            DeliveryKey key = new DeliveryKey(
                    destination, subscription.subscriberId(), envelope.messageId());
            DeliveryStatus status;
            if (delivered.add(key)) {
                status = deliver(destination, subscription, envelope);
            } else {
                status = DeliveryStatus.DUPLICATE;
            }
            outcomes.add(new DeliveryOutcome(
                    destination, Optional.of(subscription.subscriberId()),
                    envelope.messageId(), status));
        }
        return List.copyOf(outcomes);
    }

    private DeliveryStatus deliver(
            DeliveryDestination destination, Subscription subscription, MessageEnvelope envelope) {
        try {
            subscription.handler().handle(envelope);
            return DeliveryStatus.DELIVERED;
        } catch (RuntimeException failure) {
            deadLetters.add(new DeadLetter(
                    destination, subscription.subscriberId(), envelope, reasonOf(failure)));
            return DeliveryStatus.FAILED;
        }
    }

    private static String reasonOf(RuntimeException failure) {
        String message = failure.getMessage();
        String reason = message == null || message.isBlank()
                ? failure.getClass().getName()
                : message;
        return reason.length() <= DeadLetter.MAX_REASON_CHARACTERS
                ? reason
                : reason.substring(0, DeadLetter.MAX_REASON_CHARACTERS);
    }
}
