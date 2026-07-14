package com.enhancer.loop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.tool.CancellationToken;
import com.enhancer.tool.ExecutionPolicy;
import com.enhancer.tool.Tool;
import com.enhancer.tool.ToolExecutor;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentRunControllerTest {
    @TempDir
    Path projectRoot;

    @Test
    void successfulToolExecutionStopsAwaitingIndependentVerification() {
        Tool tool = tool("inspect", (request, policy) -> success("inspect", "observed"));

        try (ToolExecutor executor = new ToolExecutor(List.of(tool))) {
            AgentRunController controller = new AgentRunController(
                    executor,
                    policy(Set.of("inspect"), Set.of()),
                    ToolFailureClassifier.terminalByDefault());

            AgentRunResult result = controller.run(
                    AgentRunState.ready(approved("inspect"), request("inspect")),
                    new AgentLoop(5, 3));

            assertEquals(AgentLoopStopReason.AWAITING_VERIFICATION, result.stopReason());
            assertEquals(AgentLoopStatus.AWAITING_VERIFICATION, result.state().status());
            assertEquals(ToolResultStatus.SUCCESS, result.state().lastResult().orElseThrow().status());
            assertTrue(result.state().pendingRequest().isEmpty());
            assertEquals(1, result.iterations());
        }
    }

    @Test
    void terminalToolFailureCannotBeReportedAsCompletion() {
        Tool tool = tool("inspect", (request, policy) -> failure("inspect", "invalid request"));

        try (ToolExecutor executor = new ToolExecutor(List.of(tool))) {
            AgentRunController controller = new AgentRunController(
                    executor,
                    policy(Set.of("inspect"), Set.of()),
                    ToolFailureClassifier.terminalByDefault());

            AgentRunResult result = controller.run(
                    AgentRunState.ready(approved("inspect"), request("inspect")),
                    new AgentLoop(5, 3));

            assertEquals(AgentLoopStopReason.FAILED, result.stopReason());
            assertEquals(AgentLoopStatus.FAILED, result.state().status());
            assertFalse(result.stopReason() == AgentLoopStopReason.COMPLETED);
            assertEquals(ToolResultStatus.FAILURE, result.state().lastResult().orElseThrow().status());
            assertTrue(result.state().pendingRequest().isEmpty());
        }
    }

    @Test
    void repeatedIdenticalRetryableResultsStopAsStagnated() {
        AtomicInteger invocations = new AtomicInteger();
        Tool tool = tool("flaky", (request, policy) -> {
            int invocation = invocations.incrementAndGet();
            String output = "same failure".repeat(500);
            return new ToolResult(
                    "flaky",
                    ToolResultStatus.FAILURE,
                    OptionalInt.empty(),
                    Optional.of(ToolFailureCode.TEMPORARY_FAILURE),
                    VerificationEvidence.capture(
                            "attempt " + invocation,
                            output,
                            Optional.of("evidence/run/" + invocation)));
        });

        try (ToolExecutor executor = new ToolExecutor(List.of(tool))) {
            AgentRunController controller = new AgentRunController(
                    executor,
                    policy(Set.of("flaky"), Set.of()),
                    ToolFailureClassifier.standard());

            AgentRunResult result = controller.run(
                    AgentRunState.ready(approved("flaky"), request("flaky")),
                    new AgentLoop(10, 3));

            assertEquals(AgentLoopStopReason.STAGNATED, result.stopReason());
            assertEquals(AgentLoopStatus.RUNNING, result.state().status());
            assertEquals(4, result.iterations());
            assertEquals(4, invocations.get());
            assertTrue(result.state().pendingRequest().isPresent());
        }
    }

    @Test
    void deniedMutationToolCannotInvokeOrAuthorizeItself() throws Exception {
        Path gitHead = Files.createDirectories(projectRoot.resolve(".git")).resolve("HEAD");
        Files.writeString(gitHead, "original");
        AtomicInteger invocations = new AtomicInteger();
        Tool mutatingTool = tool("git-write", (request, policy) -> {
            invocations.incrementAndGet();
            Files.writeString(gitHead, "mutated");
            return success("git-write", "changed");
        });

        try (ToolExecutor executor = new ToolExecutor(List.of(mutatingTool))) {
            AgentRunController controller = new AgentRunController(
                    executor,
                    policy(Set.of("git-write"), Set.of("git-write")),
                    ToolFailureClassifier.terminalByDefault());

            AgentRunResult result = controller.run(
                    AgentRunState.ready(approved("git-write"), request("git-write")),
                    new AgentLoop(5, 3));

            assertEquals(AgentLoopStopReason.FAILED, result.stopReason());
            assertEquals(0, invocations.get());
            assertEquals("original", Files.readString(gitHead));
            assertTrue(result.state().lastResult().orElseThrow().evidence().summary().contains("denied"));
        }
    }

    @Test
    void runStateConstructionIsRestrictedToGovernedFactories() {
        assertTrue(java.util.Arrays.stream(AgentRunState.class.getDeclaredConstructors())
                .noneMatch(constructor -> Modifier.isPublic(constructor.getModifiers())));
    }

    private ExecutionPolicy policy(Set<String> allowed, Set<String> denied) {
        return new ExecutionPolicy(
                projectRoot,
                allowed,
                denied,
                1024,
                Duration.ofSeconds(1),
                CancellationToken.none());
    }

    private ToolRequest request(String toolName) {
        return new ToolRequest(toolName, "run-1", Map.of());
    }

    private ApprovedTask approved(String toolName) {
        return new ApprovedTask(
                "task-1",
                "Approved test task",
                "Approved by test owner",
                Set.of(toolName),
                "CURRENT_TASK.md");
    }

    private ToolResult success(String toolName, String output) {
        return new ToolResult(
                toolName,
                ToolResultStatus.SUCCESS,
                OptionalInt.empty(),
                VerificationEvidence.capture("Tool succeeded", output, Optional.empty()));
    }

    private ToolResult failure(String toolName, String output) {
        return new ToolResult(
                toolName,
                ToolResultStatus.FAILURE,
                OptionalInt.empty(),
                Optional.of(ToolFailureCode.INVALID_REQUEST),
                VerificationEvidence.capture("Tool failed", output, Optional.empty()));
    }

    private Tool tool(String name, ToolBehavior behavior) {
        return new Tool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public ToolResult execute(ToolRequest request, ExecutionPolicy policy) throws Exception {
                return behavior.execute(request, policy);
            }
        };
    }

    @FunctionalInterface
    private interface ToolBehavior {
        ToolResult execute(ToolRequest request, ExecutionPolicy policy) throws Exception;
    }
}
