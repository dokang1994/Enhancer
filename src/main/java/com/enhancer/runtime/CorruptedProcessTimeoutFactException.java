package com.enhancer.runtime;

import java.io.IOException;

/** Raised when a process-timeout point cannot be trusted or decoded exactly. */
public final class CorruptedProcessTimeoutFactException extends IOException {
    private static final long serialVersionUID = 1L;

    public CorruptedProcessTimeoutFactException(String reason) {
        super("process timeout fact is corrupt: " + reason);
    }

    public CorruptedProcessTimeoutFactException(String reason, Throwable cause) {
        super("process timeout fact is corrupt: " + reason, cause);
    }
}
