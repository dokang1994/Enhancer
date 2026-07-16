package com.enhancer.runtime;

import java.io.IOException;

public final class CorruptedSchedulerQueueStateException extends IOException {
    private static final long serialVersionUID = 1L;

    public CorruptedSchedulerQueueStateException(String message) {
        super(message);
    }

    public CorruptedSchedulerQueueStateException(
            String message,
            Throwable cause) {
        super(message, cause);
    }
}
