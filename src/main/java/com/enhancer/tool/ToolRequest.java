package com.enhancer.tool;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ToolRequest(
        String toolName,
        String correlationId,
        Map<String, String> arguments) {

    public ToolRequest {
        Objects.requireNonNull(toolName, "toolName must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(arguments, "arguments must not be null");

        if (toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must not be blank");
        }
        if (correlationId.isBlank()) {
            throw new IllegalArgumentException("correlationId must not be blank");
        }

        Map<String, String> snapshot = new LinkedHashMap<>();
        arguments.forEach((key, value) -> {
            Objects.requireNonNull(key, "argument key must not be null");
            Objects.requireNonNull(value, "argument value must not be null");
            if (key.isBlank()) {
                throw new IllegalArgumentException("argument key must not be blank");
            }
            snapshot.put(key, value);
        });
        arguments = Map.copyOf(snapshot);
    }
}
