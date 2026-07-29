package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DurableControlMessageReceiverTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000801";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000802";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000803";
    private static final String WORK_MESSAGE_ID =
            "00000000-0000-0000-0000-000000000804";
    private static final String CONTROL_MESSAGE_ID =
            "00000000-0000-0000-0000-000000000805";
    private static final DeliveryDestination DESTINATION =
            DeliveryDestination.queue("runtime-controls");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-29T01:00:00Z"),
            ZoneOffset.UTC);

    @TempDir
    Path storageRoot;

    @Test
    void publishesControlThroughTheBusAndDistinguishesPersistenceFromExactReplay()
            throws Exception {
        FileSystemAgentRuntimeStateStore store = activeRuntime();
        DurableControlMessageReceiver receiver = new DurableControlMessageReceiver(
                DESTINATION, GOAL_ID, store, CLOCK);
        TransportMessage message = new TransportMessage(
                DESTINATION, controlMessage("record intent"));

        DurableControlMessageReceiveResult recorded = receiver.receive(message);
        DurableControlMessageReceiveResult replayed = receiver.receive(message);

        assertEquals(DurableControlMessageReceiveStatus.RECORDED, recorded.status());
        assertEquals(DurableControlMessageReceiveStatus.REPLAYED, replayed.status());
        assertEquals(GOAL_ID, recorded.goalId());
        assertEquals(CONTROL_MESSAGE_ID, recorded.messageId());
        assertEquals(ControlSignal.PAUSE, recorded.signal());
        assertEquals(3, recorded.runtimeRevision());
        assertEquals(3, replayed.runtimeRevision());
        DurableAgentRuntime recovered = DurableAgentRuntime.recover(
                GOAL_ID, store, CLOCK);
        assertEquals(List.of(message.envelope()), recovered.controlRequests());
        assertEquals(RuntimeGoalStatus.ACTIVE, recovered.goal().status());
        assertEquals(RuntimeAgentRunStatus.READY,
                recovered.agentRun().orElseThrow().status());
    }

    @Test
    void refusesForeignRouteAndPayloadBeforeRuntimeMutation() throws Exception {
        FileSystemAgentRuntimeStateStore store = activeRuntime();
        DurableControlMessageReceiver receiver = new DurableControlMessageReceiver(
                DESTINATION, GOAL_ID, store, CLOCK);
        long revision = store.resolve(GOAL_ID).revision();

        assertThrows(IllegalArgumentException.class, () -> receiver.receive(
                new TransportMessage(
                        DeliveryDestination.queue("foreign-controls"),
                        controlMessage("foreign route"))));
        assertThrows(IllegalArgumentException.class, () -> receiver.receive(
                new TransportMessage(DESTINATION, workMessage())));

        assertEquals(revision, store.resolve(GOAL_ID).revision());
        assertEquals(List.of(), store.resolve(GOAL_ID).controlRequests());
    }

    @Test
    void changedContentUnderTheSameMessageIdentityFailsClosed() throws Exception {
        FileSystemAgentRuntimeStateStore store = activeRuntime();
        DurableControlMessageReceiver receiver = new DurableControlMessageReceiver(
                DESTINATION, GOAL_ID, store, CLOCK);
        receiver.receive(new TransportMessage(
                DESTINATION, controlMessage("first intent")));

        assertThrows(IllegalStateException.class, () -> receiver.receive(
                new TransportMessage(
                        DESTINATION, controlMessage("changed intent"))));
        assertEquals(3, store.resolve(GOAL_ID).revision());
        assertEquals(
                List.of(controlMessage("first intent")),
                store.resolve(GOAL_ID).controlRequests());
    }

    private FileSystemAgentRuntimeStateStore activeRuntime() throws Exception {
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(storageRoot);
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID, workItem(), store, CLOCK);
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        return store;
    }

    private static WorkItem workItem() {
        return new WorkItem(
                WORK_ITEM_ID,
                "runtime-worker",
                workMessage());
    }

    private static MessageEnvelope workMessage() {
        return new MessageEnvelope(
                WORK_MESSAGE_ID,
                "control-receiver-correlation",
                Optional.empty(),
                "control-receiver-logical-run",
                "control-receiver-test",
                Instant.parse("2026-07-29T00:30:00Z"),
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "connect-control-spool-to-durable-runtime-request",
                                "CURRENT_TASK.md",
                                "a".repeat(64)),
                        "b".repeat(64),
                        Set.of("read-file")));
    }

    private static MessageEnvelope controlMessage(String reason) {
        return new MessageEnvelope(
                CONTROL_MESSAGE_ID,
                "control-receiver-correlation",
                Optional.of(WORK_MESSAGE_ID),
                "control-receiver-logical-run",
                "untrusted-control-producer",
                Instant.parse("2026-07-29T01:00:00Z"),
                new ControlPayload(ControlSignal.PAUSE, reason));
    }
}
