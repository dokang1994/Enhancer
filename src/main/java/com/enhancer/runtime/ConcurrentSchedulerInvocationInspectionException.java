package com.enhancer.runtime;

import java.io.IOException;

/** Raised when independent Scheduler and invocation-spool samples do not remain stable. */
public final class ConcurrentSchedulerInvocationInspectionException extends IOException {
    private static final long serialVersionUID = 1L;

    public ConcurrentSchedulerInvocationInspectionException(String message) {
        super(message);
    }
}
