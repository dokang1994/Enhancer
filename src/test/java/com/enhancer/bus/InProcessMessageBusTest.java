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
                        topic, "x", envelope("00000000-0000-0000-0000-00000000000c"), "r")));
    }

    private MessageEnvelope envelope(String messageId) {
        WorkPayload work = new WorkPayload(TASK_REVISION, SNAPSHOT_ID, Set.of("read-file"));
        return new MessageEnvelope(
                messageId,
                "correlation-1",
                Optional.empty(),
                "logical-run-1",
                "agent-loop",
                OCCURRED_AT,
                work);
    }
}
