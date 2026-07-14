package com.enhancer.loop;

import java.util.Objects;
import java.util.function.UnaryOperator;

public final class AgentLoop {
    public static final int DEFAULT_MAX_ITERATIONS = 20;
    public static final int DEFAULT_STAGNATION_THRESHOLD = 3;

    private final int maxIterations;
    private final int stagnationThreshold;

    public AgentLoop() {
        this(DEFAULT_MAX_ITERATIONS, DEFAULT_STAGNATION_THRESHOLD);
    }

    public AgentLoop(int maxIterations, int stagnationThreshold) {
        if (maxIterations <= 0) {
            throw new IllegalArgumentException("maxIterations must be positive");
        }
        if (stagnationThreshold <= 0) {
            throw new IllegalArgumentException("stagnationThreshold must be positive");
        }
        this.maxIterations = maxIterations;
        this.stagnationThreshold = stagnationThreshold;
    }

    public AgentLoopResult run(AgentLoopState initialState, AgentLoopStep step) {
        Objects.requireNonNull(initialState, "initialState must not be null");
        Objects.requireNonNull(step, "step must not be null");

        LoopExecution<AgentLoopState> execution = runSnapshots(initialState, step::execute);
        return new AgentLoopResult(
                execution.state(),
                execution.stopReason(),
                execution.iterations());
    }

    <S extends AgentLoopSnapshot> LoopExecution<S> runSnapshots(
            S initialState,
            UnaryOperator<S> step) {
        S state = Objects.requireNonNull(initialState, "initialState must not be null");
        Objects.requireNonNull(step, "step must not be null");

        AgentLoopStopReason initialStopReason = terminalStopReason(state.status());
        if (initialStopReason != null) {
            return new LoopExecution<>(state, initialStopReason, 0);
        }

        int iterations = 0;
        int stagnantIterations = 0;
        String previousProgressKey = state.progressKey();

        while (iterations < maxIterations) {
            state = Objects.requireNonNull(
                    step.apply(state),
                    "step result must not be null");
            iterations++;

            AgentLoopStopReason terminalStopReason = terminalStopReason(state.status());
            if (terminalStopReason != null) {
                return new LoopExecution<>(state, terminalStopReason, iterations);
            }

            if (iterations >= maxIterations) {
                return new LoopExecution<>(
                        state,
                        AgentLoopStopReason.MAX_ITERATIONS,
                        iterations);
            }

            if (state.progressKey().equals(previousProgressKey)) {
                stagnantIterations++;
            } else {
                stagnantIterations = 0;
            }
            if (stagnantIterations >= stagnationThreshold) {
                return new LoopExecution<>(
                        state,
                        AgentLoopStopReason.STAGNATED,
                        iterations);
            }

            previousProgressKey = state.progressKey();
        }

        throw new IllegalStateException("Agent Loop terminated without a stop reason");
    }

    private AgentLoopStopReason terminalStopReason(AgentLoopStatus status) {
        return switch (status) {
            case COMPLETED -> AgentLoopStopReason.COMPLETED;
            case AWAITING_VERIFICATION -> AgentLoopStopReason.AWAITING_VERIFICATION;
            case FAILED -> AgentLoopStopReason.FAILED;
            case RUNNING -> null;
        };
    }
}
