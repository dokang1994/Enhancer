package com.enhancer.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.kernel.VerificationStatus;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSpoolMessageTransportTest {
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-20T09:00:00Z");
    private static final String SNAPSHOT_ID = "a".repeat(64);
    private static final String SOURCE_SHA = "b".repeat(64);

    @TempDir
    Path temporaryRoot;

    @Test
    void roundTripsEveryPayloadKindWithoutChangingRouteOrEnvelope() throws IOException {
        Path spool = temporaryRoot.resolve("spool");
        FileSpoolMessageTransport transport =
                new FileSpoolMessageTransport(spool, BackpressurePolicy.standard());

        List<TransportMessage> messages = List.of(
                new TransportMessage(DeliveryDestination.queue("work"), envelope(workPayload())),
                new TransportMessage(DeliveryDestination.topic("results"), envelope(
                        new ResultPayload("gate-8", "run-record/" + UUID.randomUUID(),
                                VerificationStatus.VERIFIED))),
                new TransportMessage(DeliveryDestination.queue("controls"), envelope(
                        new ControlPayload(ControlSignal.CANCEL, "user requested"))),
                new TransportMessage(DeliveryDestination.topic("handoffs"), envelope(
                        new HandoffPayload(revision(), SNAPSHOT_ID,
                                "run-record/" + UUID.randomUUID()))));

        for (TransportMessage message : messages) {
            assertEquals(
                    new TransportOutcome(TransportStatus.ACCEPTED, Optional.empty()),
                    transport.send(message),
                    "acceptance carries no reason");
        }

        List<Path> spooled = spooledFiles(spool);
        assertEquals(messages.size(), spooled.size(), "each send spools exactly one message");

        // A peer decodes the same routes and envelopes it was handed, including the optional
        // causation identity and the nested execution input, or the hop carried nothing useful.
        // Compared as a set: the transport contract is per-hop and promises no ordering across
        // separately spooled messages, so pairing them by directory order would assert a
        // guarantee the adapter does not make.
        Set<TransportMessage> decoded = new java.util.LinkedHashSet<>();
        for (Path file : spooled) {
            decoded.add(FileSpoolMessageTransport.read(file));
        }
        assertEquals(Set.copyOf(messages), decoded);
    }

    @Test
    void refusesWithBackpressureAtCapacityAndSpoolsNothingFurther() throws IOException {
        Path spool = temporaryRoot.resolve("bounded-spool");
        FileSpoolMessageTransport transport =
                new FileSpoolMessageTransport(spool, BackpressurePolicy.of(2));
        TransportMessage message =
                new TransportMessage(DeliveryDestination.queue("work"), envelope(workPayload()));

        assertTrue(transport.send(message).status().isAccepted());
        assertTrue(transport.send(message).status().isAccepted());

        TransportOutcome refused = transport.send(message);

        assertEquals(TransportStatus.BACKPRESSURED, refused.status());
        assertTrue(refused.reason().isPresent(), "a non-accepted outcome must carry a reason");
        assertEquals(2, spooledFiles(spool).size(), "refused work must not be spooled");
    }

    @Test
    void refusesWithUnavailableWhenTheSpoolRootCannotHoldMessages() throws IOException {
        Path notADirectory = Files.writeString(temporaryRoot.resolve("occupied"), "x");
        FileSpoolMessageTransport transport =
                new FileSpoolMessageTransport(notADirectory, BackpressurePolicy.standard());

        TransportOutcome refused = transport.send(
                new TransportMessage(DeliveryDestination.queue("work"), envelope(workPayload())));

        assertEquals(TransportStatus.UNAVAILABLE, refused.status());
        assertTrue(refused.reason().isPresent());
    }

    @Test
    void failsClosedOnTruncatedOrTamperedSpooledMessages() throws IOException {
        Path spool = temporaryRoot.resolve("corrupt-spool");
        FileSpoolMessageTransport transport =
                new FileSpoolMessageTransport(spool, BackpressurePolicy.standard());
        transport.send(
                new TransportMessage(DeliveryDestination.queue("work"), envelope(workPayload())));
        Path spooled = spooledFiles(spool).get(0);
        byte[] original = Files.readAllBytes(spooled);

        // The frame's corruption modes are covered without a filesystem in
        // MessageEnvelopeCodecTest; what matters here is that read surfaces the typed exception
        // from a real file rather than swallowing it into a generic IOException, so a caller can
        // dead-letter a corrupt message instead of retrying it forever.
        Files.write(spooled, java.util.Arrays.copyOf(original, original.length - 1));
        assertThrows(CorruptedSpooledMessageException.class,
                () -> FileSpoolMessageTransport.read(spooled));
    }

    @Test
    void spoolsIdenticalBytesForTheSameMessage() throws IOException {
        Path spool = temporaryRoot.resolve("deterministic-spool");
        FileSpoolMessageTransport transport =
                new FileSpoolMessageTransport(spool, BackpressurePolicy.standard());
        TransportMessage message =
                new TransportMessage(DeliveryDestination.queue("work"), envelope(workPayload()));

        transport.send(message);
        transport.send(message);

        // The adapter adds no wall-clock header of its own, so two hops of one message are
        // byte-identical and a peer can deduplicate on content.
        List<Path> spooled = spooledFiles(spool);
        assertEquals(2, spooled.size());
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                Files.readAllBytes(spooled.get(0)), Files.readAllBytes(spooled.get(1)));
    }

    @Test
    void rejectsNullInputsAndGivesEachMessageItsOwnFile() throws IOException {
        Path spool = temporaryRoot.resolve("distinct-spool");
        FileSpoolMessageTransport transport =
                new FileSpoolMessageTransport(spool, BackpressurePolicy.standard());
        TransportMessage message =
                new TransportMessage(DeliveryDestination.queue("work"), envelope(workPayload()));

        assertThrows(NullPointerException.class, () -> transport.send(null));
        assertThrows(NullPointerException.class,
                () -> new FileSpoolMessageTransport(null, BackpressurePolicy.standard()));
        assertThrows(NullPointerException.class,
                () -> new FileSpoolMessageTransport(spool, null));

        transport.send(message);
        transport.send(message);

        List<Path> spooled = spooledFiles(spool);
        assertEquals(2, spooled.size());
        assertNotEquals(
                spooled.get(0).getFileName(),
                spooled.get(1).getFileName(),
                "resending the same envelope must never overwrite an earlier hop");
    }

    private static MessageEnvelope envelope(MessagePayload payload) {
        return new MessageEnvelope(
                UUID.randomUUID().toString(),
                "correlation-1",
                Optional.of(UUID.randomUUID().toString()),
                "run-1",
                "scheduler",
                OCCURRED_AT,
                payload);
    }

    private static WorkPayload workPayload() {
        return new WorkPayload(
                revision(),
                SNAPSHOT_ID,
                Set.of("read-file"),
                Optional.of(new WorkPayload.ExecutionInput("README.md", SOURCE_SHA)));
    }

    private static ApprovedTaskRevision revision() {
        return new ApprovedTaskRevision("gate-8-task", "CURRENT_TASK.md", SOURCE_SHA);
    }

    private static List<Path> spooledFiles(Path spool) throws IOException {
        if (!Files.isDirectory(spool)) {
            return List.of();
        }
        try (var paths = Files.list(spool)) {
            return paths
                    .filter(path -> path.toString()
                            .endsWith(FileSpoolMessageTransport.FILE_SUFFIX))
                    .sorted()
                    .toList();
        }
    }
}
