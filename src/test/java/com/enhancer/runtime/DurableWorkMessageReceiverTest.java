package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.ControlPayload;
import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.time.Instant;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DurableWorkMessageReceiverTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000711";
    private static final String MESSAGE_ID =
            "00000000-0000-0000-0000-000000000712";
    private static final DeliveryDestination DESTINATION =
            DeliveryDestination.queue("scheduler-work");

    @Test
    void publishesThroughTheBusAndDistinguishesAdmissionFromExactReplay()
            throws Exception {
        RecordingStore store = new RecordingStore();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store);
        DurableWorkMessageReceiver receiver = new DurableWorkMessageReceiver(
                DESTINATION, "read-file-worker", SchedulerPriority.EXPEDITED, queue);

        DurableWorkMessageReceiveResult admitted =
                receiver.receive(new TransportMessage(DESTINATION, workMessage()));
        DurableWorkMessageReceiveResult replayed =
                receiver.receive(new TransportMessage(DESTINATION, workMessage()));

        assertEquals(DurableWorkMessageReceiveStatus.ADMITTED, admitted.status());
        assertEquals(DurableWorkMessageReceiveStatus.REPLAYED, replayed.status());
        assertEquals(1, queue.revision());
        assertEquals(1, queue.pendingCount());
        assertEquals(QUEUE_ID, admitted.queueId());
        assertEquals(
                DurableWorkItemAdmissionHandler.workItemIdFor(MESSAGE_ID),
                admitted.workItemId());
        assertEquals(SchedulerPriority.EXPEDITED, admitted.priority());
    }

    @Test
    void refusesForeignDestinationAndNonWorkBeforeQueueMutation() throws Exception {
        RecordingStore store = new RecordingStore();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store);
        DurableWorkMessageReceiver receiver = new DurableWorkMessageReceiver(
                DESTINATION, "read-file-worker", SchedulerPriority.NORMAL, queue);

        assertThrows(IllegalArgumentException.class, () -> receiver.receive(
                new TransportMessage(
                        DeliveryDestination.queue("foreign"), workMessage())));
        assertThrows(IllegalArgumentException.class, () -> receiver.receive(
                new TransportMessage(
                        DESTINATION,
                        new MessageEnvelope(
                                "00000000-0000-0000-0000-000000000713",
                                "receiver-control-correlation",
                                Optional.empty(),
                                "receiver-control-logical-run",
                                "receiver-test",
                                Instant.parse("2026-07-28T05:00:00Z"),
                                new ControlPayload(
                                        ControlSignal.CANCEL, "not work")))));
        assertEquals(0, queue.revision());
        assertEquals(0, queue.pendingCount());
    }

    @Test
    void changedContentUnderTheSameIdentityFailsClosed() throws Exception {
        RecordingStore store = new RecordingStore();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store);
        DurableWorkMessageReceiver receiver = new DurableWorkMessageReceiver(
                DESTINATION, "read-file-worker", SchedulerPriority.NORMAL, queue);
        receiver.receive(new TransportMessage(DESTINATION, workMessage()));

        assertThrows(IllegalStateException.class, () -> receiver.receive(
                new TransportMessage(DESTINATION, workMessage("changed-producer"))));
        assertEquals(1, queue.revision());
        assertEquals(1, queue.pendingCount());
    }

    @Test
    void refusesExternalTypedModelWorkBeforeQueueMutation() throws Exception {
        RecordingStore store = new RecordingStore();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store);
        DurableWorkMessageReceiver receiver = new DurableWorkMessageReceiver(
                DESTINATION, "model-worker", SchedulerPriority.NORMAL, queue);

        assertThrows(IllegalArgumentException.class, () -> receiver.receive(
                new TransportMessage(DESTINATION, ModelWorkFixtures.envelope())));

        assertEquals(0, queue.revision());
        assertEquals(0, queue.pendingCount());
    }

    private static MessageEnvelope workMessage() {
        return workMessage("receiver-test");
    }

    private static MessageEnvelope workMessage(String producer) {
        return new MessageEnvelope(
                MESSAGE_ID,
                "receiver-correlation",
                Optional.empty(),
                "receiver-logical-run",
                producer,
                Instant.parse("2026-07-28T05:00:00Z"),
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "connect-durable-work-spool-to-scheduler-worker",
                                "CURRENT_TASK.md",
                                "a".repeat(64)),
                        "b".repeat(64),
                        Set.of("read-file")));
    }

    private static final class RecordingStore implements SchedulerQueueStore {
        private SchedulerQueueState state;

        @Override
        public void create(SchedulerQueueState initialState) {
            state = initialState;
        }

        @Override
        public void update(SchedulerQueueState nextState) {
            state = nextState;
        }

        @Override
        public SchedulerQueueState resolve(String queueId) throws IOException {
            if (state == null || !state.queueId().equals(queueId)) {
                throw new MissingSchedulerQueueStateException(queueId);
            }
            return state;
        }
    }
}
