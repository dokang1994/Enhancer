package com.enhancer.runtime;

import java.io.IOException;

/**
 * Indicates that independent Scheduler recovery stores changed during one read-only
 * inspection, so no mixed projection was returned.
 */
public final class ConcurrentSchedulerRecoveryInspectionException
        extends IOException {
    private static final long serialVersionUID = 1L;

    public ConcurrentSchedulerRecoveryInspectionException(String reason) {
        super("Scheduler recovery state changed during inspection: " + reason);
    }
}
