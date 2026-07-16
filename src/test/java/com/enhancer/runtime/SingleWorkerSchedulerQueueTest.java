package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SingleWorkerSchedulerQueueTest {
    private static final String FIRST_ID =
            "00000000-0000-0000-0000-000000000101";
    private static final String SECOND_ID =
            "00000000-0000-0000-0000-000000000102";
    private static final String THIRD_ID =
            "00000000-0000-0000-0000-000000000103";

    @Test
    void claimsOneReadyItemAtATimeAndReleasesDependenciesAfterCompletion() {
        WorkItem first = workItem(FIRST_ID, "read-file-worker");
        WorkItem second = workItem(SECOND_ID, "review-worker");
        WorkItem third = workItem(THIRD_ID, "test-worker");
        SingleWorkerSchedulerQueue queue = new SingleWorkerSchedulerQueue();

        queue.enqueue(new QueuedWork(first, List.of()));
        queue.enqueue(new QueuedWork(second, List.of(FIRST_ID)));
        queue.enqueue(new QueuedWork(third, List.of(SECOND_ID)));

        WorkItem claimedFirst = queue.claimNext().orElseThrow();
        assertSame(first, claimedFirst);
        assertEquals(Optional.of(first), queue.activeWork());
        assertTrue(queue.claimNext().isEmpty());
        assertThrows(IllegalStateException.class, () -> queue.completeActive(SECOND_ID));

        queue.completeActive(FIRST_ID);
        assertSame(second, queue.claimNext().orElseThrow());
        queue.completeActive(SECOND_ID);
        assertSame(third, queue.claimNext().orElseThrow());
        queue.completeActive(THIRD_ID);

        assertTrue(queue.claimNext().isEmpty());
        assertTrue(queue.activeWork().isEmpty());
        assertEquals(0, queue.pendingCount());
        assertEquals(Set.of(FIRST_ID, SECOND_ID, THIRD_ID),
                queue.completedWorkItemIds());
    }

    @Test
    void preservesFifoForIndependentReadyWorkAndRetainsTheExactWorkItem() {
        WorkItem first = workItem(FIRST_ID, "read-file-worker");
        WorkItem second = workItem(SECOND_ID, "review-worker");
        SingleWorkerSchedulerQueue queue = new SingleWorkerSchedulerQueue();

        QueuedWork queuedFirst = new QueuedWork(first, List.of());
        queue.enqueue(queuedFirst);
        queue.enqueue(new QueuedWork(second, List.of()));

        assertSame(first, queuedFirst.workItem());
        assertEquals(Set.of(), queuedFirst.dependencyWorkItemIds());
        assertThrows(UnsupportedOperationException.class, () ->
                queuedFirst.dependencyWorkItemIds().add(SECOND_ID));
        assertSame(first.workMessage(), queuedFirst.workItem().workMessage());
        assertSame(first, queue.claimNext().orElseThrow());
        queue.completeActive(FIRST_ID);
        assertSame(second, queue.claimNext().orElseThrow());
    }

    @Test
    void rejectsInvalidDependenciesDuplicatesCapacityAndInvalidCompletion() {
        WorkItem first = workItem(FIRST_ID, "read-file-worker");
        WorkItem second = workItem(SECOND_ID, "review-worker");
        SingleWorkerSchedulerQueue queue = new SingleWorkerSchedulerQueue(2);

        assertThrows(IllegalArgumentException.class, () ->
                new QueuedWork(first, List.of(FIRST_ID)));
        assertThrows(IllegalArgumentException.class, () ->
                new QueuedWork(first, List.of(SECOND_ID, SECOND_ID)));
        assertThrows(IllegalArgumentException.class, () ->
                new QueuedWork(first, List.of("not-a-uuid")));
        assertThrows(NullPointerException.class, () ->
                new QueuedWork(first, Arrays.asList((String) null)));
        assertThrows(IllegalArgumentException.class, () ->
                new QueuedWork(first, oversizedDependencies()));
        assertThrows(NullPointerException.class, () ->
                new QueuedWork(null, List.of()));
        assertThrows(NullPointerException.class, () ->
                new QueuedWork(first, null));

        assertThrows(IllegalArgumentException.class, () ->
                queue.enqueue(new QueuedWork(second, List.of(FIRST_ID))));
        queue.enqueue(new QueuedWork(first, List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                queue.enqueue(new QueuedWork(first, List.of())));
        queue.enqueue(new QueuedWork(second, List.of(FIRST_ID)));
        assertThrows(IllegalStateException.class, () ->
                queue.enqueue(new QueuedWork(
                        workItem(THIRD_ID, "test-worker"),
                        List.of(SECOND_ID))));

        assertThrows(IllegalStateException.class, () -> queue.completeActive(FIRST_ID));
        assertThrows(NullPointerException.class, () -> queue.completeActive(null));
        assertThrows(NullPointerException.class, () -> queue.enqueue(null));
        assertThrows(IllegalArgumentException.class, () ->
                new SingleWorkerSchedulerQueue(0));
        assertThrows(IllegalArgumentException.class, () ->
                new SingleWorkerSchedulerQueue(
                        SingleWorkerSchedulerQueue.MAX_WORK_ITEMS + 1));
    }

    @Test
    void bindsOneQueueToOneLogicalRun() {
        SingleWorkerSchedulerQueue queue = new SingleWorkerSchedulerQueue();
        queue.enqueue(new QueuedWork(
                workItem(FIRST_ID, "read-file-worker"),
                List.of()));

        assertEquals(Optional.of("logical-run-queue-1"), queue.logicalRunId());
        assertThrows(IllegalArgumentException.class, () ->
                queue.enqueue(new QueuedWork(
                        workItem(
                                SECOND_ID,
                                "review-worker",
                                "logical-run-queue-2"),
                        List.of(FIRST_ID))));
    }

    private static List<String> oversizedDependencies() {
        List<String> dependencies = new ArrayList<>();
        for (int index = 0; index <= QueuedWork.MAX_DEPENDENCIES; index++) {
            dependencies.add(String.format(
                    "00000000-0000-0000-0001-%012d", index));
        }
        return dependencies;
    }

    private static WorkItem workItem(String workItemId, String capability) {
        return workItem(workItemId, capability, "logical-run-queue-1");
    }

    private static WorkItem workItem(
            String workItemId,
            String capability,
            String logicalRunId) {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                "gate-8-single-worker-scheduler-queue",
                "CURRENT_TASK.md",
                "a".repeat(64));
        MessageEnvelope envelope = new MessageEnvelope(
                incrementUuid(workItemId),
                "correlation-queue-1",
                Optional.empty(),
                logicalRunId,
                "gate-8-queue-test",
                Instant.parse("2026-07-16T14:00:00Z"),
                new WorkPayload(revision, "b".repeat(64), Set.of("read-file")));
        return new WorkItem(workItemId, capability, envelope);
    }

    private static String incrementUuid(String workItemId) {
        long suffix = Long.parseLong(workItemId.substring(workItemId.length() - 12));
        return String.format(
                "00000000-0000-0000-0002-%012d",
                suffix);
    }
}
