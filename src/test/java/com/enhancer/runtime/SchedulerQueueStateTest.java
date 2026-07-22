package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SchedulerQueueStateTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000301";
    private static final String FIRST_ID =
            "00000000-0000-0000-0000-000000000311";
    private static final String SECOND_ID =
            "00000000-0000-0000-0000-000000000312";

    @Test
    void partitionsAdmittedWorkAcrossVerifiedAndFailedDispositions() {
        QueuedWork first = new QueuedWork(workItem(FIRST_ID), List.of());
        QueuedWork second = new QueuedWork(workItem(SECOND_ID), List.of());
        SchedulerQueueState state = new SchedulerQueueState(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                QUEUE_ID, 5, 8, Optional.of("logical-run-state-1"),
                List.of(FIRST_ID, SECOND_ID),
                List.of(first, second),
                List.of(), Optional.empty(),
                Set.of(FIRST_ID), Set.of(SECOND_ID));

        assertEquals(Set.of(FIRST_ID), state.completedWorkItemIds());
        assertEquals(Set.of(SECOND_ID), state.failedWorkItemIds());
    }

    @Test
    void rejectsWorkItemThatIsBothCompletedAndFailed() {
        QueuedWork first = new QueuedWork(workItem(FIRST_ID), List.of());
        assertThrows(IllegalArgumentException.class, () -> new SchedulerQueueState(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                QUEUE_ID, 5, 8, Optional.of("logical-run-state-1"),
                List.of(FIRST_ID),
                List.of(first),
                List.of(), Optional.empty(),
                Set.of(FIRST_ID), Set.of(FIRST_ID)));
    }

    @Test
    void rejectsFailedWorkThatWasNeverAdmitted() {
        QueuedWork first = new QueuedWork(workItem(FIRST_ID), List.of());
        assertThrows(IllegalArgumentException.class, () -> new SchedulerQueueState(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                QUEUE_ID, 5, 8, Optional.of("logical-run-state-1"),
                List.of(FIRST_ID),
                List.of(first),
                List.of(), Optional.empty(),
                Set.of(FIRST_ID), Set.of(SECOND_ID)));
    }

    @Test
    void rejectsAdmissionHistoryThatDoesNotMatchOrderOrStatusContent() {
        QueuedWork first = new QueuedWork(workItem(FIRST_ID), List.of());
        QueuedWork second = new QueuedWork(workItem(SECOND_ID), List.of());

        assertThrows(IllegalArgumentException.class, () -> new SchedulerQueueState(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                QUEUE_ID, 5, 8, Optional.of("logical-run-state-1"),
                List.of(FIRST_ID, SECOND_ID),
                List.of(second, first),
                List.of(), Optional.empty(),
                Set.of(FIRST_ID), Set.of(SECOND_ID)));

        QueuedWork changed = new QueuedWork(
                new WorkItem(
                        FIRST_ID,
                        "changed-worker",
                        first.workItem().workMessage()),
                List.of());
        assertThrows(IllegalArgumentException.class, () -> new SchedulerQueueState(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                QUEUE_ID, 5, 8, Optional.of("logical-run-state-1"),
                List.of(FIRST_ID),
                List.of(first),
                List.of(changed), Optional.empty(),
                Set.of(), Set.of()));
    }

    @Test
    void retainsExactAdmissionHistoryAfterTerminalDisposition() {
        QueuedWork admitted = new QueuedWork(workItem(FIRST_ID), List.of());
        SingleWorkerSchedulerQueue queue = new SingleWorkerSchedulerQueue(8);
        queue.enqueue(admitted);
        queue.claimNext().orElseThrow();
        queue.failActive(FIRST_ID);

        SchedulerQueueState state = queue.snapshot(QUEUE_ID, 3);

        assertEquals(List.of(admitted), state.admittedWork());
        assertEquals(List.of(FIRST_ID), state.admissionOrder());
        assertEquals(Set.of(FIRST_ID), state.failedWorkItemIds());
    }

    private static WorkItem workItem(String workItemId) {
        return new WorkItem(
                workItemId,
                "read-file-worker",
                new MessageEnvelope(
                        "00000000-0000-0000-0001-000000000311",
                        "correlation-state-history",
                        Optional.empty(),
                        "logical-run-state-1",
                        "state-history-test",
                        Instant.parse("2026-07-22T13:00:00Z"),
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "persist-exact-durable-work-admission-history",
                                        "CURRENT_TASK.md",
                                        "a".repeat(64)),
                                "b".repeat(64),
                                Set.of("read-file"))));
    }
}
