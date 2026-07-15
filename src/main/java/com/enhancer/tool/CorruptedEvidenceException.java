package com.enhancer.tool;

import java.io.IOException;

public final class CorruptedEvidenceException extends IOException {
    private static final long serialVersionUID = 1L;

    public CorruptedEvidenceException(String message) {
        super(message);
    }

    public CorruptedEvidenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
