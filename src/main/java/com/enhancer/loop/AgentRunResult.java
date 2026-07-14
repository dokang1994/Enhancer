package com.enhancer.loop;

import java.util.Objects;

public record AgentRunResult(
        AgentRunState state,
        AgentLoopStopReason stopReason,
        int iterations) {

    public AgentRunResult {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(stopReason, "stopReason must not be null");
        if (iterations <= 0) {
            throw new IllegalArgumentException("Agent run requires at least one iteration");
        }

        boolean consistent = switch (stopReason) {
            case AWAITING_VERIFICATION ->
                    state.status() == AgentLoopStatus.AWAITING_VERIFICATION;
            case FAILED -> state.status() == AgentLoopStatus.FAILED;
            case MAX_ITERATIONS, STAGNATED -> state.status() == AgentLoopStatus.RUNNING;
            case COMPLETED -> false;
        };
        if (!consistent) {
            throw new IllegalArgumentException(
                    "stopReason " + stopReason + " contradicts Agent run state " + state.status());
        }
    }
}
