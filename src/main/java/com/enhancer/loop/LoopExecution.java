package com.enhancer.loop;

import java.util.Objects;

record LoopExecution<S extends AgentLoopSnapshot>(
        S state,
        AgentLoopStopReason stopReason,
        int iterations) {

    LoopExecution {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(stopReason, "stopReason must not be null");
        if (iterations < 0) {
            throw new IllegalArgumentException("iterations must not be negative");
        }
    }
}
