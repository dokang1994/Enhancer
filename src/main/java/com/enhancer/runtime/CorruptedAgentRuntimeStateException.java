package com.enhancer.runtime;

import java.io.IOException;

public final class CorruptedAgentRuntimeStateException extends IOException {
    private static final long serialVersionUID = 1L;

    public CorruptedAgentRuntimeStateException(
            String goalId,
            String reason) {
        super("Agent runtime state is corrupt for "
                + goalId + ": " + reason);
    }

    public CorruptedAgentRuntimeStateException(
            String goalId,
            String reason,
            Throwable cause) {
        super("Agent runtime state is corrupt for "
                + goalId + ": " + reason,
                cause);
    }
}
