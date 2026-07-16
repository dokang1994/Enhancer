package com.enhancer.runtime;

public enum RuntimeAgentRunStatus {
    PLANNING,
    READY,
    EXECUTING,
    AWAITING_VERIFICATION,
    COMPLETED,
    FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
