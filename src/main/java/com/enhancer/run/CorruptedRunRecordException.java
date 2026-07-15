package com.enhancer.run;

import java.io.IOException;

public final class CorruptedRunRecordException extends IOException {
    public CorruptedRunRecordException(String message) {
        super(message);
    }

    public CorruptedRunRecordException(String message, Throwable cause) {
        super(message, cause);
    }
}
