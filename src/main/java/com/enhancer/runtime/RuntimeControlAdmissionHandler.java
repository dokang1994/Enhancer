package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.MessageHandler;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.Objects;

/**
 * Gate 7-to-Gate 8 adapter that records a bound control request durably without applying it.
 */
public final class RuntimeControlAdmissionHandler implements MessageHandler {
    private final String goalId;
    private final AgentRuntimeStateStore store;
    private final Clock clock;

    public RuntimeControlAdmissionHandler(
            String goalId,
            AgentRuntimeStateStore store,
            Clock clock) {
        this.goalId = AgentRuntimeState.requireCanonicalGoalId(goalId);
        this.store = Objects.requireNonNull(
                store, "store must not be null");
        this.clock = Objects.requireNonNull(
                clock, "clock must not be null");
    }

    @Override
    public void handle(MessageEnvelope envelope) {
        try {
            DurableAgentRuntime.recoverForControlAdmission(
                            goalId, store, clock)
                    .recordControlRequest(envelope);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "runtime control request could not be persisted",
                    exception);
        }
    }
}
