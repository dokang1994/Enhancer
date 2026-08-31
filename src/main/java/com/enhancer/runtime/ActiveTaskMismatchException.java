package com.enhancer.runtime;

import java.util.Objects;

/** Reports which retained ModelWork field does not match current repository authority. */
public final class ActiveTaskMismatchException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public enum Reason {
        NOT_MODEL_WORK,
        TASK_ID,
        SOURCE_DOCUMENT,
        SOURCE_SHA256,
        ALLOWED_TOOLS
    }

    private final Reason reason;

    public ActiveTaskMismatchException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public Reason reason() {
        return reason;
    }
}
