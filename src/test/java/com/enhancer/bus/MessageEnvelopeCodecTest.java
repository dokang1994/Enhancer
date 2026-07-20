package com.enhancer.bus;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.kernel.VerificationStatus;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Verifies the wire format without a filesystem. Publication concerns — atomic move, spool
 * capacity, unusable roots — belong to {@link FileSpoolMessageTransportTest}.
 */
class MessageEnvelopeCodecTest {
    private static final ApprovedTaskRevision TASK_REVISION = new ApprovedTaskRevision(
            "gate-8-file-spool-transport", "CURRENT_TASK.md", "a".repeat(64));
    private static final String SNAPSHOT_ID = "b".repeat(64);

    private final MessageEnvelopeCodec codec = new MessageEnvelopeCodec();

    @Test
    void roundTripsEveryPayloadKind() throws Exception {
        assertRoundTrip(DeliveryDestination.queue("worker-tasks"), Optional.of(uuid()),
                new WorkPayload(TASK_REVISION, SNAPSHOT_ID, Set.of("read-file", "verify"),
                        Optional.of(new WorkPayload.ExecutionInput(
                                "docs/target-🚀.md", "c".repeat(64)))));
        assertRoundTrip(DeliveryDestination.queue("worker-tasks"), Optional.empty(),
                new WorkPayload(TASK_REVISION, SNAPSHOT_ID, Set.of("read-file")));
        assertRoundTrip(DeliveryDestination.topic("results"), Optional.of(uuid()),
                new ResultPayload("gate-8", "run-record/" + uuid(), VerificationStatus.VERIFIED));
        assertRoundTrip(DeliveryDestination.queue("controls"), Optional.empty(),
                new ControlPayload(ControlSignal.PAUSE, "operator paused the run"));
        assertRoundTrip(DeliveryDestination.topic("handoffs"), Optional.of(uuid()),
                new HandoffPayload(TASK_REVISION, SNAPSHOT_ID, "run-record/" + uuid()));
    }

    @Test
    void preservesSubMillisecondOccurrenceTime() throws Exception {
        // Rounding an Instant to milliseconds would silently rewrite provenance the receiver is
        // meant to trust, so the frame carries epoch-second plus nanosecond.
        Instant withNanos = Instant.parse("2026-07-20T09:00:00.123456789Z");
        TransportMessage message = new TransportMessage(
                DeliveryDestination.queue("work"),
                new MessageEnvelope(uuid(), "correlation-1", Optional.empty(), "run-1",
                        "scheduler", withNanos,
                        new WorkPayload(TASK_REVISION, SNAPSHOT_ID, Set.of("read-file"))));

        TransportMessage decoded = codec.decode(codec.encode(message));

        assertEquals(withNanos, decoded.envelope().occurredAt());
        assertEquals(message, decoded);
    }

    @Test
    void encodesDeterministically() {
        TransportMessage message = message(new WorkPayload(
                TASK_REVISION, SNAPSHOT_ID, Set.of("read-file")));

        // No wall-clock or random state enters the frame, so a peer may deduplicate on content.
        assertArrayEquals(codec.encode(message), codec.encode(message));
    }

    @Test
    void failsClosedOnEveryFrameCorruption() {
        byte[] frame = codec.encode(message(
                new WorkPayload(TASK_REVISION, SNAPSHOT_ID, Set.of("read-file"))));

        byte[] badMagic = frame.clone();
        badMagic[0] ^= 0x7f;
        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(badMagic));

        byte[] tamperedBody = frame.clone();
        tamperedBody[tamperedBody.length - 1] ^= 0x7f;
        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(tamperedBody));

        byte[] truncated = java.util.Arrays.copyOf(frame, frame.length - 1);
        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(truncated));

        byte[] trailing = java.util.Arrays.copyOf(frame, frame.length + 1);
        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(trailing));

        assertThrows(CorruptedSpooledMessageException.class, () -> codec.decode(new byte[0]));
    }

    private void assertRoundTrip(
            DeliveryDestination destination,
            Optional<String> causationId,
            MessagePayload payload) throws Exception {
        TransportMessage message = new TransportMessage(destination, new MessageEnvelope(
                uuid(), "correlation-1", causationId, "run-1", "scheduler",
                Instant.parse("2026-07-20T09:00:00Z"), payload));

        assertEquals(message, codec.decode(codec.encode(message)));
    }

    private static TransportMessage message(MessagePayload payload) {
        return new TransportMessage(DeliveryDestination.queue("work"), new MessageEnvelope(
                "00000000-0000-0000-0000-0000000000a1", "correlation-1",
                Optional.of("00000000-0000-0000-0000-0000000000c1"), "run-1", "scheduler",
                Instant.parse("2026-07-20T09:00:00Z"), payload));
    }

    private static String uuid() {
        return java.util.UUID.randomUUID().toString();
    }
}
