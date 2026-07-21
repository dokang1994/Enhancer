package com.enhancer.runtime;

/** Why a further AgentRun for one WorkItem is refused. */
public enum AgentRunRetryRefusalReason {
    /** The last disposition was not a failure, so it is not a retry candidate. */
    NOT_FAILED,
    /** At least one external effect is PREPARED, so retry could hide a replayed effect. */
    UNRESOLVED_EXTERNAL_EFFECT,
    /** At least one external effect requires explicit user recovery. */
    EFFECT_REQUIRES_USER_RECOVERY,
    /** The attempt budget is exhausted. */
    ATTEMPTS_EXHAUSTED
}
