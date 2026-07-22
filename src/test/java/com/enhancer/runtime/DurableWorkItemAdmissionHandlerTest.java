package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.DeliveryStatus;
import com.enhancer.bus.InProcessMessageBus;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.RetryPolicy;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DurableWorkItemAdmissionHandlerTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000701";
    private static final String MESSAGE_ID =
            "00000000-0000-0000-0000-000000000702";
    private static final DeliveryDestination DESTINATION =
            DeliveryDestination.queue("durable-work-admission");

    @Test
    void persistsStableDistinctWorkIdentityBeforeDelivery() throws Exception {
        RecordingStore store = new RecordingStore();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store);
        InProcessMessageBus bus = new InProcessMessageBus();
        bus.subscribe(
                DESTINATION,
                "durable-gate-8-admission",
                new DurableWorkItemAdmissionHandler("read-file-worker", queue));

        assertEquals(
                DeliveryStatus.DELIVERED,
                bus.publish(DESTINATION, workMessage()).get(0).status());
        assertEquals(1, store.updateAttempts);
        WorkItem admitted = queue.claimNext().orElseThrow();
        assertEquals(workMessage(), admitted.workMessage());
        assertNotEquals(MESSAGE_ID, admitted.workItemId());
        assertEquals(
                admitted.workItemId(),
                DurableWorkItemAdmissionHandler.workItemIdFor(MESSAGE_ID));
    }

    @Test
    void storageFailureUsesBusRetryAndDeadLetterWithoutExposure()
            throws Exception {
        RecordingStore store = new RecordingStore();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store);
        store.failUpdates = true;
        InProcessMessageBus bus = new InProcessMessageBus(RetryPolicy.of(2));
        bus.subscribe(
                DESTINATION,
                "durable-gate-8-admission",
                new DurableWorkItemAdmissionHandler("read-file-worker", queue));

        assertEquals(
                DeliveryStatus.FAILED,
                bus.publish(DESTINATION, workMessage()).get(0).status());
        assertEquals(2, store.updateAttempts);
        assertEquals(1, bus.deadLetters().size());
        assertEquals(0, queue.revision());
        assertEquals(0, queue.pendingCount());
        assertEquals(Optional.empty(), queue.logicalRunId());
    }

    @Test
    void directCheckedPersistenceFailureIsExposedAsUncheckedIo() throws Exception {
        RecordingStore store = new RecordingStore();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store);
        store.failUpdates = true;
        DurableWorkItemAdmissionHandler handler =
                new DurableWorkItemAdmissionHandler("read-file-worker", queue);

        assertThrows(UncheckedIOException.class, () -> handler.handle(workMessage()));
    }

    @Test
    void freshBusReplayFailsClosedWithoutASecondDurableAdmission()
            throws Exception {
        RecordingStore store = new RecordingStore();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store);
        InProcessMessageBus firstBus = new InProcessMessageBus();
        firstBus.subscribe(
                DESTINATION,
                "durable-gate-8-admission",
                new DurableWorkItemAdmissionHandler("read-file-worker", queue));
        assertEquals(
                DeliveryStatus.DELIVERED,
                firstBus.publish(DESTINATION, workMessage()).get(0).status());

        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, store);
        InProcessMessageBus restartedBus = new InProcessMessageBus();
        restartedBus.subscribe(
                DESTINATION,
                "durable-gate-8-admission",
                new DurableWorkItemAdmissionHandler("read-file-worker", recovered));

        assertEquals(
                DeliveryStatus.FAILED,
                restartedBus.publish(DESTINATION, workMessage()).get(0).status());
        assertEquals(1, restartedBus.deadLetters().size());
        assertEquals(1, recovered.revision());
        assertEquals(1, recovered.pendingCount());
    }

    private static MessageEnvelope workMessage() {
        return new MessageEnvelope(
                MESSAGE_ID,
                "correlation-durable-admission",
                Optional.empty(),
                "logical-run-durable-admission",
                "durable-admission-test",
                Instant.parse("2026-07-22T10:00:00Z"),
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "connect-work-admission-to-durable-queue",
                                "CURRENT_TASK.md",
                                "a".repeat(64)),
                        "b".repeat(64),
                        Set.of("read-file")));
    }

    private static final class RecordingStore implements SchedulerQueueStore {
        private SchedulerQueueState state;
        private int updateAttempts;
        private boolean failUpdates;

        @Override
        public void create(SchedulerQueueState initialState) {
            state = initialState;
        }

        @Override
        public void update(SchedulerQueueState nextState) throws IOException {
            updateAttempts++;
            if (failUpdates) {
                throw new IOException("simulated admission persistence failure");
            }
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
