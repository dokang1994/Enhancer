package com.enhancer.loop;

import java.util.Objects;

public record AgentLoopState(AgentLoopStatus status, String progressKey) {
    public AgentLoopState {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(progressKey, "progressKey must not be null");
        if (progressKey.isBlank()) {
            throw new IllegalArgumentException("progressKey must not be blank");
        }
    }
}
