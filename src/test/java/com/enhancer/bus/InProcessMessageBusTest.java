package com.enhancer.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.workspace.ApprovedTaskRevision;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InProcessMessageBusTest {
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-15T14:00:00Z");
    private static final String SNAPSHOT_ID = "a".repeat(64);
    private static final ApprovedTaskRevision TASK_REVISION = new ApprovedTaskRevision(
            "gate-7-in-process-delivery",
            "CURRENT_TASK.md",
            "f".repeat(64));

    @Test
    void fansOutTopicToSubscribersInRegistrationOrder() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");
        List<String> order = new ArrayList<>();
        bus.subscribe(topic, "first", envelope -> order.add("first:" + envelope.messageId()));
        bus.subscribe(topic, "second", envelope -> order.add("second:" + envelope.messageId()));

        MessageEnvelope envelope = envelope("00000000-0000-0000-0000-00000000000a");
        List<DeliveryOutcome> outcomes = bus.publish(topic, envelope);

        assertEquals(
                List.of(
                        "first:00000000-0000-0000-0000-00000000000a",
                        "second:00000000-0000-0000-0000-00000000000a"),
                order);
        assertEquals(2, outcomes.size());
        assertEquals(Optional.of("first"), outcomes.get(0).subscriberId());
        assertEquals(DeliveryStatus.DELIVERED, outcomes.get(0).status());
        assertEquals(Optional.of("second"), outcomes.get(1).subscriberId());
        assertEquals(DeliveryStatus.DELIVERED, outcomes.get(1).status());
    }

    @Test
    void deliversQueuePointToPointAndRejectsSecondConsumer() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination queue = DeliveryDestination.queue("worker-tasks");
        List<String> received = new ArrayList<>();
        bus.subscribe(queue, "worker", envelope -> received.add(envelope.messageId()));

        List<DeliveryOutcome> outcomes = bus.publish(
                queue, envelope("00000000-0000-0000-0000-00000000000a"));

        assertEquals(1, outcomes.size());
        assertEquals(DeliveryStatus.DELIVERED, outcomes.get(0).status());
        assertEquals(List.of("00000000-0000-0000-0000-00000000000a"), received);
        assertThrows(IllegalStateException.class, () ->
                bus.subscribe(queue, "second-worker", envelope -> { }));
    }

    @Test
    void reportsUnroutedWhenNoSubscriberExists() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination topic = DeliveryDestination.topic("empty");

        List<DeliveryOutcome> outcomes = bus.publish(
                topic, envelope("00000000-0000-0000-0000-00000000000a"));

        assertEquals(1, outcomes.size());
        assertEquals(DeliveryStatus.UNROUTED, outcomes.get(0).status());
        assertEquals(Optional.empty(), outcomes.get(0).subscriberId());
    }

    @Test
    void isIdempotentForRepeatedMessageId() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");
        List<String> received = new ArrayList<>();
        bus.subscribe(topic, "worker", envelope -> received.add(envelope.messageId()));

        MessageEnvelope envelope = envelope("00000000-0000-0000-0000-00000000000a");
        DeliveryStatus first = bus.publish(topic, envelope).get(0).status();
        DeliveryStatus second = bus.publish(topic, envelope).get(0).status();

        assertEquals(DeliveryStatus.DELIVERED, first);
        assertEquals(DeliveryStatus.DUPLICATE, second);
        assertEquals(List.of("00000000-0000-0000-0000-00000000000a"), received);
    }

    @Test
    void replayReproducesOutcomesOnFreshBusWithoutDuplicateSideEffects() {
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");
        InProcessMessageBus source = new InProcessMessageBus();
        source.subscribe(topic, "worker", envelope -> { });
        source.publish(topic, envelope("00000000-0000-0000-0000-00000000000a"));
        source.publish(topic, envelope("00000000-0000-0000-0000-00000000000b"));

        List<JournaledMessage> journal = source.journal();
        assertEquals(2, journal.size());

        InProcessMessageBus replayed = new InProcessMessageBus();
        List<String> received = new ArrayList<>();
        replayed.subscribe(topic, "worker", envelope -> received.add(envelope.messageId()));

        List<DeliveryOutcome> firstReplay = replayed.replay(journal);
        assertEquals(2, firstReplay.size());
        assertTrue(firstReplay.stream()
                .allMatch(outcome -> outcome.status() == DeliveryStatus.DELIVERED));
        assertEquals(
                List.of(
                        "00000000-0000-0000-0000-00000000000a",
                        "00000000-0000-0000-0000-00000000000b"),
                received);
        assertTrue(replayed.journal().isEmpty(), "replay must not append to the journal");

        List<DeliveryOutcome> secondReplay = replayed.replay(journal);
        assertTrue(secondReplay.stream()
                .allMatch(outcome -> outcome.status() == DeliveryStatus.DUPLICATE));
        assertEquals(2, received.size(), "replay must not repeat side effects");
    }

    @Test
    void preservesAuthorizationAndProvenanceAcrossHop() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination topic = DeliveryDestination.topic("work");
        List<MessageEnvelope> received = new ArrayList<>();
        bus.subscribe(topic, "worker", received::add);

        WorkPayload work = new WorkPayload(TASK_REVISION, SNAPSHOT_ID, Set.of("read-file"));
        MessageEnvelope envelope = new MessageEnvelope(
                "00000000-0000-0000-0000-00000000000a",
                "correlation-1",
                Optional.of("00000000-0000-0000-0000-00000000000b"),
                "logical-run-1",
                "agent-loop",
                OCCURRED_AT,
                work);
        bus.publish(topic, envelope);

        assertEquals(1, received.size());
        MessageEnvelope delivered = received.get(0);
        assertSame(envelope, delivered);
        assertSame(work, delivered.payload());
        assertEquals(Set.of("read-file"), ((WorkPayload) delivered.payload()).allowedTools());
        assertEquals(TASK_REVISION, ((WorkPayload) delivered.payload()).taskRevision());
        assertEquals("correlation-1", delivered.correlationId());
        assertEquals(Optional.of("00000000-0000-0000-0000-00000000000b"),
                delivered.causationId());
    }

    @Test
    void validatesDestinationSubscriberAndJournalInvariants() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");

        assertThrows(IllegalArgumentException.class, () -> DeliveryDestination.topic(" "));
        assertThrows(IllegalArgumentException.class, () -> DeliveryDestination.queue(""));
        assertThrows(NullPointerException.class, () -> DeliveryDestination.topic(null));
        assertEquals(DeliveryDestination.topic("x"), DeliveryDestination.topic("x"));
        assertFalse(DeliveryDestination.topic("x").equals(DeliveryDestination.queue("x")));

        assertThrows(IllegalArgumentException.class, () ->
                bus.subscribe(topic, " ", envelope -> { }));
        assertThrows(NullPointerException.class, () ->
                bus.subscribe(topic, "worker", null));
        assertThrows(NullPointerException.class, () ->
                bus.publish(topic, null));

        bus.subscribe(topic, "worker", envelope -> { });
        bus.publish(topic, envelope("00000000-0000-0000-0000-00000000000a"));
        assertThrows(UnsupportedOperationException.class, () ->
                bus.journal().add(new JournaledMessage(
                        topic, envelope("00000000-0000-0000-0000-00000000000c"))));
    }

    @Test
    void isolatesHandlerFailureAndContinuesFanOut() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");
        List<String> received = new ArrayList<>();
        bus.subscribe(topic, "failing", envelope -> {
            throw new IllegalStateException("handler exploded");
        });
        bus.subscribe(topic, "healthy", envelope -> received.add(envelope.messageId()));

        MessageEnvelope envelope = envelope("00000000-0000-0000-0000-00000000000a");
        List<DeliveryOutcome> outcomes = bus.publish(topic, envelope);

        assertEquals(2, outcomes.size());
        assertEquals(DeliveryStatus.FAILED, outcomes.get(0).status());
        assertEquals(Optional.of("failing"), outcomes.get(0).subscriberId());
        assertEquals(DeliveryStatus.DELIVERED, outcomes.get(1).status());
        assertEquals(List.of("00000000-0000-0000-0000-00000000000a"), received);

        List<DeadLetter> deadLetters = bus.deadLetters();
        assertEquals(1, deadLetters.size());
        assertEquals(topic, deadLetters.get(0).destination());
        assertEquals("failing", deadLetters.get(0).subscriberId());
        assertSame(envelope, deadLetters.get(0).envelope());
        assertTrue(deadLetters.get(0).reason().contains("handler exploded"));
    }

    @Test
    void treatsAFailedDeliveryAsIdempotentAndTerminal() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");
        List<String> attempts = new ArrayList<>();
        bus.subscribe(topic, "failing", envelope -> {
            attempts.add(envelope.messageId());
            throw new IllegalStateException("handler exploded");
        });

        MessageEnvelope envelope = envelope("00000000-0000-0000-0000-00000000000a");
        assertEquals(DeliveryStatus.FAILED, bus.publish(topic, envelope).get(0).status());
        assertEquals(DeliveryStatus.DUPLICATE, bus.publish(topic, envelope).get(0).status());

        assertEquals(1, attempts.size(), "a failed delivery must not be automatically retried");
        assertEquals(1, bus.deadLetters().size(), "a duplicate must not add another dead letter");
    }

    @Test
    void deadLettersAreOrderedImmutableAndBoundedInReason() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");
        bus.subscribe(topic, "failing", envelope -> {
            throw new IllegalStateException((String) null);
        });

        bus.publish(topic, envelope("00000000-0000-0000-0000-00000000000a"));
        bus.publish(topic, envelope("00000000-0000-0000-0000-00000000000b"));

        List<DeadLetter> deadLetters = bus.deadLetters();
        assertEquals(2, deadLetters.size());
        assertEquals("00000000-0000-0000-0000-00000000000a",
                deadLetters.get(0).envelope().messageId());
        assertEquals("00000000-0000-0000-0000-00000000000b",
                deadLetters.get(1).envelope().messageId());
        assertFalse(deadLetters.get(0).reason().isBlank(),
                "a null exception message must fall back to the exception type");
        assertThrows(UnsupportedOperationException.class, () ->
                deadLetters.add(new DeadLetter(
                        topic, "x", envelope("00000000-0000-0000-0000-00000000000c"), "r", 1)));
    }

    @Test
    void retriesAFailingHandlerWithinTheBoundedAttemptPolicy() {
        InProcessMessageBus bus = new InProcessMessageBus(RetryPolicy.of(3));
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");
        List<String> attempts = new ArrayList<>();
        bus.subscribe(topic, "flaky", envelope -> {
            attempts.add(envelope.messageId());
            if (attempts.size() < 3) {
                throw new IllegalStateException("transient failure");
            }
        });

        List<DeliveryOutcome> outcomes = bus.publish(
                topic, envelope("00000000-0000-0000-0000-00000000000a"));

        assertEquals(DeliveryStatus.DELIVERED, outcomes.get(0).status());
        assertEquals(3, attempts.size(), "the handler must be retried immediately within the policy");
        assertTrue(bus.deadLetters().isEmpty(),
                "a delivery that succeeds within the policy must not be dead-lettered");
    }

    @Test
    void deadLettersWithTheAttemptCountAfterExhaustingThePolicy() {
        InProcessMessageBus bus = new InProcessMessageBus(RetryPolicy.of(2));
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");
        List<String> attempts = new ArrayList<>();
        List<String> received = new ArrayList<>();
        bus.subscribe(topic, "failing", envelope -> {
            attempts.add(envelope.messageId());
            throw new IllegalStateException("handler exploded");
        });
        bus.subscribe(topic, "healthy", envelope -> received.add(envelope.messageId()));

        List<DeliveryOutcome> outcomes = bus.publish(
                topic, envelope("00000000-0000-0000-0000-00000000000a"));

        assertEquals(DeliveryStatus.FAILED, outcomes.get(0).status());
        assertEquals(DeliveryStatus.DELIVERED, outcomes.get(1).status());
        assertEquals(2, attempts.size(), "the policy must bound the attempts");
        assertEquals(List.of("00000000-0000-0000-0000-00000000000a"), received);
        assertEquals(1, bus.deadLetters().size());
        assertEquals(2, bus.deadLetters().get(0).attempts(),
                "the dead letter must record how many attempts failed");
    }

    @Test
    void redeliversADeadLetterAndResolvesItOnSuccess() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");
        List<Boolean> healthy = new ArrayList<>();
        List<String> received = new ArrayList<>();
        bus.subscribe(topic, "recovering", envelope -> {
            if (healthy.isEmpty()) {
                throw new IllegalStateException("still starting");
            }
            received.add(envelope.messageId());
        });

        MessageEnvelope envelope = envelope("00000000-0000-0000-0000-00000000000a");
        assertEquals(DeliveryStatus.FAILED, bus.publish(topic, envelope).get(0).status());
        DeadLetter deadLetter = bus.deadLetters().get(0);

        healthy.add(Boolean.TRUE);
        DeliveryOutcome outcome = bus.redeliver(deadLetter);

        assertEquals(DeliveryStatus.DELIVERED, outcome.status());
        assertEquals(Optional.of("recovering"), outcome.subscriberId());
        assertEquals(List.of("00000000-0000-0000-0000-00000000000a"), received);
        assertTrue(bus.deadLetters().isEmpty(), "a successful re-delivery must resolve the dead letter");
        assertEquals(1, bus.journal().size(), "re-delivery must not append to the journal");
        assertEquals(DeliveryStatus.DUPLICATE, bus.publish(topic, envelope).get(0).status(),
                "the consumed idempotency key must stay consumed after re-delivery");
        assertThrows(IllegalStateException.class, () -> bus.redeliver(deadLetter),
                "a resolved dead letter must not be re-delivered again");
    }

    @Test
    void keepsAFailedRedeliveryDeadLetteredWithAccumulatedAttempts() {
        InProcessMessageBus bus = new InProcessMessageBus(RetryPolicy.of(2));
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");
        List<String> attempts = new ArrayList<>();
        bus.subscribe(topic, "failing", envelope -> {
            attempts.add(envelope.messageId());
            throw new IllegalStateException("attempt " + attempts.size());
        });

        bus.publish(topic, envelope("00000000-0000-0000-0000-00000000000a"));
        DeadLetter deadLetter = bus.deadLetters().get(0);
        assertEquals(2, deadLetter.attempts());

        DeliveryOutcome outcome = bus.redeliver(deadLetter);

        assertEquals(DeliveryStatus.FAILED, outcome.status());
        assertEquals(4, attempts.size(), "re-delivery must apply the same bounded policy");
        assertEquals(1, bus.deadLetters().size(),
                "a failed re-delivery must keep exactly one dead letter for the delivery");
        assertEquals(4, bus.deadLetters().get(0).attempts(),
                "the dead letter must accumulate the failed attempts");
        assertTrue(bus.deadLetters().get(0).reason().contains("attempt 4"),
                "the dead letter must carry the latest failure reason");
    }

    @Test
    void validatesRetryPolicyAndRedeliveryInvariants() {
        assertThrows(IllegalArgumentException.class, () -> RetryPolicy.of(0));
        assertThrows(IllegalArgumentException.class, () -> RetryPolicy.of(11));
        assertEquals(1, RetryPolicy.singleAttempt().maxAttempts());

        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");
        bus.subscribe(topic, "worker", envelope -> { });

        assertThrows(NullPointerException.class, () ->
                new InProcessMessageBus(null));
        assertThrows(NullPointerException.class, () -> bus.redeliver(null));
        assertThrows(IllegalStateException.class, () -> bus.redeliver(new DeadLetter(
                topic, "worker", envelope("00000000-0000-0000-0000-00000000000c"), "foreign", 1)),
                "a dead letter this bus does not record must be rejected");
        assertThrows(IllegalArgumentException.class, () -> new DeadLetter(
                topic, "worker", envelope("00000000-0000-0000-0000-00000000000c"), "r", 0),
                "a dead letter must record at least one failed attempt");
    }

    @Test
    void refusesAPublicationIntoACancelledCorrelationWithoutAnySideEffect() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");
        List<String> received = new ArrayList<>();
        bus.subscribe(topic, "worker", envelope -> received.add(envelope.messageId()));

        bus.cancel("correlation-1");

        List<DeliveryOutcome> outcomes = bus.publish(
                topic, envelope("00000000-0000-0000-0000-00000000000a"));

        assertEquals(1, outcomes.size());
        assertEquals(DeliveryStatus.CANCELLED, outcomes.get(0).status());
        assertEquals(Optional.empty(), outcomes.get(0).subscriberId(),
                "a scope-level refusal targets no subscription");
        assertTrue(received.isEmpty(), "a cancelled publication must invoke no handler");
        assertTrue(bus.deadLetters().isEmpty(), "a cancelled publication must not be dead-lettered");
        assertTrue(bus.journal().isEmpty(),
                "a cancelled publication must not be journaled, so replay stays deterministic");
        assertTrue(bus.isCancelled("correlation-1"));
    }

    @Test
    void cancellationDominatesUnroutedAndDuplicate() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination unrouted = DeliveryDestination.topic("empty");
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");
        List<String> received = new ArrayList<>();
        bus.subscribe(topic, "worker", envelope -> received.add(envelope.messageId()));

        MessageEnvelope envelope = envelope("00000000-0000-0000-0000-00000000000a");
        assertEquals(DeliveryStatus.DELIVERED, bus.publish(topic, envelope).get(0).status());

        bus.cancel("correlation-1");

        assertEquals(DeliveryStatus.CANCELLED, bus.publish(topic, envelope).get(0).status(),
                "cancellation must dominate an already-consumed idempotency key");
        assertEquals(DeliveryStatus.CANCELLED, bus.publish(unrouted, envelope).get(0).status(),
                "cancellation must dominate an absent subscription");
        assertEquals(1, received.size());
        assertEquals(1, bus.journal().size(), "only the admitted publication is journaled");
    }

    @Test
    void propagatesCancellationToReplayWhileLiveCorrelationsStillDeliver() {
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");
        InProcessMessageBus source = new InProcessMessageBus();
        source.subscribe(topic, "worker", envelope -> { });
        source.publish(topic, envelope("00000000-0000-0000-0000-00000000000a"));
        source.publish(topic, envelopeIn(
                "00000000-0000-0000-0000-00000000000b", "correlation-2"));
        List<JournaledMessage> journal = source.journal();

        InProcessMessageBus replayed = new InProcessMessageBus();
        List<String> received = new ArrayList<>();
        replayed.subscribe(topic, "worker", envelope -> received.add(envelope.messageId()));
        replayed.cancel("correlation-1");

        List<DeliveryOutcome> outcomes = replayed.replay(journal);

        assertEquals(2, outcomes.size());
        assertEquals(DeliveryStatus.CANCELLED, outcomes.get(0).status());
        assertEquals(DeliveryStatus.DELIVERED, outcomes.get(1).status());
        assertEquals(List.of("00000000-0000-0000-0000-00000000000b"), received,
                "only the live correlation may take effect on replay");
    }

    @Test
    void propagatesCancellationToRedeliveryAndRetainsTheDeadLetter() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");
        List<String> attempts = new ArrayList<>();
        bus.subscribe(topic, "failing", envelope -> {
            attempts.add(envelope.messageId());
            throw new IllegalStateException("handler exploded");
        });

        bus.publish(topic, envelope("00000000-0000-0000-0000-00000000000a"));
        DeadLetter deadLetter = bus.deadLetters().get(0);
        assertEquals(1, attempts.size());

        bus.cancel("correlation-1");
        DeliveryOutcome outcome = bus.redeliver(deadLetter);

        assertEquals(DeliveryStatus.CANCELLED, outcome.status());
        assertEquals(Optional.empty(), outcome.subscriberId());
        assertEquals(1, attempts.size(),
                "a cancelled correlation must not re-invoke the handler");
        assertEquals(List.of(deadLetter), bus.deadLetters(),
                "a cancelled re-delivery must retain the dead-letter record unchanged");
    }

    @Test
    void keepsCancellationIdempotentMonotonicAndScopedToItsCorrelation() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");
        List<String> received = new ArrayList<>();
        bus.subscribe(topic, "worker", envelope -> received.add(envelope.messageId()));

        assertFalse(bus.isCancelled("correlation-1"));
        bus.cancel("correlation-1");
        bus.cancel("correlation-1");
        assertTrue(bus.isCancelled("correlation-1"), "cancellation must be idempotent and terminal");
        assertFalse(bus.isCancelled("correlation-2"),
                "cancellation must not leak to another correlation");

        assertEquals(DeliveryStatus.DELIVERED, bus.publish(
                topic, envelopeIn("00000000-0000-0000-0000-00000000000b", "correlation-2"))
                .get(0).status());
        assertEquals(List.of("00000000-0000-0000-0000-00000000000b"), received);

        assertThrows(IllegalArgumentException.class, () -> bus.cancel(" "));
        assertThrows(NullPointerException.class, () -> bus.cancel(null));
        assertThrows(NullPointerException.class, () -> bus.isCancelled(null));
        assertThrows(IllegalArgumentException.class, () -> new DeliveryOutcome(
                topic, Optional.of("worker"), "00000000-0000-0000-0000-00000000000a",
                DeliveryStatus.CANCELLED),
                "a CANCELLED outcome must not carry a subscriberId");
    }

    @Test
    void deliversACascadeOnlyAfterTheCurrentFanOutCompletes() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination parent = DeliveryDestination.topic("parent-events");
        DeliveryDestination child = DeliveryDestination.topic("child-events");
        MessageEnvelope childEnvelope = envelope("00000000-0000-0000-0000-00000000000b");
        List<String> order = new ArrayList<>();

        bus.subscribe(child, "childWorker", envelope -> order.add("child"));
        bus.subscribe(parent, "first", envelope -> {
            order.add("first");
            bus.publish(child, childEnvelope);
        });
        bus.subscribe(parent, "second", envelope -> order.add("second"));

        List<DeliveryOutcome> outcomes = bus.publish(
                parent, envelope("00000000-0000-0000-0000-00000000000a"));

        assertEquals(List.of("first", "second", "child"), order,
                "no subscriber may observe an effect before its cause");
        assertEquals(3, outcomes.size(), "the draining call must report the whole cascade");
        assertEquals(Optional.of("first"), outcomes.get(0).subscriberId());
        assertEquals(Optional.of("second"), outcomes.get(1).subscriberId());
        assertEquals(Optional.of("childWorker"), outcomes.get(2).subscriberId());
        assertTrue(outcomes.stream()
                .allMatch(outcome -> outcome.status() == DeliveryStatus.DELIVERED));
        assertEquals(List.of(parent, child),
                bus.journal().stream().map(JournaledMessage::destination).toList(),
                "the journal records admission order");
    }

    @Test
    void reportsAReEntrantPublicationAsEnqueued() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination parent = DeliveryDestination.topic("parent-events");
        DeliveryDestination child = DeliveryDestination.topic("child-events");
        MessageEnvelope childEnvelope = envelope("00000000-0000-0000-0000-00000000000b");
        List<DeliveryOutcome> nested = new ArrayList<>();

        bus.subscribe(child, "childWorker", envelope -> { });
        bus.subscribe(parent, "worker", envelope -> nested.addAll(bus.publish(child, childEnvelope)));

        bus.publish(parent, envelope("00000000-0000-0000-0000-00000000000a"));

        assertEquals(1, nested.size());
        assertEquals(DeliveryStatus.ENQUEUED, nested.get(0).status());
        assertEquals(Optional.empty(), nested.get(0).subscriberId(),
                "an enqueued publication has targeted no subscription yet");
        assertEquals("00000000-0000-0000-0000-00000000000b", nested.get(0).messageId());
    }

    @Test
    void queuesCascadedPublicationsInFifoPublicationOrder() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination parent = DeliveryDestination.topic("parent-events");
        DeliveryDestination child = DeliveryDestination.topic("child-events");
        List<String> order = new ArrayList<>();

        bus.subscribe(child, "childWorker", envelope -> order.add(envelope.messageId()));
        bus.subscribe(parent, "first", envelope ->
                bus.publish(child, envelope("00000000-0000-0000-0000-00000000000b")));
        bus.subscribe(parent, "second", envelope ->
                bus.publish(child, envelope("00000000-0000-0000-0000-00000000000c")));

        bus.publish(parent, envelope("00000000-0000-0000-0000-00000000000a"));

        assertEquals(
                List.of(
                        "00000000-0000-0000-0000-00000000000b",
                        "00000000-0000-0000-0000-00000000000c"),
                order,
                "cascaded publications must drain in FIFO publication order");
    }

    @Test
    void refusesAQueuedPublicationCancelledDuringTheCascade() {
        InProcessMessageBus bus = new InProcessMessageBus();
        DeliveryDestination parent = DeliveryDestination.topic("parent-events");
        DeliveryDestination child = DeliveryDestination.topic("child-events");
        MessageEnvelope childEnvelope = envelope("00000000-0000-0000-0000-00000000000b");
        List<String> received = new ArrayList<>();

        bus.subscribe(child, "childWorker", envelope -> received.add("child"));
        bus.subscribe(parent, "first", envelope -> {
            received.add("first");
            bus.publish(child, childEnvelope);
            bus.cancel("correlation-1");
        });
        bus.subscribe(parent, "second", envelope -> received.add("second"));

        List<DeliveryOutcome> outcomes = bus.publish(
                parent, envelope("00000000-0000-0000-0000-00000000000a"));

        assertEquals(List.of("first", "second"), received,
                "an in-flight fan-out completes atomically and the queued child is refused");
        assertEquals(DeliveryStatus.CANCELLED, outcomes.get(outcomes.size() - 1).status());
        assertEquals(List.of(parent),
                bus.journal().stream().map(JournaledMessage::destination).toList(),
                "a refused cascade entry must not be journaled");
    }

    @Test
    void classifiesScopeLevelStatusesAndValidatesEnqueuedOutcomes() {
        DeliveryDestination topic = DeliveryDestination.topic("runtime-events");

        assertTrue(DeliveryStatus.UNROUTED.isScopeLevel());
        assertTrue(DeliveryStatus.CANCELLED.isScopeLevel());
        assertTrue(DeliveryStatus.ENQUEUED.isScopeLevel());
        assertFalse(DeliveryStatus.DELIVERED.isScopeLevel());
        assertFalse(DeliveryStatus.DUPLICATE.isScopeLevel());
        assertFalse(DeliveryStatus.FAILED.isScopeLevel());

        assertThrows(IllegalArgumentException.class, () -> new DeliveryOutcome(
                topic, Optional.of("worker"), "00000000-0000-0000-0000-00000000000a",
                DeliveryStatus.ENQUEUED),
                "an ENQUEUED outcome must not carry a subscriberId");
        assertThrows(IllegalArgumentException.class, () -> new DeliveryOutcome(
                topic, Optional.empty(), "00000000-0000-0000-0000-00000000000a",
                DeliveryStatus.DELIVERED),
                "a DELIVERED outcome must name the subscription it targeted");
    }

    private MessageEnvelope envelope(String messageId) {
        return envelopeIn(messageId, "correlation-1");
    }

    private MessageEnvelope envelopeIn(String messageId, String correlationId) {
        WorkPayload work = new WorkPayload(TASK_REVISION, SNAPSHOT_ID, Set.of("read-file"));
        return new MessageEnvelope(
                messageId,
                correlationId,
                Optional.empty(),
                "logical-run-1",
                "agent-loop",
                OCCURRED_AT,
                work);
    }
}
