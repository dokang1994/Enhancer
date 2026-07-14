package com.enhancer.loop;

import java.util.Objects;

public record AgentLoopResult(
        AgentLoopState state,
        AgentLoopStopReason stopReason,
        int iterations) {

    public AgentLoopResult {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(stopReason, "stopReason must not be null");
        if (iterations < 0) {
            throw new IllegalArgumentException("iterations must not be negative");
        }

        boolean consistent = switch (stopReason) {
            case COMPLETED -> state.status() == AgentLoopStatus.COMPLETED;
            case FAILED -> state.status() == AgentLoopStatus.FAILED;
            case MAX_ITERATIONS, STAGNATED -> state.status() == AgentLoopStatus.RUNNING;
        };
        if (!consistent) {
            throw new IllegalArgumentException(
                    "stopReason " + stopReason + " contradicts state status " + state.status());
        }
        if ((stopReason == AgentLoopStopReason.MAX_ITERATIONS
                        || stopReason == AgentLoopStopReason.STAGNATED)
                && iterations == 0) {
            throw new IllegalArgumentException(stopReason + " requires at least one iteration");
        }
    }
}
