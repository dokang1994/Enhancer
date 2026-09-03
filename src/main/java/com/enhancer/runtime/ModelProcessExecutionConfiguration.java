package com.enhancer.runtime;

import com.enhancer.tool.EvidenceStoragePolicy;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Explicit scalar inputs retained by the parent for typed process validation. */
record ModelProcessExecutionConfiguration(
        SchedulerModelInvocationLimits invocationLimits,
        Set<String> deniedTools,
        long maximumReadBytes,
        Duration toolTimeout) {

    ModelProcessExecutionConfiguration {
        Objects.requireNonNull(invocationLimits, "invocationLimits must not be null");
        Objects.requireNonNull(deniedTools, "deniedTools must not be null");
        Objects.requireNonNull(toolTimeout, "toolTimeout must not be null");
        deniedTools = toolNames(deniedTools);
        if (maximumReadBytes <= 0
                || maximumReadBytes > EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES) {
            throw new IllegalArgumentException(
                    "maximumReadBytes is outside the supported evidence bound");
        }
        requireMillisecondPrecision(
                invocationLimits.gatewayTimeout(), "gatewayTimeout");
        requireMillisecondPrecision(toolTimeout, "toolTimeout");
    }

    private static Set<String> toolNames(Set<String> values) {
        Set<String> snapshot = new LinkedHashSet<>();
        for (String value : values) {
            Objects.requireNonNull(value, "deniedTools must not contain null");
            if (value.isBlank()) {
                throw new IllegalArgumentException(
                        "deniedTools must not contain blank names");
            }
            snapshot.add(value);
        }
        return Set.copyOf(snapshot);
    }

    private static void requireMillisecondPrecision(Duration value, String name) {
        if (value.isZero()
                || value.isNegative()
                || !Duration.ofMillis(value.toMillis()).equals(value)) {
            throw new IllegalArgumentException(
                    name + " must be positive and use millisecond precision");
        }
    }
}
