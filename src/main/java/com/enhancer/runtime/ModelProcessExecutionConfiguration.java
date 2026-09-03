package com.enhancer.runtime;

import com.enhancer.tool.EvidenceStoragePolicy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Explicit scalar inputs retained by the parent for typed process validation. */
record ModelProcessExecutionConfiguration(
        SchedulerModelInvocationLimits invocationLimits,
        Set<String> deniedTools,
        long maximumReadBytes,
        Duration toolTimeout) {
    private static final int BASE_INVOCATION_ARGUMENTS = 8;
    private static final int CONFIGURATION_SCALARS = 5;

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

    List<String> appendInvocationArguments(List<String> base) {
        Objects.requireNonNull(base, "base must not be null");
        if (base.size() != BASE_INVOCATION_ARGUMENTS) {
            throw new IllegalArgumentException(
                    "model process base invocation requires eight arguments");
        }
        List<String> arguments = new ArrayList<>(
                base.size() + CONFIGURATION_SCALARS + deniedTools.size());
        arguments.addAll(base);
        arguments.add(Long.toString(invocationLimits.gatewayTimeout().toMillis()));
        arguments.add(Integer.toString(invocationLimits.maximumResponseCharacters()));
        arguments.add(Long.toString(maximumReadBytes));
        arguments.add(Long.toString(toolTimeout.toMillis()));
        arguments.add(Integer.toString(deniedTools.size()));
        deniedTools.stream().sorted().forEach(arguments::add);
        return List.copyOf(arguments);
    }

    static Optional<ModelProcessExecutionConfiguration> fromInvocationArguments(
            String[] arguments) {
        Objects.requireNonNull(arguments, "arguments must not be null");
        if (arguments.length == BASE_INVOCATION_ARGUMENTS) {
            return Optional.empty();
        }
        if (arguments.length < BASE_INVOCATION_ARGUMENTS + CONFIGURATION_SCALARS) {
            throw new IllegalArgumentException(
                    "typed model process invocation configuration is incomplete");
        }
        int offset = BASE_INVOCATION_ARGUMENTS;
        long gatewayTimeoutMillis = Long.parseLong(arguments[offset]);
        int maximumResponseCharacters = Integer.parseInt(arguments[offset + 1]);
        long maximumReadBytes = Long.parseLong(arguments[offset + 2]);
        long toolTimeoutMillis = Long.parseLong(arguments[offset + 3]);
        int deniedCount = Integer.parseInt(arguments[offset + 4]);
        if (deniedCount < 0
                || arguments.length
                        != BASE_INVOCATION_ARGUMENTS
                                + CONFIGURATION_SCALARS
                                + deniedCount) {
            throw new IllegalArgumentException(
                    "typed model process denied-Tool arguments do not match their count");
        }
        Set<String> denied = new LinkedHashSet<>();
        for (int index = 0; index < deniedCount; index++) {
            if (!denied.add(arguments[offset + CONFIGURATION_SCALARS + index])) {
                throw new IllegalArgumentException(
                        "typed model process denied Tools must be unique");
            }
        }
        return Optional.of(new ModelProcessExecutionConfiguration(
                new SchedulerModelInvocationLimits(
                        Duration.ofMillis(gatewayTimeoutMillis),
                        maximumResponseCharacters),
                denied,
                maximumReadBytes,
                Duration.ofMillis(toolTimeoutMillis)));
    }
}
