package com.enhancer.tool;

import java.io.IOException;

public final class CorruptedEvidenceException extends IOException {
    public CorruptedEvidenceException(String message) {
        super(message);
    }

    public CorruptedEvidenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
