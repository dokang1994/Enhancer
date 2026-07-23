package com.enhancer.runtime;

import java.io.IOException;

/**
 * Indicates that the correlated Scheduler or external-effect ledger changed during
 * one read-only inspection.
 */
public final class ConcurrentSchedulerExternalEffectInspectionException
        extends IOException {
    private static final long serialVersionUID = 1L;

    public ConcurrentSchedulerExternalEffectInspectionException(
            String reason) {
        super("Scheduler external-effect recovery inspection changed: "
                + reason);
    }
}
