package com.enhancer.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Immutable authoritative fact that the parent watchdog timed out one isolated AgentRun. */
public record ProcessTimeoutFact(
        String schemaVersion,
        Instant occurredAt,
        RuntimeEventBinding binding,
        String agentRunId,
        Duration timeout,
        String reason) {

    public static final String SCHEMA_VERSION = "process-timeout-v1";

    public ProcessTimeoutFact {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException(
                    "process timeout schema version is unsupported");
        }
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(binding, "binding must not be null");
        agentRunId = RuntimeIdentity.canonicalUuid(agentRunId, "agentRunId");
        timeout = Objects.requireNonNull(timeout, "timeout must not be null");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        reason = RuntimeEventContractSupport.bounded(
                reason,
                "reason",
                IsolatedWorkerOutcome.MAX_REASON_CHARACTERS);
    }

    public static ProcessTimeoutFact create(
            Instant occurredAt,
            RuntimeEventBinding binding,
            String agentRunId,
            Duration timeout,
            String reason) {
        return new ProcessTimeoutFact(
                SCHEMA_VERSION,
                occurredAt,
                binding,
                agentRunId,
                timeout,
                reason);
    }

    public String reference() {
        return reference(binding.goalId(), agentRunId);
    }

    public static String reference(String goalId, String agentRunId) {
        return "process-timeout/"
                + RuntimeIdentity.canonicalUuid(goalId, "goalId")
                + "/"
                + RuntimeIdentity.canonicalUuid(agentRunId, "agentRunId");
    }
}
