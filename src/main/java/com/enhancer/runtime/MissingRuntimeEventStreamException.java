package com.enhancer.runtime;

import java.io.IOException;

/** A requested Goal has no durable runtime-event stream. */
public final class MissingRuntimeEventStreamException extends IOException {
    private static final long serialVersionUID = 1L;

    public MissingRuntimeEventStreamException(String goalId) {
        super("runtime event stream does not exist: " + goalId);
    }
}
