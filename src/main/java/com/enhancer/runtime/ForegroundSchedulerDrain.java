package com.enhancer.runtime;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** Runs only work already available through a finite sequence of recoverable cycles. */
public final class ForegroundSchedulerDrain {
    private final SchedulerCycle cycle;

    public ForegroundSchedulerDrain(DurableAgentRunWorker worker) {
        this(Objects.requireNonNull(worker, "worker must not be null")::runOneCycle);
    }

    ForegroundSchedulerDrain(SchedulerCycle cycle) {
        this.cycle = Objects.requireNonNull(cycle, "cycle must not be null");
    }

    public SchedulerDrainResult drain(
            int maxCycles,
            Duration leaseDuration) throws IOException {
        if (maxCycles < 1
                || maxCycles > SingleWorkerSchedulerQueue.MAX_WORK_ITEMS) {
            throw new IllegalArgumentException(
                    "maxCycles must be between 1 and "
                            + SingleWorkerSchedulerQueue.MAX_WORK_ITEMS);
        }
        Objects.requireNonNull(
                leaseDuration, "leaseDuration must not be null");
        if (leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException(
                    "leaseDuration must be positive");
        }

        int verifiedCompleted = 0;
        for (int cyclesInvoked = 1;
                cyclesInvoked <= maxCycles;
                cyclesInvoked++) {
            Optional<WorkItemDisposition> disposition =
                    cycle.run(leaseDuration);
            if (disposition.isEmpty()) {
                return new SchedulerDrainResult(
                        SchedulerDrainStopReason.IDLE,
                        cyclesInvoked,
                        verifiedCompleted,
                        0);
            }
            switch (disposition.orElseThrow()) {
                case VERIFIED_COMPLETED -> verifiedCompleted++;
                case FAILED -> {
                    return new SchedulerDrainResult(
                            SchedulerDrainStopReason.FAILED,
                            cyclesInvoked,
                            verifiedCompleted,
                            1);
                }
            }
            if (cyclesInvoked == maxCycles) {
                return new SchedulerDrainResult(
                        SchedulerDrainStopReason.LIMIT_REACHED,
                        cyclesInvoked,
                        verifiedCompleted,
                        0);
            }
        }
        throw new IllegalStateException("Scheduler drain exceeded its cycle bound");
    }

    @FunctionalInterface
    interface SchedulerCycle {
        Optional<WorkItemDisposition> run(Duration leaseDuration)
                throws IOException;
    }
}
