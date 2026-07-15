package com.enhancer.run;

import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.verification.VerificationDecision;
import com.enhancer.verification.VerificationStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public record RunRecord(
        String logicalRunId,
        Instant recordedAt,
        ApprovedTask approvedTask,
        ToolRequest toolRequest,
        PolicyDecision policyDecision,
        ToolResult toolResult,
        Optional<String> expectedContentSha256,
        VerificationDecision verification,
        int iterations,
        AgentLoopStopReason workerStopReason,
        AgentLoopStopReason finalStopReason) {

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public RunRecord {
        Objects.requireNonNull(logicalRunId, "logicalRunId must not be null");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        Objects.requireNonNull(approvedTask, "approvedTask must not be null");
        Objects.requireNonNull(toolRequest, "toolRequest must not be null");
        Objects.requireNonNull(policyDecision, "policyDecision must not be null");
        Objects.requireNonNull(toolResult, "toolResult must not be null");
        Objects.requireNonNull(
                expectedContentSha256,
                "expectedContentSha256 must not be null");
        Objects.requireNonNull(verification, "verification must not be null");
        Objects.requireNonNull(workerStopReason, "workerStopReason must not be null");
        Objects.requireNonNull(finalStopReason, "finalStopReason must not be null");
        if (logicalRunId.isBlank()) {
            throw new IllegalArgumentException("logicalRunId must not be blank");
        }
        if (iterations <= 0) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        if (!approvedTask.allows(toolRequest.toolName())) {
            throw new IllegalArgumentException("Tool request is outside approved task scope");
        }
        if (!toolRequest.toolName().equals(toolResult.toolName())) {
            throw new IllegalArgumentException("Tool request and result names must match");
        }
        if (expectedContentSha256.isPresent()
                && !SHA_256.matcher(expectedContentSha256.orElseThrow()).matches()) {
            throw new IllegalArgumentException(
                    "expectedContentSha256 must be 64 lowercase hexadecimal characters");
        }
        if (toolResult.status() == ToolResultStatus.SUCCESS
                && policyDecision.status() != PolicyDecisionStatus.ALLOWED) {
            throw new IllegalArgumentException(
                    "successful Tool result requires an allowed policy decision");
        }
        if (policyDecision.status() != PolicyDecisionStatus.ALLOWED
                && (toolResult.failureCode().isEmpty()
                        || toolResult.failureCode().orElseThrow()
                                != ToolFailureCode.POLICY_DENIED)) {
            throw new IllegalArgumentException(
                    "denied policy decision requires a policy-denied Tool result");
        }

        validateLifecycle(
                toolResult,
                expectedContentSha256,
                verification,
                workerStopReason,
                finalStopReason);
    }

    private static void validateLifecycle(
            ToolResult toolResult,
            Optional<String> expectedContentSha256,
            VerificationDecision verification,
            AgentLoopStopReason workerStopReason,
            AgentLoopStopReason finalStopReason) {
        if (workerStopReason == AgentLoopStopReason.COMPLETED) {
            throw new IllegalArgumentException(
                    "worker cannot complete before independent verification");
        }

        if (workerStopReason == AgentLoopStopReason.AWAITING_VERIFICATION) {
            if (toolResult.status() != ToolResultStatus.SUCCESS) {
                throw new IllegalArgumentException(
                        "verification-waiting RunRecord requires a successful Tool result");
            }
            if (verification.status() == VerificationStatus.NOT_PERFORMED) {
                throw new IllegalArgumentException(
                        "verification-waiting run requires an actual verification decision");
            }
            if (expectedContentSha256.isEmpty()) {
                throw new IllegalArgumentException(
                        "performed verification requires the expected content digest");
            }
            AgentLoopStopReason expectedFinal =
                    verification.status() == VerificationStatus.VERIFIED
                            ? AgentLoopStopReason.COMPLETED
                            : AgentLoopStopReason.AWAITING_VERIFICATION;
            if (finalStopReason != expectedFinal) {
                throw new IllegalArgumentException(
                        "verification decision contradicts the final stop reason");
            }
            return;
        }

        if (toolResult.status() != ToolResultStatus.FAILURE) {
            throw new IllegalArgumentException(
                    "failed or bounded worker stop requires a failed Tool result");
        }
        if (verification.status() != VerificationStatus.NOT_PERFORMED) {
            throw new IllegalArgumentException(
                    "verification can be performed only after verification wait");
        }
        if (expectedContentSha256.isPresent()) {
            throw new IllegalArgumentException(
                    "Not Performed verification cannot carry an expected digest");
        }
        if (finalStopReason != workerStopReason) {
            throw new IllegalArgumentException(
                    "non-verification RunRecord cannot change the worker stop reason");
        }
    }
}
