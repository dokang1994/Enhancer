package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class BoundedSchedulerServiceTest {
    private static final Duration LEASE = Duration.ofMinutes(5);
    private static final Duration IDLE_WAIT = Duration.ofMillis(25);

    @Test
    void policyRejectsUnboundedOrNonPositiveInputs() {
        assertThrows(IllegalArgumentException.class, () ->
                new SchedulerServicePolicy(0, 1, IDLE_WAIT));
        assertThrows(IllegalArgumentException.class, () ->
                new SchedulerServicePolicy(
                        SingleWorkerSchedulerQueue.MAX_WORK_ITEMS + 1,
                        1,
                        IDLE_WAIT));
        assertThrows(IllegalArgumentException.class, () ->
                new SchedulerServicePolicy(1, 0, IDLE_WAIT));
        assertThrows(IllegalArgumentException.class, () ->
                new SchedulerServicePolicy(
                        1,
                        SingleWorkerSchedulerQueue.MAX_WORK_ITEMS + 1,
                        IDLE_WAIT));
        assertThrows(IllegalArgumentException.class, () ->
                new SchedulerServicePolicy(1, 1, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () ->
                new SchedulerServicePolicy(
                        1,
                        1,
                        SchedulerServicePolicy.MAX_IDLE_WAIT.plusNanos(1)));
    }

    @Test
    void stopsBeforeInvokingAWorkerWhenTheCallerAlreadyRequestedStop()
            throws Exception {
        AtomicInteger cycleCalls = new AtomicInteger();
        AtomicInteger waits = new AtomicInteger();
        BoundedSchedulerService service = new BoundedSchedulerService(
                ignored -> {
                    cycleCalls.incrementAndGet();
                    return Optional.empty();
                },
                ignored -> waits.incrementAndGet());

        SchedulerServiceResult result = service.serve(
                policy(8, 2),
                LEASE,
                () -> true);

        assertEquals(SchedulerServiceStopReason.STOP_REQUESTED,
                result.stopReason());
        assertEquals(0, result.cyclesInvoked());
        assertEquals(0, result.verifiedCompleted());
        assertEquals(0, result.idleCycles());
        assertEquals(0, result.failed());
        assertEquals(0, cycleCalls.get());
        assertEquals(0, waits.get());
    }

    @Test
    void waitsAcrossIdleCyclesAndResetsConsecutiveIdleAfterVerifiedWork()
            throws Exception {
        ArrayDeque<Optional<WorkItemDisposition>> outcomes =
                new ArrayDeque<>(List.of(
                        Optional.empty(),
                        Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                        Optional.empty(),
                        Optional.empty()));
        AtomicInteger waits = new AtomicInteger();
        BoundedSchedulerService service = new BoundedSchedulerService(
                ignored -> outcomes.removeFirst(),
                duration -> {
                    assertEquals(IDLE_WAIT, duration);
                    waits.incrementAndGet();
                });

        SchedulerServiceResult result = service.serve(
                policy(8, 2),
                LEASE,
                () -> false);

        assertEquals(SchedulerServiceStopReason.IDLE_LIMIT,
                result.stopReason());
        assertEquals(4, result.cyclesInvoked());
        assertEquals(1, result.verifiedCompleted());
        assertEquals(3, result.idleCycles());
        assertEquals(0, result.failed());
        assertEquals(2, waits.get());
    }

    @Test
    void stopsAtTheFirstFailedDisposition() throws Exception {
        ArrayDeque<Optional<WorkItemDisposition>> outcomes =
                new ArrayDeque<>(List.of(
                        Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                        Optional.of(WorkItemDisposition.FAILED),
                        Optional.of(WorkItemDisposition.VERIFIED_COMPLETED)));
        BoundedSchedulerService service = new BoundedSchedulerService(
                ignored -> outcomes.removeFirst(),
                ignored -> {
                    throw new AssertionError("failure must not idle-wait");
                });

        SchedulerServiceResult result = service.serve(
                policy(8, 2),
                LEASE,
                () -> false);

        assertEquals(SchedulerServiceStopReason.FAILED, result.stopReason());
        assertEquals(2, result.cyclesInvoked());
        assertEquals(1, result.verifiedCompleted());
        assertEquals(0, result.idleCycles());
        assertEquals(1, result.failed());
        assertEquals(1, outcomes.size());
    }

    @Test
    void stopsAtTheTotalCycleLimitWithoutAnotherProbe() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        BoundedSchedulerService service = new BoundedSchedulerService(
                ignored -> {
                    calls.incrementAndGet();
                    return Optional.of(WorkItemDisposition.VERIFIED_COMPLETED);
                },
                ignored -> {
                    throw new AssertionError("verified work must not idle-wait");
                });

        SchedulerServiceResult result = service.serve(
                policy(2, 2),
                LEASE,
                () -> false);

        assertEquals(SchedulerServiceStopReason.CYCLE_LIMIT,
                result.stopReason());
        assertEquals(2, result.cyclesInvoked());
        assertEquals(2, result.verifiedCompleted());
        assertEquals(2, calls.get());
    }

    @Test
    void stopsAtTheConsecutiveIdleLimitWithoutWaitingAgain() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger waits = new AtomicInteger();
        BoundedSchedulerService service = new BoundedSchedulerService(
                ignored -> {
                    calls.incrementAndGet();
                    return Optional.empty();
                },
                ignored -> waits.incrementAndGet());

        SchedulerServiceResult result = service.serve(
                policy(8, 2),
                LEASE,
                () -> false);

        assertEquals(SchedulerServiceStopReason.IDLE_LIMIT,
                result.stopReason());
        assertEquals(2, result.cyclesInvoked());
        assertEquals(2, result.idleCycles());
        assertEquals(2, calls.get());
        assertEquals(1, waits.get());
    }

    @Test
    void interruptedIdleWaitRestoresTheInterruptFlagAndStops()
            throws Exception {
        AtomicInteger calls = new AtomicInteger();
        BoundedSchedulerService service = new BoundedSchedulerService(
                ignored -> {
                    calls.incrementAndGet();
                    return Optional.empty();
                },
                ignored -> {
                    throw new InterruptedException("stop");
                });
        assertFalse(Thread.currentThread().isInterrupted());

        SchedulerServiceResult result = service.serve(
                policy(8, 2),
                LEASE,
                () -> false);

        try {
            assertEquals(SchedulerServiceStopReason.INTERRUPTED,
                    result.stopReason());
            assertEquals(1, result.cyclesInvoked());
            assertEquals(1, result.idleCycles());
            assertEquals(1, calls.get());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void checksTheLocalStopSignalBeforeEveryCycle() throws Exception {
        AtomicBoolean stop = new AtomicBoolean();
        AtomicInteger calls = new AtomicInteger();
        BoundedSchedulerService service = new BoundedSchedulerService(
                ignored -> {
                    calls.incrementAndGet();
                    stop.set(true);
                    return Optional.of(WorkItemDisposition.VERIFIED_COMPLETED);
                },
                ignored -> {
                    throw new AssertionError("verified work must not idle-wait");
                });

        SchedulerServiceResult result = service.serve(
                policy(8, 2),
                LEASE,
                stop::get);

        assertEquals(SchedulerServiceStopReason.STOP_REQUESTED,
                result.stopReason());
        assertEquals(1, result.cyclesInvoked());
        assertEquals(1, result.verifiedCompleted());
        assertEquals(1, calls.get());
    }

    @Test
    void resultRejectsImpossibleOutcomeCounts() {
        assertThrows(IllegalArgumentException.class, () ->
                new SchedulerServiceResult(
                        SchedulerServiceStopReason.FAILED,
                        1,
                        1,
                        0,
                        0));
        assertThrows(IllegalArgumentException.class, () ->
                new SchedulerServiceResult(
                        SchedulerServiceStopReason.IDLE_LIMIT,
                        1,
                        1,
                        0,
                        0));
    }

    private SchedulerServicePolicy policy(
            int maxCycles,
            int maxConsecutiveIdleCycles) {
        return new SchedulerServicePolicy(
                maxCycles,
                maxConsecutiveIdleCycles,
                IDLE_WAIT);
    }
}
