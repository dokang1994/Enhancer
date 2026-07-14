package com.enhancer.tool;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

public record ToolResult(
        String toolName,
        ToolResultStatus status,
        OptionalInt exitCode,
        Optional<ToolFailureCode> failureCode,
        VerificationEvidence evidence) {

    public ToolResult {
        Objects.requireNonNull(toolName, "toolName must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(exitCode, "exitCode must not be null");
        Objects.requireNonNull(failureCode, "failureCode must not be null");
        Objects.requireNonNull(evidence, "evidence must not be null");

        if (toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must not be blank");
        }
        if (status == ToolResultStatus.SUCCESS
                && exitCode.isPresent()
                && exitCode.orElseThrow() != 0) {
            throw new IllegalArgumentException("successful Tool result requires exit code zero");
        }
        if (status == ToolResultStatus.FAILURE
                && exitCode.isPresent()
                && exitCode.orElseThrow() == 0) {
            throw new IllegalArgumentException("failed Tool result cannot have exit code zero");
        }
        if (status == ToolResultStatus.SUCCESS && failureCode.isPresent()) {
            throw new IllegalArgumentException("successful Tool result cannot have a failure code");
        }
        if (status == ToolResultStatus.FAILURE && failureCode.isEmpty()) {
            throw new IllegalArgumentException("failed Tool result requires a failure code");
        }
    }

    public ToolResult(
            String toolName,
            ToolResultStatus status,
            OptionalInt exitCode,
            VerificationEvidence evidence) {
        this(
                toolName,
                status,
                exitCode,
                status == ToolResultStatus.FAILURE
                        ? Optional.of(ToolFailureCode.TOOL_REPORTED_FAILURE)
                        : Optional.empty(),
                evidence);
    }
}
