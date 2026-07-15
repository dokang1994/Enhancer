package com.enhancer.run;

import java.io.IOException;

public final class CorruptedRunRecordException extends IOException {
    private static final long serialVersionUID = 1L;

    public CorruptedRunRecordException(String message) {
        super(message);
    }

    public CorruptedRunRecordException(String message, Throwable cause) {
        super(message, cause);
    }
}
