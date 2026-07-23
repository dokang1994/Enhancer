package com.enhancer.runtime;

import java.util.Objects;

/** Bounded observations from one foreground drain invocation. */
public record SchedulerDrainResult(
        SchedulerDrainStopReason stopReason,
        int cyclesInvoked,
        int verifiedCompleted,
        int failed) {

    public SchedulerDrainResult {
        Objects.requireNonNull(stopReason, "stopReason must not be null");
        if (cyclesInvoked < 1
                || cyclesInvoked > SingleWorkerSchedulerQueue.MAX_WORK_ITEMS) {
            throw new IllegalArgumentException(
                    "cyclesInvoked must be between 1 and "
                            + SingleWorkerSchedulerQueue.MAX_WORK_ITEMS);
        }
        if (verifiedCompleted < 0 || failed < 0) {
            throw new IllegalArgumentException(
                    "drain outcome counts must not be negative");
        }
        switch (stopReason) {
            case IDLE -> {
                if (failed != 0 || cyclesInvoked != verifiedCompleted + 1) {
                    throw new IllegalArgumentException(
                            "idle drain result must end after one non-work cycle");
                }
            }
            case FAILED -> {
                if (failed != 1 || cyclesInvoked != verifiedCompleted + 1) {
                    throw new IllegalArgumentException(
                            "failed drain result must end at the first failed cycle");
                }
            }
            case LIMIT_REACHED -> {
                if (failed != 0 || cyclesInvoked != verifiedCompleted) {
                    throw new IllegalArgumentException(
                            "limit drain result must contain only verified cycles");
                }
            }
        }
    }
}
