package com.enhancer.runtime;

/** Why a further AgentRun for one WorkItem is refused. */
public enum AgentRunRetryRefusalReason {
    /** The supplied latest AgentRun attempt was not failed, so it is not a retry candidate. */
    NOT_FAILED,
    /** At least one external effect is PREPARED, so retry could hide a replayed effect. */
    UNRESOLVED_EXTERNAL_EFFECT,
    /** At least one external effect requires explicit user recovery. */
    EFFECT_REQUIRES_USER_RECOVERY,
    /** At least one applied or deduplicated effect has no cross-attempt replay contract. */
    NON_COMPENSATED_EXTERNAL_EFFECT,
    /** The attempt budget is exhausted. */
    ATTEMPTS_EXHAUSTED
}
