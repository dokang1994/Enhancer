package com.enhancer.runtime;

import java.io.IOException;

public final class MissingAgentRuntimeStateException extends IOException {
    private static final long serialVersionUID = 1L;

    public MissingAgentRuntimeStateException(String goalId) {
        super("Agent runtime state does not exist: " + goalId);
    }
}
