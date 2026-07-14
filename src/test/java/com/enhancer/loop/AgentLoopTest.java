package com.enhancer.loop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AgentLoopTest {
    @Test
    void stopsWhenAStateCompletes() {
        AgentLoop loop = new AgentLoop(10, 3);
        AtomicInteger steps = new AtomicInteger();

        AgentLoopResult result = loop.run(running("start"), current -> {
            int step = steps.incrementAndGet();
            return step == 2
                    ? new AgentLoopState(AgentLoopStatus.COMPLETED, "done")
                    : running("progress-" + step);
        });

        assertEquals(AgentLoopStopReason.COMPLETED, result.stopReason());
        assertEquals(2, result.iterations());
        assertEquals(AgentLoopStatus.COMPLETED, result.state().status());
    }

    @Test
    void stopsWhenAStateFails() {
        AgentLoop loop = new AgentLoop(10, 3);

        AgentLoopResult result = loop.run(
                running("start"),
                current -> new AgentLoopState(AgentLoopStatus.FAILED, "failed"));

        assertEquals(AgentLoopStopReason.FAILED, result.stopReason());
        assertEquals(1, result.iterations());
    }

    @Test
    void stopsAtTheMaximumIterationCeiling() {
        AgentLoop loop = new AgentLoop(3, 2);
        AtomicInteger steps = new AtomicInteger();

        AgentLoopResult result = loop.run(
                running("start"),
                current -> running("progress-" + steps.incrementAndGet()));

        assertEquals(AgentLoopStopReason.MAX_ITERATIONS, result.stopReason());
        assertEquals(3, result.iterations());
        assertEquals("progress-3", result.state().progressKey());
    }

    @Test
    void stopsAfterConsecutiveUnchangedProgress() {
        AgentLoop loop = new AgentLoop(10, 3);

        AgentLoopResult result = loop.run(
                running("unchanged"),
                current -> running("unchanged"));

        assertEquals(AgentLoopStopReason.STAGNATED, result.stopReason());
        assertEquals(3, result.iterations());
    }

    @Test
    void terminalStatusWinsOverStagnation() {
        AgentLoop loop = new AgentLoop(10, 1);

        AgentLoopResult result = loop.run(
                running("same"),
                current -> new AgentLoopState(AgentLoopStatus.COMPLETED, "same"));

        assertEquals(AgentLoopStopReason.COMPLETED, result.stopReason());
        assertEquals(1, result.iterations());
    }

    @Test
    void maximumIterationWinsWhenTheCeilingAndStagnationCoincide() {
        AgentLoop loop = new AgentLoop(2, 2);

        AgentLoopResult result = loop.run(
                running("same"),
                current -> running("same"));

        assertEquals(AgentLoopStopReason.MAX_ITERATIONS, result.stopReason());
        assertEquals(2, result.iterations());
    }

    @Test
    void reportsZeroIterationsForAnInitiallyTerminalState() {
        AgentLoop loop = new AgentLoop();

        AgentLoopResult result = loop.run(
                new AgentLoopState(AgentLoopStatus.COMPLETED, "already-done"),
                current -> {
                    throw new AssertionError("The step must not run for a terminal state");
                });

        assertEquals(AgentLoopStopReason.COMPLETED, result.stopReason());
        assertEquals(0, result.iterations());
    }

    @Test
    void rejectsAResultWhoseReasonContradictsItsState() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AgentLoopResult(running("active"), AgentLoopStopReason.COMPLETED, 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AgentLoopResult(
                        new AgentLoopState(AgentLoopStatus.FAILED, "failed"),
                        AgentLoopStopReason.STAGNATED,
                        1));
    }

    @Test
    void rejectsInvalidLoopLimitsAndProgressKeys() {
        assertThrows(IllegalArgumentException.class, () -> new AgentLoop(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new AgentLoop(1, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AgentLoopState(AgentLoopStatus.RUNNING, " "));
    }

    private AgentLoopState running(String progressKey) {
        return new AgentLoopState(AgentLoopStatus.RUNNING, progressKey);
    }
}
