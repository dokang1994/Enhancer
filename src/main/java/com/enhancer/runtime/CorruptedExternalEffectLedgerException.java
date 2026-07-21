package com.enhancer.runtime;

import java.io.IOException;

public final class CorruptedExternalEffectLedgerException extends IOException {
    private static final long serialVersionUID = 1L;

    public CorruptedExternalEffectLedgerException(String message) {
        super(message);
    }

    public CorruptedExternalEffectLedgerException(
            String message,
            Throwable cause) {
        super(message, cause);
    }
}
