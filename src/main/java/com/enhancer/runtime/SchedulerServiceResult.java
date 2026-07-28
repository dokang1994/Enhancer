package com.enhancer.runtime;

import java.util.Objects;

/** Exact observations from one bounded caller-driven Scheduler service run. */
public record SchedulerServiceResult(
        SchedulerServiceStopReason stopReason,
        int cyclesInvoked,
        int verifiedCompleted,
        int idleCycles,
        int failed) {

    public SchedulerServiceResult {
        Objects.requireNonNull(stopReason, "stopReason must not be null");
        if (cyclesInvoked < 0
                || cyclesInvoked > SingleWorkerSchedulerQueue.MAX_WORK_ITEMS) {
            throw new IllegalArgumentException(
                    "cyclesInvoked must be between 0 and "
                            + SingleWorkerSchedulerQueue.MAX_WORK_ITEMS);
        }
        if (verifiedCompleted < 0 || idleCycles < 0) {
            throw new IllegalArgumentException(
                    "service outcome counts must not be negative");
        }
        if (failed < 0 || failed > 1) {
            throw new IllegalArgumentException(
                    "failed must be zero or one");
        }
        if (cyclesInvoked != verifiedCompleted + idleCycles + failed) {
            throw new IllegalArgumentException(
                    "cycle outcome counts must equal cyclesInvoked");
        }
        if (stopReason == SchedulerServiceStopReason.FAILED
                && failed != 1) {
            throw new IllegalArgumentException(
                    "failed stop must retain one failed cycle");
        }
        if (stopReason != SchedulerServiceStopReason.FAILED
                && failed != 0) {
            throw new IllegalArgumentException(
                    "only failed stop may retain a failed cycle");
        }
        if (stopReason == SchedulerServiceStopReason.IDLE_LIMIT
                && idleCycles == 0) {
            throw new IllegalArgumentException(
                    "idle-limit stop must retain an idle cycle");
        }
        if (stopReason == SchedulerServiceStopReason.CYCLE_LIMIT
                && cyclesInvoked == 0) {
            throw new IllegalArgumentException(
                    "cycle-limit stop must retain an invoked cycle");
        }
    }
}
