package com.enhancer.runtime;

import java.io.IOException;

public final class CorruptedPendingFinalizationException extends IOException {
    private static final long serialVersionUID = 1L;

    public CorruptedPendingFinalizationException(String reason) {
        super("Pending finalization state is corrupt: " + reason);
    }

    public CorruptedPendingFinalizationException(
            String reason,
            Throwable cause) {
        super("Pending finalization state is corrupt: " + reason, cause);
    }
}
