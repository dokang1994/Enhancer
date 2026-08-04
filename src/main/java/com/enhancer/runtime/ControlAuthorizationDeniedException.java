package com.enhancer.runtime;

/** Explicit refusal from the trusted control-request authorizer. */
public final class ControlAuthorizationDeniedException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    public ControlAuthorizationDeniedException(String reason) {
        super("control authorization was denied: " + reason);
    }
}
