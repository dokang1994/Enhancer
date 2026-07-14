package com.enhancer.loop;

import com.enhancer.tool.ExecutionPolicy;
import com.enhancer.tool.ToolExecutor;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import java.util.Objects;

public final class AgentRunController {
    private final ToolExecutor toolExecutor;
    private final ExecutionPolicy executionPolicy;
    private final ToolFailureClassifier failureClassifier;

    public AgentRunController(
            ToolExecutor toolExecutor,
            ExecutionPolicy executionPolicy,
            ToolFailureClassifier failureClassifier) {
        this.toolExecutor = Objects.requireNonNull(
                toolExecutor,
                "toolExecutor must not be null");
        this.executionPolicy = Objects.requireNonNull(
                executionPolicy,
                "executionPolicy must not be null");
        this.failureClassifier = Objects.requireNonNull(
                failureClassifier,
                "failureClassifier must not be null");
    }

    public AgentRunResult run(AgentRunState initialState, AgentLoop loop) {
        Objects.requireNonNull(initialState, "initialState must not be null");
        Objects.requireNonNull(loop, "loop must not be null");
        if (initialState.status() != AgentLoopStatus.RUNNING) {
            throw new IllegalArgumentException("Agent run must start in RUNNING state");
        }

        LoopExecution<AgentRunState> execution = loop.runSnapshots(initialState, this::executeStep);
        return new AgentRunResult(
                execution.state(),
                execution.stopReason(),
                execution.iterations());
    }

    private AgentRunState executeStep(AgentRunState current) {
        ToolRequest request = current.pendingRequest().orElseThrow(
                () -> new IllegalStateException("RUNNING state has no pending request"));
        ToolResult result = toolExecutor.execute(request, executionPolicy);

        if (result.status() == ToolResultStatus.SUCCESS) {
            return AgentRunState.awaitingVerification(current, request, result);
        }

        ToolFailureDisposition disposition = Objects.requireNonNull(
                failureClassifier.classify(request, result),
                "failure classifier result must not be null");
        return disposition == ToolFailureDisposition.RETRYABLE
                ? AgentRunState.retrying(current, request, result)
                : AgentRunState.failed(current, request, result);
    }
}
