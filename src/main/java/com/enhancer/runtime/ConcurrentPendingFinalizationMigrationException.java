package com.enhancer.runtime;

import java.io.IOException;

/**
 * Signals that the migration source changed after it was validated.
 */
public final class ConcurrentPendingFinalizationMigrationException
        extends IOException {
    private static final long serialVersionUID = 1L;

    public ConcurrentPendingFinalizationMigrationException() {
        super("Pending finalization state changed during migration");
    }
}
