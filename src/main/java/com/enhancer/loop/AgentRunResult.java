package com.enhancer.loop;

import com.enhancer.tool.ExecutionPolicy;
import java.util.Objects;

public final class AgentRunResult {
    private final AgentRunState state;
    private final AgentLoopStopReason stopReason;
    private final int iterations;
    private final ExecutionPolicy executionPolicy;

    AgentRunResult(
            AgentRunState state,
            AgentLoopStopReason stopReason,
            int iterations,
            ExecutionPolicy executionPolicy) {
        this.state = Objects.requireNonNull(state, "state must not be null");
        this.stopReason = Objects.requireNonNull(
                stopReason,
                "stopReason must not be null");
        this.executionPolicy = Objects.requireNonNull(
                executionPolicy,
                "executionPolicy must not be null");
        if (iterations <= 0) {
            throw new IllegalArgumentException("Agent run requires at least one iteration");
        }
        this.iterations = iterations;

        boolean consistent = switch (stopReason) {
            case AWAITING_VERIFICATION ->
                    state.status() == AgentLoopStatus.AWAITING_VERIFICATION;
            case FAILED -> state.status() == AgentLoopStatus.FAILED;
            case MAX_ITERATIONS, STAGNATED -> state.status() == AgentLoopStatus.RUNNING;
            case COMPLETED -> false;
        };
        if (!consistent) {
            throw new IllegalArgumentException(
                    "stopReason " + stopReason
                            + " contradicts Agent run state " + state.status());
        }
    }

    public AgentRunState state() {
        return state;
    }

    public AgentLoopStopReason stopReason() {
        return stopReason;
    }

    public int iterations() {
        return iterations;
    }

    public ExecutionPolicy executionPolicy() {
        return executionPolicy;
    }
}
