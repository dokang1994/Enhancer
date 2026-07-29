package com.enhancer.runtime;

import com.enhancer.bus.ControlSignal;
import java.util.Objects;
import java.util.UUID;

public record DurableControlMessageReceiveResult(
        DurableControlMessageReceiveStatus status,
        String goalId,
        long runtimeRevision,
        String messageId,
        ControlSignal signal) {

    public DurableControlMessageReceiveResult {
        Objects.requireNonNull(status, "status must not be null");
        goalId = AgentRuntimeState.requireCanonicalGoalId(goalId);
        if (runtimeRevision < 0) {
            throw new IllegalArgumentException(
                    "runtimeRevision must not be negative");
        }
        messageId = requireCanonicalUuid(messageId, "messageId");
        Objects.requireNonNull(signal, "signal must not be null");
    }

    private static String requireCanonicalUuid(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        try {
            UUID parsed = UUID.fromString(value);
            if (!parsed.toString().equals(value)) {
                throw new IllegalArgumentException(
                        name + " must be a canonical UUID");
            }
            return value;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    name + " must be a canonical UUID", exception);
        }
    }
}
