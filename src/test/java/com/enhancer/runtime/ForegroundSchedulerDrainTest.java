package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ForegroundSchedulerDrainTest {
    private static final Duration LEASE = Duration.ofMinutes(5);

    @Test
    void continuesAfterVerifiedCompletionAndStopsOnIdle() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ArrayDeque<Optional<WorkItemDisposition>> outcomes =
                new ArrayDeque<>(List.of(
                        Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                        Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                        Optional.empty()));
        ForegroundSchedulerDrain drain = new ForegroundSchedulerDrain(ignored -> {
            calls.incrementAndGet();
            return outcomes.removeFirst();
        });

        SchedulerDrainResult result = drain.drain(8, LEASE);

        assertEquals(SchedulerDrainStopReason.IDLE, result.stopReason());
        assertEquals(3, result.cyclesInvoked());
        assertEquals(2, result.verifiedCompleted());
        assertEquals(0, result.failed());
        assertEquals(3, calls.get());
    }

    @Test
    void reachesTheLimitWithoutProbingTheQueueAgain() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ForegroundSchedulerDrain drain = new ForegroundSchedulerDrain(ignored -> {
            calls.incrementAndGet();
            return Optional.of(WorkItemDisposition.VERIFIED_COMPLETED);
        });

        SchedulerDrainResult result = drain.drain(2, LEASE);

        assertEquals(SchedulerDrainStopReason.LIMIT_REACHED, result.stopReason());
        assertEquals(2, result.cyclesInvoked());
        assertEquals(2, result.verifiedCompleted());
        assertEquals(0, result.failed());
        assertEquals(2, calls.get());
    }

    @Test
    void stopsOnTheFirstFailedDisposition() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ArrayDeque<Optional<WorkItemDisposition>> outcomes =
                new ArrayDeque<>(List.of(
                        Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                        Optional.of(WorkItemDisposition.FAILED),
                        Optional.of(WorkItemDisposition.VERIFIED_COMPLETED)));
        ForegroundSchedulerDrain drain = new ForegroundSchedulerDrain(ignored -> {
            calls.incrementAndGet();
            return outcomes.removeFirst();
        });

        SchedulerDrainResult result = drain.drain(8, LEASE);

        assertEquals(SchedulerDrainStopReason.FAILED, result.stopReason());
        assertEquals(2, result.cyclesInvoked());
        assertEquals(1, result.verifiedCompleted());
        assertEquals(1, result.failed());
        assertEquals(2, calls.get());
    }

    @Test
    void rejectsCycleBoundsBeforeInvokingTheWorker() {
        AtomicInteger calls = new AtomicInteger();
        ForegroundSchedulerDrain drain = new ForegroundSchedulerDrain(ignored -> {
            calls.incrementAndGet();
            return Optional.empty();
        });

        assertThrows(IllegalArgumentException.class, () -> drain.drain(0, LEASE));
        assertThrows(IllegalArgumentException.class, () ->
                drain.drain(
                        SingleWorkerSchedulerQueue.MAX_WORK_ITEMS + 1,
                        LEASE));
        assertEquals(0, calls.get());
    }
}
