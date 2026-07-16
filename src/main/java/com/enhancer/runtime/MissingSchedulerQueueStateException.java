package com.enhancer.runtime;

import java.io.IOException;

public final class MissingSchedulerQueueStateException extends IOException {
    private static final long serialVersionUID = 1L;

    public MissingSchedulerQueueStateException(String queueId) {
        super("Scheduler queue state is missing: " + queueId);
    }
}
