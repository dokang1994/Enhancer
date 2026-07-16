package com.enhancer.tool;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record ExecutionPolicy(
        Path projectRoot,
        Set<String> allowedTools,
        Set<String> deniedTools,
        long maxReadBytes,
        Duration timeout,
        CancellationToken cancellationToken) {

    public ExecutionPolicy {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(allowedTools, "allowedTools must not be null");
        Objects.requireNonNull(deniedTools, "deniedTools must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        Objects.requireNonNull(cancellationToken, "cancellationToken must not be null");

        if (maxReadBytes <= 0) {
            throw new IllegalArgumentException("maxReadBytes must be positive");
        }
        if (maxReadBytes > EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES) {
            throw new IllegalArgumentException("maxReadBytes exceeds the supported evidence limit");
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        try {
            if (timeout.toMillis() <= 0) {
                throw new IllegalArgumentException(
                        "timeout must be at least one millisecond");
            }
            timeout.toNanos();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "timeout exceeds the supported execution range",
                    exception);
        }

        projectRoot = projectRoot.toAbsolutePath().normalize();
        allowedTools = validatedToolNames(allowedTools, "allowedTools");
        deniedTools = validatedToolNames(deniedTools, "deniedTools");
    }

    public boolean allows(String toolName) {
        return toolName != null
                && allowedTools.contains(toolName)
                && !deniedTools.contains(toolName);
    }

    private static Set<String> validatedToolNames(Set<String> toolNames, String fieldName) {
        Set<String> snapshot = new LinkedHashSet<>();
        for (String toolName : toolNames) {
            Objects.requireNonNull(toolName, fieldName + " must not contain null");
            if (toolName.isBlank()) {
                throw new IllegalArgumentException(fieldName + " must not contain blank names");
            }
            snapshot.add(toolName);
        }
        return Set.copyOf(snapshot);
    }
}
