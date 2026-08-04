package com.enhancer.runtime;

public enum RuntimeAgentRunStatus {
    PLANNING,
    READY,
    EXECUTING,
    AWAITING_VERIFICATION,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    public boolean hasResult() {
        return this == COMPLETED || this == FAILED;
    }
}
