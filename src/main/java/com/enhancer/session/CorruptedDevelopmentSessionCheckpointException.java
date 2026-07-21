package com.enhancer.session;

import java.io.IOException;

/** The checkpoint artifact exists but cannot be trusted. */
public final class CorruptedDevelopmentSessionCheckpointException extends IOException {
    private static final long serialVersionUID = 1L;

    public CorruptedDevelopmentSessionCheckpointException(String message) {
        super(message);
    }

    public CorruptedDevelopmentSessionCheckpointException(
            String message,
            Throwable cause) {
        super(message, cause);
    }
}
