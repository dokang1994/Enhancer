package com.enhancer.bus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
 * <p>Each publication runs to completion before any publication it causes is delivered: a
 * publication made from inside a handler is queued and drained after the current fan-out
 * finishes, so delivery order equals publication order and no subscriber observes an effect
 * before its cause.
 *
 * <p>The bus carries whole envelopes without mutation, so authorization and provenance survive
 * every hop; it never creates authority. A handler failure is retried immediately under the bus's
 * bounded {@link RetryPolicy}, then isolated into a dead letter that supports explicit
 * re-delivery. A cancelled correlation is refused admission on every delivery path. The bus never
 * reads a payload to decide delivery, so a {@link ControlSignal#CANCEL} {@link ControlPayload} is
 * a consumer semantic that a handler may act on by calling {@link #cancel(String)} itself.
 *
 * <p>The pending queue has a finite {@link BackpressurePolicy}. Capacity exhaustion refuses the
 * attempted publication immediately and without side effects because blocking a handler inside
 * the single-threaded drain would deadlock. It provides no backoff, priority ordering, competing
 * consumers, threading, persistence, or transport; those remain later Delivery Gate 7 increments.
 */
public final class InProcessMessageBus {

    private record Subscription(String subscriberId, MessageHandler handler) {
    }

    private record DeliveryKey(
            DeliveryDestination destination, String subscriberId, String messageId) {
    }

    /** One submission awaiting admission. Replayed entries are dispatched but not journaled. */
    private record Pending(
            DeliveryDestination destination, MessageEnvelope envelope, boolean journal) {
    }

    private final Map<DeliveryDestination, List<Subscription>> subscriptions =
            new LinkedHashMap<>();
    private final List<JournaledMessage> journal = new ArrayList<>();
    private final List<DeadLetter> deadLetters = new ArrayList<>();
    private final Set<DeliveryKey> delivered = new HashSet<>();
    private final Set<String> cancelledCorrelations = new HashSet<>();
    private final Deque<Pending> pending = new ArrayDeque<>();
    private final RetryPolicy retryPolicy;
    private final BackpressurePolicy backpressurePolicy;
    private boolean draining;
    private boolean journalCausedPublications = true;

    /** Creates a bus that invokes every handler exactly once and never retries. */
    public InProcessMessageBus() {
        this(RetryPolicy.singleAttempt(), BackpressurePolicy.standard());
    }

    /** Creates a bus that retries a failing handler under the given bounded policy. */
    public InProcessMessageBus(RetryPolicy retryPolicy) {
        this(retryPolicy, BackpressurePolicy.standard());
    }

    /** Creates a bus with explicit bounded retry and pending-publication admission policies. */
    public InProcessMessageBus(
            RetryPolicy retryPolicy, BackpressurePolicy backpressurePolicy) {
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy must not be null");
        this.backpressurePolicy = Objects.requireNonNull(
                backpressurePolicy, "backpressurePolicy must not be null");
    }

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
     * Cancels a correlation. Every later delivery of an envelope carrying this correlation is
     * refused on every path, so one call reaches every topic, queue, replay, and re-delivery that
     * shares it. Cancellation is idempotent and monotonic: a cancelled correlation stays
     * cancelled, and resuming abandoned work requires a new correlation identity.
     */
    public void cancel(String correlationId) {
        cancelledCorrelations.add(BusContractSupport.bounded(
                correlationId, "correlationId", BusContractSupport.MAX_IDENTITY_CHARACTERS));
    }

    /** Returns whether this correlation has been cancelled. */
    public boolean isCancelled(String correlationId) {
        return cancelledCorrelations.contains(BusContractSupport.bounded(
                correlationId, "correlationId", BusContractSupport.MAX_IDENTITY_CHARACTERS));
    }

    /**
     * Publishes an envelope to a destination, records it in the journal, and dispatches it to the
     * destination's subscriptions in registration order.
     *
     * <p>A publication into a cancelled correlation is refused before it is journaled: it reports
     * {@link DeliveryStatus#CANCELLED}, invokes no handler, consumes no idempotency key, and
     * creates no dead letter. Journaling it would make a fresh-bus replay produce a side effect
     * that never originally happened, so the journal records only admitted publications.
     *
     * <p>Called from outside a handler this drains the pending queue to exhaustion and returns
     * the whole ordered cascade. Called from inside a handler it reports {@link
     * DeliveryStatus#ENQUEUED} when accepted or {@link DeliveryStatus#BACKPRESSURED} when the
     * bounded pending queue is full; resulting delivery outcomes for accepted work reach the
     * caller that is draining.
     */
    public List<DeliveryOutcome> publish(DeliveryDestination destination, MessageEnvelope envelope) {
        Objects.requireNonNull(destination, "destination must not be null");
        Objects.requireNonNull(envelope, "envelope must not be null");
        boolean journal = !draining || journalCausedPublications;
        return submit(List.of(new Pending(destination, envelope, journal)));
    }

    /**
     * Re-dispatches journal entries deterministically without appending to the journal. Idempotency
     * guarantees that entries already processed by this bus invoke no handler and report {@link
     * DeliveryStatus#DUPLICATE}, and an entry whose correlation is cancelled is refused and
     * reports {@link DeliveryStatus#CANCELLED} while entries in live correlations still deliver.
     * A batch larger than the pending capacity delivers the deterministic prefix that fits and
     * reports the remaining entries as {@link DeliveryStatus#BACKPRESSURED}.
     */
    public List<DeliveryOutcome> replay(List<JournaledMessage> entries) {
        Objects.requireNonNull(entries, "entries must not be null");
        List<Pending> submissions = new ArrayList<>();
        for (JournaledMessage entry : entries) {
            Objects.requireNonNull(entry, "entries must not contain null");
            submissions.add(new Pending(entry.destination(), entry.envelope(), false));
        }
        return submit(submissions);
    }

    /** Returns the ordered, immutable journal of published messages. */
    public List<JournaledMessage> journal() {
        return List.copyOf(journal);
    }

    /** Returns the ordered, immutable record of deliveries whose handler threw. */
    public List<DeadLetter> deadLetters() {
        return List.copyOf(deadLetters);
    }

    /**
     * Explicitly re-delivers a dead letter this bus currently records, under the same bounded
     * attempt policy. Success resolves the dead letter; renewed exhaustion replaces it in place
     * with the accumulated attempt count and the latest reason. Re-delivery never appends to the
     * journal and never releases the consumed idempotency key, so publish and replay still report
     * {@link DeliveryStatus#DUPLICATE}.
     *
     * <p>A dead letter whose correlation is cancelled is refused: it reports {@link
     * DeliveryStatus#CANCELLED}, invokes no handler, and keeps its record, because the failure
     * still happened even though the work was abandoned.
     */
    public DeliveryOutcome redeliver(DeadLetter deadLetter) {
        Objects.requireNonNull(deadLetter, "deadLetter must not be null");
        int index = deadLetters.indexOf(deadLetter);
        if (index < 0) {
            throw new IllegalStateException("dead letter is not recorded by this bus");
        }
        if (isCancelled(deadLetter.envelope().correlationId())) {
            return cancelled(deadLetter.destination(), deadLetter.envelope());
        }
        MessageHandler handler = handlerOf(deadLetter.destination(), deadLetter.subscriberId());
        RuntimeException failure = attemptDelivery(handler, deadLetter.envelope());
        DeliveryStatus status;
        if (failure == null) {
            deadLetters.remove(index);
            status = DeliveryStatus.DELIVERED;
        } else {
            deadLetters.set(index, new DeadLetter(
                    deadLetter.destination(), deadLetter.subscriberId(), deadLetter.envelope(),
                    reasonOf(failure), deadLetter.attempts() + retryPolicy.maxAttempts()));
            status = DeliveryStatus.FAILED;
        }
        return new DeliveryOutcome(
                deadLetter.destination(), Optional.of(deadLetter.subscriberId()),
                deadLetter.envelope().messageId(), status);
    }

    /**
     * Queues submissions in publication order up to the configured capacity. A submission made
     * while a drain is already running reports {@link DeliveryStatus#ENQUEUED} when accepted or
     * {@link DeliveryStatus#BACKPRESSURED} when refused; the running drain delivers accepted work
     * once the current publication has run to completion.
     */
    private List<DeliveryOutcome> submit(List<Pending> submissions) {
        List<DeliveryOutcome> submissionOutcomes = new ArrayList<>();
        List<DeliveryOutcome> refused = new ArrayList<>();
        for (Pending submission : submissions) {
            DeliveryOutcome outcome;
            if (pending.size() >= backpressurePolicy.maxPendingPublications()) {
                outcome = backpressured(submission.destination(), submission.envelope());
                refused.add(outcome);
            } else {
                pending.addLast(submission);
                outcome = new DeliveryOutcome(
                        submission.destination(), Optional.empty(),
                        submission.envelope().messageId(), DeliveryStatus.ENQUEUED);
            }
            submissionOutcomes.add(outcome);
        }
        if (draining) {
            return List.copyOf(submissionOutcomes);
        }
        List<DeliveryOutcome> delivered = drain();
        if (refused.isEmpty()) {
            return delivered;
        }
        List<DeliveryOutcome> outcomes = new ArrayList<>(delivered.size() + refused.size());
        outcomes.addAll(delivered);
        outcomes.addAll(refused);
        return List.copyOf(outcomes);
    }

    /**
     * Admits and dispatches queued submissions until the queue is exhausted, returning the whole
     * ordered cascade. Admission happens here rather than at the publishing call so the journal's
     * order is the bus's own delivery order and a correlation cancelled mid-cascade still refuses
     * entries queued behind it. An {@link Error} abandons the cascade entirely rather than leaking
     * queued entries into a later publication.
     */
    private List<DeliveryOutcome> drain() {
        draining = true;
        try {
            List<DeliveryOutcome> outcomes = new ArrayList<>();
            while (!pending.isEmpty()) {
                Pending next = pending.removeFirst();
                if (isCancelled(next.envelope().correlationId())) {
                    outcomes.add(cancelled(next.destination(), next.envelope()));
                    continue;
                }
                if (next.journal()) {
                    journal.add(new JournaledMessage(next.destination(), next.envelope()));
                }
                boolean previousJournalMode = journalCausedPublications;
                journalCausedPublications = next.journal();
                try {
                    outcomes.addAll(dispatch(next.destination(), next.envelope()));
                } finally {
                    journalCausedPublications = previousJournalMode;
                }
            }
            return List.copyOf(outcomes);
        } finally {
            draining = false;
            journalCausedPublications = true;
            pending.clear();
        }
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
        RuntimeException failure = attemptDelivery(subscription.handler(), envelope);
        if (failure == null) {
            return DeliveryStatus.DELIVERED;
        }
        deadLetters.add(new DeadLetter(
                destination, subscription.subscriberId(), envelope, reasonOf(failure),
                retryPolicy.maxAttempts()));
        return DeliveryStatus.FAILED;
    }

    /**
     * Invokes the handler until it succeeds or the bounded policy is exhausted, immediately and
     * with no delay between attempts. Returns null on success, or the last failure.
     */
    private RuntimeException attemptDelivery(MessageHandler handler, MessageEnvelope envelope) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= retryPolicy.maxAttempts(); attempt++) {
            try {
                handler.handle(envelope);
                return null;
            } catch (RuntimeException failure) {
                lastFailure = failure;
            }
        }
        return lastFailure;
    }

    private MessageHandler handlerOf(DeliveryDestination destination, String subscriberId) {
        for (Subscription subscription : subscriptions.getOrDefault(destination, List.of())) {
            if (subscription.subscriberId().equals(subscriberId)) {
                return subscription.handler();
            }
        }
        throw new IllegalStateException(
                "no subscription exists for the dead letter: " + subscriberId);
    }

    private static DeliveryOutcome cancelled(
            DeliveryDestination destination, MessageEnvelope envelope) {
        return new DeliveryOutcome(
                destination, Optional.empty(), envelope.messageId(), DeliveryStatus.CANCELLED);
    }

    private static DeliveryOutcome backpressured(
            DeliveryDestination destination, MessageEnvelope envelope) {
        return new DeliveryOutcome(
                destination, Optional.empty(), envelope.messageId(),
                DeliveryStatus.BACKPRESSURED);
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
