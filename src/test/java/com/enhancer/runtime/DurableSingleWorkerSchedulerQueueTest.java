package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DurableSingleWorkerSchedulerQueueTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000201";
    private static final String FIRST_ID =
            "00000000-0000-0000-0000-000000000211";
    private static final String SECOND_ID =
            "00000000-0000-0000-0000-000000000212";

    @Test
    void persistsEveryVisibleTransitionAndRequeuesInterruptedActiveWork()
            throws Exception {
        MemoryQueueStore store = new MemoryQueueStore();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store);
        WorkItem first = workItem(FIRST_ID, "read-file-worker");
        WorkItem second = workItem(SECOND_ID, "review-worker");

        assertEquals(0, queue.revision());
        queue.enqueue(new QueuedWork(first, List.of()));
        queue.enqueue(new QueuedWork(second, List.of(FIRST_ID)));
        assertSame(first, queue.claimNext().orElseThrow());
        assertEquals(3, queue.revision());

        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, store);

        assertEquals(4, recovered.revision());
        assertTrue(recovered.activeWork().isEmpty());
        assertEquals(2, recovered.pendingCount());
        WorkItem retried = recovered.claimNext().orElseThrow();
        assertEquals(first, retried);
        assertEquals(first.workMessage(), retried.workMessage());
        recovered.completeActive(FIRST_ID);

        DurableSingleWorkerSchedulerQueue afterCompletion =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, store);
        assertEquals(Set.of(FIRST_ID), afterCompletion.completedWorkItemIds());
        assertSame(second, afterCompletion.claimNext().orElseThrow());
        assertEquals(Optional.of("logical-run-durable-1"),
                afterCompletion.logicalRunId());
    }

    @Test
    void persistenceFailureLeavesThePreviousQueueRevisionVisible()
            throws Exception {
        MemoryQueueStore store = new MemoryQueueStore();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store);
        WorkItem first = workItem(FIRST_ID, "read-file-worker");

        store.failNextUpdate();
        assertThrows(IOException.class, () ->
                queue.enqueue(new QueuedWork(first, List.of())));
        assertEquals(0, queue.revision());
        assertEquals(0, queue.pendingCount());
        assertTrue(queue.logicalRunId().isEmpty());

        queue.enqueue(new QueuedWork(first, List.of()));
        store.failNextUpdate();
        assertThrows(IOException.class, queue::claimNext);
        assertEquals(1, queue.revision());
        assertEquals(1, queue.pendingCount());
        assertTrue(queue.activeWork().isEmpty());

        assertSame(first, queue.claimNext().orElseThrow());
        store.failNextUpdate();
        assertThrows(IOException.class, () -> queue.completeActive(FIRST_ID));
        assertEquals(2, queue.revision());
        assertEquals(Optional.of(first), queue.activeWork());
        assertTrue(queue.completedWorkItemIds().isEmpty());
    }

    @Test
    void rejectsInvalidIdentityAndExistingCreation() throws Exception {
        MemoryQueueStore store = new MemoryQueueStore();

        assertThrows(IllegalArgumentException.class, () ->
                DurableSingleWorkerSchedulerQueue.create("not-a-uuid", 8, store));
        DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store);
        assertThrows(IOException.class, () ->
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, store));
        assertThrows(MissingSchedulerQueueStateException.class, () ->
                DurableSingleWorkerSchedulerQueue.recover(
                        "00000000-0000-0000-0000-000000000299",
                        store));
    }

    private static WorkItem workItem(String workItemId, String capability) {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                "gate-8-durable-scheduler-queue-state",
                "CURRENT_TASK.md",
                "a".repeat(64));
        MessageEnvelope envelope = new MessageEnvelope(
                incrementUuid(workItemId),
                "correlation-durable-1",
                Optional.empty(),
                "logical-run-durable-1",
                "gate-8-durable-queue-test",
                Instant.parse("2026-07-16T16:00:00.123456789Z"),
                new WorkPayload(
                        revision,
                        "b".repeat(64),
                        Set.of("read-file", "verify")));
        return new WorkItem(workItemId, capability, envelope);
    }

    private static String incrementUuid(String workItemId) {
        long suffix = Long.parseLong(
                workItemId.substring(workItemId.length() - 12));
        return String.format(
                "00000000-0000-0000-0002-%012d",
                suffix);
    }

    private static final class MemoryQueueStore implements SchedulerQueueStore {
        private SchedulerQueueState state;
        private boolean failNextUpdate;

        @Override
        public void create(SchedulerQueueState initialState) throws IOException {
            if (state != null) {
                throw new IOException("queue already exists");
            }
            state = initialState;
        }

        @Override
        public void update(SchedulerQueueState nextState) throws IOException {
            if (failNextUpdate) {
                failNextUpdate = false;
                throw new IOException("simulated persistence failure");
            }
            if (state == null
                    || nextState.revision() != state.revision() + 1) {
                throw new IOException("revision does not advance by one");
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

        void failNextUpdate() {
            failNextUpdate = true;
        }
    }
}
