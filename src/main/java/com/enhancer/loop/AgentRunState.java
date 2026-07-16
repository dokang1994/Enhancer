package com.enhancer.loop;

import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.kernel.VerificationStatus;
import java.util.Objects;
import java.util.Optional;

public final class AgentRunState implements AgentLoopSnapshot {
    private final ApprovedTask approvedTask;
    private final ToolRequest executedRequest;
    private final Optional<ToolRequest> pendingRequest;
    private final Optional<ToolResult> lastResult;
    private final AgentLoopStatus status;
    private final String progressKey;

    private AgentRunState(
            ApprovedTask approvedTask,
            ToolRequest executedRequest,
            Optional<ToolRequest> pendingRequest,
            Optional<ToolResult> lastResult,
            AgentLoopStatus status,
            String progressKey) {
        this.approvedTask = Objects.requireNonNull(
                approvedTask,
                "approvedTask must not be null");
        this.executedRequest = Objects.requireNonNull(
                executedRequest,
                "executedRequest must not be null");
        this.pendingRequest = Objects.requireNonNull(
                pendingRequest,
                "pendingRequest must not be null");
        this.lastResult = Objects.requireNonNull(lastResult, "lastResult must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.progressKey = Objects.requireNonNull(progressKey, "progressKey must not be null");
        if (progressKey.isBlank()) {
            throw new IllegalArgumentException("progressKey must not be blank");
        }
        validateState();
    }

    public static AgentRunState ready(ApprovedTask approvedTask, ToolRequest request) {
        Objects.requireNonNull(approvedTask, "approvedTask must not be null");
        Objects.requireNonNull(request, "request must not be null");
        if (!approvedTask.allows(request.toolName())) {
            throw new IllegalArgumentException(
                    "Tool request is outside the approved Tool scope: " + request.toolName());
        }
        return new AgentRunState(
                approvedTask,
                request,
                Optional.of(request),
                Optional.empty(),
                AgentLoopStatus.RUNNING,
                AgentRunProgressKey.pending(approvedTask, request));
    }

    static AgentRunState awaitingVerification(
            AgentRunState current,
            ToolRequest request,
            ToolResult result) {
        return transition(
                current,
                request,
                result,
                Optional.empty(),
                AgentLoopStatus.AWAITING_VERIFICATION);
    }

    static AgentRunState retrying(
            AgentRunState current,
            ToolRequest request,
            ToolResult result) {
        return transition(
                current,
                request,
                result,
                Optional.of(request),
                AgentLoopStatus.RUNNING);
    }

    static AgentRunState failed(
            AgentRunState current,
            ToolRequest request,
            ToolResult result) {
        return transition(
                current,
                request,
                result,
                Optional.empty(),
                AgentLoopStatus.FAILED);
    }

    static AgentRunState completedAfterVerification(
            AgentRunState current,
            VerificationDecision decision) {
        Objects.requireNonNull(current, "current must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
        if (current.status != AgentLoopStatus.AWAITING_VERIFICATION) {
            throw new IllegalArgumentException(
                    "completion requires AWAITING_VERIFICATION state");
        }
        if (decision.status() != VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException("completion requires a Verified decision");
        }
        return new AgentRunState(
                current.approvedTask,
                current.executedRequest,
                Optional.empty(),
                current.lastResult,
                AgentLoopStatus.COMPLETED,
                current.progressKey + "|verification=" + decision.code());
    }

    public ApprovedTask approvedTask() {
        return approvedTask;
    }

    public ToolRequest executedRequest() {
        return executedRequest;
    }

    public Optional<ToolRequest> pendingRequest() {
        return pendingRequest;
    }

    public Optional<ToolResult> lastResult() {
        return lastResult;
    }

    @Override
    public AgentLoopStatus status() {
        return status;
    }

    @Override
    public String progressKey() {
        return progressKey;
    }

    private static AgentRunState transition(
            AgentRunState current,
            ToolRequest request,
            ToolResult result,
            Optional<ToolRequest> pendingRequest,
            AgentLoopStatus status) {
        Objects.requireNonNull(current, "current must not be null");
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(result, "result must not be null");
        if (!current.executedRequest.equals(request)) {
            throw new IllegalArgumentException(
                    "transition request must match the preserved executed request");
        }
        return new AgentRunState(
                current.approvedTask,
                request,
                pendingRequest,
                Optional.of(result),
                status,
                AgentRunProgressKey.result(current.approvedTask, request, result));
    }

    private void validateState() {
        switch (status) {
            case RUNNING -> {
                if (pendingRequest.isEmpty()) {
                    throw new IllegalArgumentException("RUNNING requires a pending request");
                }
                if (lastResult.isPresent()
                        && lastResult.orElseThrow().status() != ToolResultStatus.FAILURE) {
                    throw new IllegalArgumentException(
                            "RUNNING can retain only a retryable failure result");
                }
            }
            case AWAITING_VERIFICATION -> requireTerminalResult(
                    ToolResultStatus.SUCCESS,
                    "AWAITING_VERIFICATION");
            case FAILED -> requireTerminalResult(ToolResultStatus.FAILURE, "FAILED");
            case COMPLETED -> requireTerminalResult(ToolResultStatus.SUCCESS, "COMPLETED");
        }
        if (pendingRequest.isPresent() && !pendingRequest.orElseThrow().equals(executedRequest)) {
            throw new IllegalArgumentException(
                    "pending request must match the preserved executed request");
        }
    }

    private void requireTerminalResult(ToolResultStatus expectedStatus, String stateName) {
        if (pendingRequest.isPresent()) {
            throw new IllegalArgumentException(stateName + " cannot retain a pending request");
        }
        if (lastResult.isEmpty() || lastResult.orElseThrow().status() != expectedStatus) {
            throw new IllegalArgumentException(
                    stateName + " requires a " + expectedStatus + " Tool result");
        }
    }
}
