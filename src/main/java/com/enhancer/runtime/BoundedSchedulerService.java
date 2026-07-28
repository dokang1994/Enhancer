package com.enhancer.runtime;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Caller-driven, finite idle-polling lifecycle over the existing recoverable Scheduler
 * cycle.
 */
public final class BoundedSchedulerService {
    private final SchedulerCycle cycle;
    private final IdleWait idleWait;

    public BoundedSchedulerService(DurableAgentRunWorker worker) {
        this(
                Objects.requireNonNull(
                        worker, "worker must not be null")::runOneCycle,
                duration -> TimeUnit.NANOSECONDS.sleep(duration.toNanos()));
    }

    BoundedSchedulerService(
            SchedulerCycle cycle,
            IdleWait idleWait) {
        this.cycle = Objects.requireNonNull(
                cycle, "cycle must not be null");
        this.idleWait = Objects.requireNonNull(
                idleWait, "idleWait must not be null");
    }

    public SchedulerServiceResult serve(
            SchedulerServicePolicy policy,
            Duration leaseDuration,
            BooleanSupplier stopRequested) throws IOException {
        Objects.requireNonNull(policy, "policy must not be null");
        Objects.requireNonNull(
                leaseDuration, "leaseDuration must not be null");
        if (leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException(
                    "leaseDuration must be positive");
        }
        Objects.requireNonNull(
                stopRequested, "stopRequested must not be null");

        int cyclesInvoked = 0;
        int verifiedCompleted = 0;
        int idleCycles = 0;
        int consecutiveIdleCycles = 0;
        while (cyclesInvoked < policy.maxCycles()) {
            if (stopRequested.getAsBoolean()) {
                return result(
                        SchedulerServiceStopReason.STOP_REQUESTED,
                        cyclesInvoked,
                        verifiedCompleted,
                        idleCycles,
                        0);
            }

            Optional<WorkItemDisposition> disposition =
                    cycle.run(leaseDuration);
            cyclesInvoked++;
            if (disposition.isPresent()) {
                switch (disposition.orElseThrow()) {
                    case VERIFIED_COMPLETED -> {
                        verifiedCompleted++;
                        consecutiveIdleCycles = 0;
                    }
                    case FAILED -> {
                        return result(
                                SchedulerServiceStopReason.FAILED,
                                cyclesInvoked,
                                verifiedCompleted,
                                idleCycles,
                                1);
                    }
                }
            } else {
                idleCycles++;
                consecutiveIdleCycles++;
            }

            if (cyclesInvoked == policy.maxCycles()) {
                return result(
                        SchedulerServiceStopReason.CYCLE_LIMIT,
                        cyclesInvoked,
                        verifiedCompleted,
                        idleCycles,
                        0);
            }
            if (disposition.isEmpty()) {
                if (consecutiveIdleCycles
                        == policy.maxConsecutiveIdleCycles()) {
                    return result(
                            SchedulerServiceStopReason.IDLE_LIMIT,
                            cyclesInvoked,
                            verifiedCompleted,
                            idleCycles,
                            0);
                }
                try {
                    idleWait.await(policy.idleWait());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return result(
                            SchedulerServiceStopReason.INTERRUPTED,
                            cyclesInvoked,
                            verifiedCompleted,
                            idleCycles,
                            0);
                }
            }
        }
        throw new IllegalStateException(
                "Scheduler service exceeded its cycle bound");
    }

    private SchedulerServiceResult result(
            SchedulerServiceStopReason reason,
            int cyclesInvoked,
            int verifiedCompleted,
            int idleCycles,
            int failed) {
        return new SchedulerServiceResult(
                reason,
                cyclesInvoked,
                verifiedCompleted,
                idleCycles,
                failed);
    }

    @FunctionalInterface
    interface SchedulerCycle {
        Optional<WorkItemDisposition> run(Duration leaseDuration)
                throws IOException;
    }

    @FunctionalInterface
    interface IdleWait {
        void await(Duration duration) throws InterruptedException;
    }
}
