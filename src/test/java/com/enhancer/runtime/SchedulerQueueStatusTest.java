package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SchedulerQueueStatusTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000901";
    private static final String VERIFIED_ID =
            "00000000-0000-0000-0000-000000000911";
    private static final String FAILED_ID =
            "00000000-0000-0000-0000-000000000912";
    private static final String ACTIVE_ID =
            "00000000-0000-0000-0000-000000000913";
    private static final String READY_ID =
            "00000000-0000-0000-0000-000000000914";
    private static final String BLOCKED_ID =
            "00000000-0000-0000-0000-000000000915";

    @Test
    void projectsEveryPersistedQueueStateInAdmissionOrder() {
        SingleWorkerSchedulerQueue queue = queueWithEveryState();

        SchedulerQueueStatus status =
                SchedulerQueueStatus.project(queue.snapshot(QUEUE_ID, 9));

        assertEquals(QUEUE_ID, status.queueId());
        assertEquals(9, status.revision());
        assertEquals(8, status.maxWorkItems());
        assertEquals(List.of(
                new SchedulerQueueStatus.WorkStatus(
                        VERIFIED_ID, SchedulerQueueStatus.WorkState.VERIFIED),
                new SchedulerQueueStatus.WorkStatus(
                        FAILED_ID, SchedulerQueueStatus.WorkState.FAILED),
                new SchedulerQueueStatus.WorkStatus(
                        ACTIVE_ID, SchedulerQueueStatus.WorkState.ACTIVE),
                new SchedulerQueueStatus.WorkStatus(
                        READY_ID, SchedulerQueueStatus.WorkState.READY),
                new SchedulerQueueStatus.WorkStatus(
                        BLOCKED_ID, SchedulerQueueStatus.WorkState.BLOCKED)),
                status.workItems());
        for (SchedulerQueueStatus.WorkState state
                : SchedulerQueueStatus.WorkState.values()) {
            assertEquals(1, status.count(state));
        }
    }

    @Test
    void projectsAnEmptyQueueWithoutInventingWork() {
        SchedulerQueueStatus status = SchedulerQueueStatus.project(
                SchedulerQueueState.initial(QUEUE_ID, 4));

        assertEquals(0, status.revision());
        assertEquals(4, status.maxWorkItems());
        assertEquals(List.of(), status.workItems());
        for (SchedulerQueueStatus.WorkState state
                : SchedulerQueueStatus.WorkState.values()) {
            assertEquals(0, status.count(state));
        }
    }

    private SingleWorkerSchedulerQueue queueWithEveryState() {
        SingleWorkerSchedulerQueue queue = new SingleWorkerSchedulerQueue(8);
        queue.enqueue(queued(VERIFIED_ID, List.of()));
        queue.claimNext();
        queue.completeActiveVerified(VERIFIED_ID);
        queue.enqueue(queued(FAILED_ID, List.of()));
        queue.claimNext();
        queue.failActive(FAILED_ID);
        queue.enqueue(queued(ACTIVE_ID, List.of()));
        queue.claimNext();
        queue.enqueue(queued(READY_ID, List.of(VERIFIED_ID)));
        queue.enqueue(queued(BLOCKED_ID, List.of(FAILED_ID)));
        return queue;
    }

    private QueuedWork queued(String workItemId, List<String> dependencies) {
        ApprovedTaskRevision taskRevision = new ApprovedTaskRevision(
                "scheduler-status-test",
                "CURRENT_TASK.md",
                "a".repeat(64));
        long suffix = Long.parseLong(
                workItemId.substring(workItemId.length() - 12));
        MessageEnvelope envelope = new MessageEnvelope(
                String.format("00000000-0000-0000-0001-%012d", suffix),
                "scheduler-status-correlation",
                Optional.empty(),
                "scheduler-status-logical-run",
                "scheduler-status-test",
                Instant.parse("2026-07-23T02:00:00Z"),
                new WorkPayload(
                        taskRevision,
                        "b".repeat(64),
                        Set.of("read-file")));
        return new QueuedWork(
                new WorkItem(workItemId, "read-file-worker", envelope),
                dependencies);
    }
}
