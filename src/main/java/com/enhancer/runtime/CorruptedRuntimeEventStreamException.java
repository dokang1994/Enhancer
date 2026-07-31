package com.enhancer.runtime;

import java.io.IOException;

/** A runtime-event artifact failed its bounded integrity or schema contract. */
public final class CorruptedRuntimeEventStreamException extends IOException {
    private static final long serialVersionUID = 1L;

    public CorruptedRuntimeEventStreamException(String message) {
        super(message);
    }

    public CorruptedRuntimeEventStreamException(
            String message,
            Throwable cause) {
        super(message, cause);
    }
}
