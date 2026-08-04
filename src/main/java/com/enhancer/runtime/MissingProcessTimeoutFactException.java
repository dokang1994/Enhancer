package com.enhancer.runtime;

import java.io.IOException;

/** Raised when a deterministic process-timeout point has no persisted artifact. */
public final class MissingProcessTimeoutFactException extends IOException {
    private static final long serialVersionUID = 1L;

    public MissingProcessTimeoutFactException(String reference) {
        super("process timeout fact is missing: " + reference);
    }
}
