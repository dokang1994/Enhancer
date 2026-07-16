package com.enhancer.run;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.kernel.VerificationCode;
import com.enhancer.kernel.VerificationDecision;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RunRecordTest {
    private static final String EXPECTED_DIGEST = "a".repeat(64);

    @Test
    void rejectsWorkerCompletionBeforeIndependentVerification() {
        assertThrows(
                IllegalArgumentException.class,
                () -> record(
                        success(),
                        Optional.empty(),
                        VerificationDecision.notPerformed("worker did not wait"),
                        AgentLoopStopReason.COMPLETED,
                        AgentLoopStopReason.COMPLETED));
    }

    @Test
    void rejectsPerformedVerificationForFailedWorker() {
        assertThrows(
                IllegalArgumentException.class,
                () -> record(
                        failure(),
                        Optional.of(EXPECTED_DIGEST),
                        VerificationDecision.rejected(
                                VerificationCode.TOOL_RESULT_FAILURE,
                                "Tool failed"),
                        AgentLoopStopReason.FAILED,
                        AgentLoopStopReason.FAILED));
    }

    @Test
    void rejectsSuccessfulToolResultForBoundedNonVerificationStop() {
        assertThrows(
                IllegalArgumentException.class,
                () -> record(
                        success(),
                        Optional.empty(),
                        VerificationDecision.notPerformed("iteration limit reached"),
                        AgentLoopStopReason.MAX_ITERATIONS,
                        AgentLoopStopReason.MAX_ITERATIONS));
    }

    @Test
    void rejectsFailedToolResultAtVerificationBoundary() {
        assertThrows(
                IllegalArgumentException.class,
                () -> record(
                        failure(),
                        Optional.of(EXPECTED_DIGEST),
                        VerificationDecision.rejected(
                                VerificationCode.TOOL_RESULT_FAILURE,
                                "Tool failed"),
                        AgentLoopStopReason.AWAITING_VERIFICATION,
                        AgentLoopStopReason.AWAITING_VERIFICATION));
    }

    @Test
    void acceptsGovernedVerificationAndFailureLifecycles() {
        assertDoesNotThrow(() -> record(
                success(),
                Optional.of(EXPECTED_DIGEST),
                VerificationDecision.verified("content matched"),
                AgentLoopStopReason.AWAITING_VERIFICATION,
                AgentLoopStopReason.COMPLETED));
        assertDoesNotThrow(() -> record(
                success(),
                Optional.of(EXPECTED_DIGEST),
                VerificationDecision.rejected(
                        VerificationCode.CONTENT_MISMATCH,
                        "content differed"),
                AgentLoopStopReason.AWAITING_VERIFICATION,
                AgentLoopStopReason.AWAITING_VERIFICATION));
        assertDoesNotThrow(() -> record(
                failure(),
                Optional.empty(),
                VerificationDecision.notPerformed("worker failed"),
                AgentLoopStopReason.FAILED,
                AgentLoopStopReason.FAILED));
    }

    private RunRecord record(
            ToolResult result,
            Optional<String> expectedDigest,
            VerificationDecision verification,
            AgentLoopStopReason workerStop,
            AgentLoopStopReason finalStop) {
        return new RunRecord(
                "logical-run",
                Instant.parse("2026-07-14T00:00:00Z"),
                new ApprovedTask(
                        "run-record-test",
                        "Validate RunRecord lifecycle",
                        "Approved by test owner",
                        Set.of("read-file"),
                        "CURRENT_TASK.md"),
                new ToolRequest(
                        "read-file",
                        "correlation-1",
                        Map.of("path", "target.txt")),
                new PolicyDecision(
                        PolicyDecisionStatus.ALLOWED,
                        "C:/project",
                        Set.of("read-file"),
                        Set.of(),
                        4096,
                        1000),
                result,
                expectedDigest,
                verification,
                1,
                workerStop,
                finalStop);
    }

    private ToolResult success() {
        return new ToolResult(
                "read-file",
                ToolResultStatus.SUCCESS,
                OptionalInt.empty(),
                VerificationEvidence.capture(
                        "read succeeded",
                        "content",
                        Optional.empty()));
    }

    private ToolResult failure() {
        return new ToolResult(
                "read-file",
                ToolResultStatus.FAILURE,
                OptionalInt.empty(),
                Optional.of(ToolFailureCode.INVALID_REQUEST),
                VerificationEvidence.capture(
                        "read failed",
                        "invalid request",
                        Optional.empty()));
    }
}
