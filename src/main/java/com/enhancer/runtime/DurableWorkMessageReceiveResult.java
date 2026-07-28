package com.enhancer.runtime;

import java.util.Objects;

/** Bounded evidence that one Message Bus delivery reached durable Scheduler admission. */
public record DurableWorkMessageReceiveResult(
        DurableWorkMessageReceiveStatus status,
        String queueId,
        long queueRevision,
        String workItemId,
        SchedulerPriority priority) {

    public DurableWorkMessageReceiveResult {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(queueId, "queueId must not be null");
        if (queueRevision < 0) {
            throw new IllegalArgumentException("queueRevision must not be negative");
        }
        Objects.requireNonNull(workItemId, "workItemId must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
    }
}
