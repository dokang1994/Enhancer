package com.enhancer.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.workspace.ApprovedTaskRevision;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MessageTransportTest {
    private static final ApprovedTaskRevision TASK_REVISION = new ApprovedTaskRevision(
            "gate-7-transport-neutral-ipc-interface",
            "CURRENT_TASK.md",
            "f".repeat(64));

    @Test
    void carriesTheExistingRouteAndEnvelopeWithoutMutation() {
        DeliveryDestination destination = DeliveryDestination.queue("worker-tasks");
        MessageEnvelope envelope = envelope();

        TransportMessage message = new TransportMessage(destination, envelope);

        assertSame(destination, message.destination());
        assertSame(envelope, message.envelope());
        assertSame(envelope.payload(), message.envelope().payload());
        assertThrows(NullPointerException.class, () -> new TransportMessage(null, envelope));
        assertThrows(NullPointerException.class, () -> new TransportMessage(destination, null));
    }

    @Test
    void exposesOneProviderNeutralSendOperation() throws NoSuchMethodException {
        AtomicReference<TransportMessage> captured = new AtomicReference<>();
        MessageTransport transport = message -> {
            captured.set(message);
            return TransportOutcome.accepted();
        };
        TransportMessage message = new TransportMessage(
                DeliveryDestination.topic("runtime-events"), envelope());

        TransportOutcome outcome = transport.send(message);

        assertSame(message, captured.get());
        assertEquals(TransportStatus.ACCEPTED, outcome.status());
        Method send = MessageTransport.class.getMethod("send", TransportMessage.class);
        assertEquals(TransportOutcome.class, send.getReturnType());
        assertEquals(1, MessageTransport.class.getDeclaredMethods().length);
    }

    @Test
    void separatesHopAcceptanceFromMessageBusDelivery() {
        TransportOutcome accepted = TransportOutcome.accepted();
        TransportOutcome backpressured = TransportOutcome.backpressured("outbound capacity full");
        TransportOutcome unavailable = TransportOutcome.unavailable("peer unavailable");

        assertTrue(accepted.status().isAccepted());
        assertEquals(Optional.empty(), accepted.reason());
        assertFalse(backpressured.status().isAccepted());
        assertEquals(Optional.of("outbound capacity full"), backpressured.reason());
        assertFalse(unavailable.status().isAccepted());
        assertEquals(Optional.of("peer unavailable"), unavailable.reason());
        assertFalse(accepted.status().name().equals(DeliveryStatus.DELIVERED.name()),
                "transport acceptance must not masquerade as subscriber delivery");
    }

    @Test
    void requiresBoundedReasonsOnlyForNonAcceptance() {
        assertThrows(IllegalArgumentException.class, () -> new TransportOutcome(
                TransportStatus.ACCEPTED, Optional.of("not allowed")));
        assertThrows(IllegalArgumentException.class, () -> new TransportOutcome(
                TransportStatus.UNAVAILABLE, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new TransportOutcome(
                TransportStatus.BACKPRESSURED, Optional.of(" ")));
        assertThrows(IllegalArgumentException.class, () -> new TransportOutcome(
                TransportStatus.UNAVAILABLE,
                Optional.of("x".repeat(TransportOutcome.MAX_REASON_CHARACTERS + 1))));
        assertThrows(NullPointerException.class, () -> new TransportOutcome(
                null, Optional.empty()));
        assertThrows(NullPointerException.class, () -> new TransportOutcome(
                TransportStatus.UNAVAILABLE, null));
    }

    private static MessageEnvelope envelope() {
        return new MessageEnvelope(
                "00000000-0000-0000-0000-00000000000a",
                "correlation-1",
                Optional.empty(),
                "logical-run-1",
                "agent-loop",
                Instant.parse("2026-07-16T08:00:00Z"),
                new WorkPayload(TASK_REVISION, "a".repeat(64), Set.of("read-file")));
    }
}
