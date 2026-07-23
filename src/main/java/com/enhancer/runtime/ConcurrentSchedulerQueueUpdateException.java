package com.enhancer.runtime;

import java.io.IOException;

/** A local queue writer is already performing its read-validate-publish transaction. */
public final class ConcurrentSchedulerQueueUpdateException extends IOException {
    private static final long serialVersionUID = 1L;

    public ConcurrentSchedulerQueueUpdateException(String queueId) {
        super("Scheduler queue update is already in progress: " + queueId);
    }

    public ConcurrentSchedulerQueueUpdateException(
            String queueId,
            Throwable cause) {
        super("Scheduler queue update is already in progress: " + queueId, cause);
    }
}
