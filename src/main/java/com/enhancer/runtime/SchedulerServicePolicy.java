package com.enhancer.runtime;

import java.time.Duration;
import java.util.Objects;

/** Finite operating bounds for one caller-driven Scheduler service run. */
public record SchedulerServicePolicy(
        int maxCycles,
        int maxConsecutiveIdleCycles,
        Duration idleWait) {
    public static final Duration MAX_IDLE_WAIT = Duration.ofHours(1);

    public SchedulerServicePolicy {
        if (maxCycles < 1
                || maxCycles > SingleWorkerSchedulerQueue.MAX_WORK_ITEMS) {
            throw new IllegalArgumentException(
                    "maxCycles must be between 1 and "
                            + SingleWorkerSchedulerQueue.MAX_WORK_ITEMS);
        }
        if (maxConsecutiveIdleCycles < 1
                || maxConsecutiveIdleCycles
                        > SingleWorkerSchedulerQueue.MAX_WORK_ITEMS) {
            throw new IllegalArgumentException(
                    "maxConsecutiveIdleCycles must be between 1 and "
                            + SingleWorkerSchedulerQueue.MAX_WORK_ITEMS);
        }
        Objects.requireNonNull(idleWait, "idleWait must not be null");
        if (idleWait.isZero()
                || idleWait.isNegative()
                || idleWait.compareTo(MAX_IDLE_WAIT) > 0) {
            throw new IllegalArgumentException(
                    "idleWait must be positive and no greater than "
                            + MAX_IDLE_WAIT);
        }
    }
}
