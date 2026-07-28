package com.enhancer.runtime;

/** Explicit bounded termination reasons for one caller-driven Scheduler service run. */
public enum SchedulerServiceStopReason {
    STOP_REQUESTED,
    INTERRUPTED,
    FAILED,
    CYCLE_LIMIT,
    IDLE_LIMIT
}
