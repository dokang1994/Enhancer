package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.BackpressurePolicy;
import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.FileSpoolPublicationOutcome;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.TransportStatus;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ControlSpoolPublisherTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000821";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000822";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000823";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000000824";
    private static final String CONTROL_MESSAGE_ID =
            "00000000-0000-0000-0000-000000000825";
    private static final DeliveryDestination DESTINATION =
            DeliveryDestination.queue("runtime-controls");

    @TempDir
    Path temporaryRoot;

    @Test
    void derivesBindingFromActiveRuntimeAndPublishesCallerOwnedIntent()
            throws Exception {
        Path runtimeRoot = temporaryRoot.resolve("runtime");
        Path spoolRoot = temporaryRoot.resolve("spool");
        FileSystemAgentRuntimeStateStore store = activeRuntime(runtimeRoot);
        long revision = store.resolve(GOAL_ID).revision();
        ControlSpoolPublisher publisher = new ControlSpoolPublisher(
                store,
                new FileSpoolMessageTransport(
                        spoolRoot, BackpressurePolicy.of(2)),
                DESTINATION);

        FileSpoolPublicationOutcome outcome = publisher.publish(
                GOAL_ID,
                CONTROL_MESSAGE_ID,
                "untrusted-operator",
                Instant.parse("2026-07-29T03:00:00Z"),
                ControlSignal.PAUSE,
                "inspect before continuing");

        assertEquals(TransportStatus.ACCEPTED, outcome.outcome().status());
        TransportMessage spooled = FileSpoolMessageTransport.read(
                spoolRoot.resolve(outcome.messageFile().orElseThrow()));
        assertEquals(DESTINATION, spooled.destination());
        MessageEnvelope control = spooled.envelope();
        assertEquals(CONTROL_MESSAGE_ID, control.messageId());
        assertEquals(workMessage().correlationId(), control.correlationId());
        assertEquals(Optional.of(WORK_MESSAGE_ID), control.causationId());
        assertEquals(workMessage().logicalRunId(), control.logicalRunId());
        assertEquals("untrusted-operator", control.producer());
        assertEquals(Instant.parse("2026-07-29T03:00:00Z"), control.occurredAt());
        assertEquals(
                new ControlPayload(
                        ControlSignal.PAUSE, "inspect before continuing"),
                control.payload());
        assertEquals(revision, store.resolve(GOAL_ID).revision());
        assertTrue(store.resolve(GOAL_ID).controlRequests().isEmpty());
    }

    @Test
    void refusesRuntimeWithoutActiveNonTerminalAgentRunBeforeSpooling()
            throws Exception {
        Path runtimeRoot = temporaryRoot.resolve("inactive-runtime");
        Path spoolRoot = temporaryRoot.resolve("inactive-spool");
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(runtimeRoot);
        DurableAgentRuntime.create(
                GOAL_ID, workItem(), store, Clock.systemUTC());
        ControlSpoolPublisher publisher = new ControlSpoolPublisher(
                store,
                new FileSpoolMessageTransport(
                        spoolRoot, BackpressurePolicy.standard()),
                DESTINATION);

        assertThrows(IllegalStateException.class, () -> publisher.publish(
                GOAL_ID,
                CONTROL_MESSAGE_ID,
                "untrusted-operator",
                Instant.parse("2026-07-29T03:00:00Z"),
                ControlSignal.CANCEL,
                "not yet active"));

        assertFalse(Files.exists(spoolRoot));
        assertTrue(store.resolve(GOAL_ID).controlRequests().isEmpty());
    }

    @Test
    void preservesHopLevelBackpressureWithoutAnotherPointOrRuntimeMutation()
            throws Exception {
        Path runtimeRoot = temporaryRoot.resolve("bounded-runtime");
        Path spoolRoot = temporaryRoot.resolve("bounded-spool");
        FileSystemAgentRuntimeStateStore store = activeRuntime(runtimeRoot);
        ControlSpoolPublisher publisher = new ControlSpoolPublisher(
                store,
                new FileSpoolMessageTransport(
                        spoolRoot, BackpressurePolicy.of(1)),
                DESTINATION);
        publisher.publish(
                GOAL_ID,
                CONTROL_MESSAGE_ID,
                "untrusted-operator",
                Instant.parse("2026-07-29T03:00:00Z"),
                ControlSignal.PAUSE,
                "first");

        FileSpoolPublicationOutcome refused = publisher.publish(
                GOAL_ID,
                "00000000-0000-0000-0000-000000000826",
                "untrusted-operator",
                Instant.parse("2026-07-29T03:01:00Z"),
                ControlSignal.RESUME,
                "second");

        assertEquals(TransportStatus.BACKPRESSURED, refused.outcome().status());
        assertTrue(refused.messageFile().isEmpty());
        try (var points = Files.list(spoolRoot)) {
            assertEquals(1, points.filter(Files::isRegularFile).count());
        }
        assertTrue(store.resolve(GOAL_ID).controlRequests().isEmpty());
    }

    private FileSystemAgentRuntimeStateStore activeRuntime(Path runtimeRoot)
            throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(runtimeRoot);
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), store, Clock.systemUTC());
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        return store;
    }

    private static WorkItem workItem() {
        return new WorkItem(WORK_ITEM_ID, "runtime-worker", workMessage());
    }

    private static MessageEnvelope workMessage() {
        return new MessageEnvelope(
                WORK_MESSAGE_ID,
                "control-publisher-correlation",
                Optional.empty(),
                "control-publisher-logical-run",
                "work-producer",
                Instant.parse("2026-07-29T02:30:00Z"),
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "publish-untrusted-control-intent-from-existing-goal-state",
                                "CURRENT_TASK.md",
                                "a".repeat(64)),
                        "b".repeat(64),
                        Set.of("read-file")));
    }
}
