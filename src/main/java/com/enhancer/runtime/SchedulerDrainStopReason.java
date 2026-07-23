package com.enhancer.runtime;

/** Why one finite foreground Scheduler drain invocation stopped. */
public enum SchedulerDrainStopReason {
    IDLE,
    FAILED,
    LIMIT_REACHED
}
