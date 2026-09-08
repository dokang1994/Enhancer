package com.enhancer.cli;

import com.enhancer.model.ModelRequest;
import com.enhancer.runtime.DeterministicFakeModelSchedulerConfiguration;
import com.enhancer.tool.EvidenceStoragePolicy;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Bounded CLI input for the one supported model-aware Scheduler composition. */
record SchedulerModelExecutionCliConfiguration(
        Duration gatewayTimeout,
        int maximumResponseCharacters,
        long maximumReadBytes,
        Duration toolTimeout,
        Set<String> deniedTools) {
    static final String EXECUTION = "deterministic-fake-v2";
    static final int MAX_DENIED_TOOLS = 16;
    static final int MAX_DENIED_TOOL_CHARACTERS = 128;

    SchedulerModelExecutionCliConfiguration {
        Objects.requireNonNull(gatewayTimeout, "gatewayTimeout");
        Objects.requireNonNull(toolTimeout, "toolTimeout");
        Objects.requireNonNull(deniedTools, "deniedTools");
        if (gatewayTimeout.isZero()
                || gatewayTimeout.isNegative()
                || gatewayTimeout.compareTo(ModelRequest.MAX_TIMEOUT) > 0
                || gatewayTimeout.toNanosPart() % 1_000_000 != 0) {
            throw new IllegalArgumentException("gatewayTimeout is outside the supported bound");
        }
        if (maximumResponseCharacters < 1
                || maximumResponseCharacters > ModelRequest.MAX_RESPONSE_LENGTH) {
            throw new IllegalArgumentException(
                    "maximumResponseCharacters is outside the supported bound");
        }
        if (maximumReadBytes < 1
                || maximumReadBytes > EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES) {
            throw new IllegalArgumentException("maximumReadBytes is outside the supported bound");
        }
        if (toolTimeout.isZero()
                || toolTimeout.isNegative()
                || toolTimeout.toNanosPart() % 1_000_000 != 0
                || gatewayTimeout.compareTo(toolTimeout) >= 0) {
            throw new IllegalArgumentException(
                    "toolTimeout must be a whole millisecond greater than gatewayTimeout");
        }
        if (deniedTools.size() > MAX_DENIED_TOOLS) {
            throw new IllegalArgumentException(
                    "deniedTools exceeds the supported count bound");
        }
        TreeSet<String> canonicalDeniedTools = new TreeSet<>();
        for (String deniedTool : deniedTools) {
            Objects.requireNonNull(deniedTool, "deniedTool");
            if (deniedTool.isBlank()
                    || deniedTool.length() > MAX_DENIED_TOOL_CHARACTERS) {
                throw new IllegalArgumentException(
                        "deniedTool is blank or exceeds the supported length bound");
            }
            canonicalDeniedTools.add(deniedTool);
        }
        deniedTools = Collections.unmodifiableSet(canonicalDeniedTools);
    }

    DeterministicFakeModelSchedulerConfiguration toRuntimeConfiguration() {
        return new DeterministicFakeModelSchedulerConfiguration(
                gatewayTimeout,
                maximumResponseCharacters,
                maximumReadBytes,
                toolTimeout,
                deniedTools);
    }
}
