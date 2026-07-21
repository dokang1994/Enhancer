package com.enhancer.session;

import java.io.IOException;

/** A valid checkpoint cannot be changed because its task, writer, or working tree drifted. */
public final class DevelopmentSessionCheckpointConflictException extends IOException {
    private static final long serialVersionUID = 1L;

    public DevelopmentSessionCheckpointConflictException(String message) {
        super(message);
    }
}
