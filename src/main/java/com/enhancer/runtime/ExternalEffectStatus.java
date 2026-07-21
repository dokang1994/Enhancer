package com.enhancer.runtime;

/** Durable lifecycle status of one external effect. */
public enum ExternalEffectStatus {
    PREPARED,
    APPLIED,
    DEDUPLICATED,
    COMPENSATED,
    REQUIRES_USER_RECOVERY;

    public boolean isTerminal() {
        return this != PREPARED;
    }
}
