package com.enhancer.runtime;

import java.io.IOException;

/**
 * Signals that a validated queue source changed before migration publication.
 */
public final class ConcurrentSchedulerQueueMigrationException
        extends IOException {
    private static final long serialVersionUID = 1L;

    public ConcurrentSchedulerQueueMigrationException(String queueId) {
        super("Scheduler queue state changed during migration: " + queueId);
    }
}
