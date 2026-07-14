package com.enhancer.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class ToolResultTest {
    private final VerificationEvidence evidence = VerificationEvidence.capture(
            "Tool completed",
            "structured output",
            Optional.empty());

    @Test
    void representsSuccessfulProcessAndNonProcessTools() {
        ToolResult processResult = new ToolResult(
                "terminal",
                ToolResultStatus.SUCCESS,
                OptionalInt.of(0),
                evidence);
        ToolResult nonProcessResult = new ToolResult(
                "read-file",
                ToolResultStatus.SUCCESS,
                OptionalInt.empty(),
                evidence);

        assertEquals(0, processResult.exitCode().orElseThrow());
        assertTrue(nonProcessResult.exitCode().isEmpty());
        assertEquals(evidence, processResult.evidence());
    }

    @Test
    void representsAFailedProcessTool() {
        ToolResult result = new ToolResult(
                "terminal",
                ToolResultStatus.FAILURE,
                OptionalInt.of(1),
                evidence);

        assertEquals(ToolResultStatus.FAILURE, result.status());
        assertEquals(1, result.exitCode().orElseThrow());
    }

    @Test
    void rejectsStatusAndExitCodeContradictions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ToolResult(
                        "terminal",
                        ToolResultStatus.SUCCESS,
                        OptionalInt.of(1),
                        evidence));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ToolResult(
                        "terminal",
                        ToolResultStatus.FAILURE,
                        OptionalInt.of(0),
                        evidence));
    }

    @Test
    void rejectsMissingIdentityOrEvidence() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ToolResult(
                        " ",
                        ToolResultStatus.SUCCESS,
                        OptionalInt.empty(),
                        evidence));
        assertThrows(
                NullPointerException.class,
                () -> new ToolResult(
                        "read-file",
                        ToolResultStatus.SUCCESS,
                        OptionalInt.empty(),
                        null));
    }
}
