package com.enhancer.runtime;

import com.enhancer.tool.EvidenceStoragePolicy;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Public bounded scalar inputs for the closed deterministic-fake Scheduler worker. */
public record DeterministicFakeModelSchedulerConfiguration(
        Duration gatewayTimeout,
        int maximumResponseCharacters,
        long maximumReadBytes,
        Duration toolTimeout,
        Set<String> deniedTools) {
    public static final int MAX_DENIED_TOOLS = 16;
    public static final int MAX_DENIED_TOOL_CHARACTERS = 128;

    public DeterministicFakeModelSchedulerConfiguration {
        Objects.requireNonNull(gatewayTimeout, "gatewayTimeout must not be null");
        Objects.requireNonNull(toolTimeout, "toolTimeout must not be null");
        Objects.requireNonNull(deniedTools, "deniedTools must not be null");
        SchedulerModelInvocationLimits limits = new SchedulerModelInvocationLimits(
                gatewayTimeout, maximumResponseCharacters);
        if (gatewayTimeout.compareTo(toolTimeout) >= 0) {
            throw new IllegalArgumentException(
                    "toolTimeout must be greater than gatewayTimeout");
        }
        if (maximumReadBytes < 1
                || maximumReadBytes > EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES) {
            throw new IllegalArgumentException(
                    "maximumReadBytes is outside the supported evidence bound");
        }
        if (deniedTools.size() > MAX_DENIED_TOOLS) {
            throw new IllegalArgumentException(
                    "deniedTools exceeds the supported count bound");
        }
        TreeSet<String> canonicalDeniedTools = new TreeSet<>();
        for (String deniedTool : deniedTools) {
            Objects.requireNonNull(
                    deniedTool, "deniedTools must not contain null");
            if (deniedTool.isBlank()
                    || deniedTool.length() > MAX_DENIED_TOOL_CHARACTERS) {
                throw new IllegalArgumentException(
                        "deniedTools contains a blank or oversized name");
            }
            canonicalDeniedTools.add(deniedTool);
        }
        deniedTools = Collections.unmodifiableSet(canonicalDeniedTools);
        new ModelProcessExecutionConfiguration(
                limits, deniedTools, maximumReadBytes, toolTimeout);
    }

    ModelProcessExecutionConfiguration toProcessConfiguration() {
        return new ModelProcessExecutionConfiguration(
                new SchedulerModelInvocationLimits(
                        gatewayTimeout, maximumResponseCharacters),
                deniedTools,
                maximumReadBytes,
                toolTimeout);
    }
}
