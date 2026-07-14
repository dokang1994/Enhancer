package com.enhancer.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ToolExecutorTest {
    @TempDir
    Path projectRoot;

    @Test
    void executesAnAllowedToolAndPreservesItsResult() {
        Tool expectedTool = tool("fake", (request, policy) -> success("fake", "done"));

        try (ToolExecutor executor = new ToolExecutor(List.of(expectedTool))) {
            ToolResult result = executor.execute(
                    request("fake"),
                    policy(Set.of("fake"), Set.of(), CancellationToken.none(), Duration.ofSeconds(1)));

            assertEquals(ToolResultStatus.SUCCESS, result.status());
            assertEquals("done", result.evidence().outputTail());
        }
    }

    @Test
    void deniesBeforeInvocationAndRejectsUnknownTools() {
        AtomicInteger invocations = new AtomicInteger();
        Tool deniedTool = tool("fake", (request, policy) -> {
            invocations.incrementAndGet();
            return success("fake", "unexpected");
        });

        try (ToolExecutor executor = new ToolExecutor(List.of(deniedTool))) {
            ToolResult denied = executor.execute(
                    request("fake"),
                    policy(
                            Set.of("fake"),
                            Set.of("fake"),
                            CancellationToken.none(),
                            Duration.ofSeconds(1)));
            ToolResult unknown = executor.execute(
                    request("unknown"),
                    policy(
                            Set.of("unknown"),
                            Set.of(),
                            CancellationToken.none(),
                            Duration.ofSeconds(1)));

            assertEquals(ToolResultStatus.FAILURE, denied.status());
            assertEquals(ToolFailureCode.POLICY_DENIED, denied.failureCode().orElseThrow());
            assertTrue(denied.evidence().summary().contains("denied"));
            assertEquals(ToolResultStatus.FAILURE, unknown.status());
            assertEquals(ToolFailureCode.TOOL_NOT_REGISTERED, unknown.failureCode().orElseThrow());
            assertTrue(unknown.evidence().summary().contains("registered"));
            assertEquals(0, invocations.get());
        }
    }

    @Test
    void reportsCancellationBeforeAndAfterInvocation() {
        AtomicInteger invocations = new AtomicInteger();
        Tool tool = tool("fake", (request, policy) -> {
            invocations.incrementAndGet();
            return success("fake", "done");
        });

        try (ToolExecutor executor = new ToolExecutor(List.of(tool))) {
            ToolResult before = executor.execute(
                    request("fake"),
                    policy(Set.of("fake"), Set.of(), () -> true, Duration.ofSeconds(1)));
            ToolResult after = executor.execute(
                    request("fake"),
                    policy(
                            Set.of("fake"),
                            Set.of(),
                            () -> invocations.get() > 0,
                            Duration.ofSeconds(1)));

            assertEquals(ToolResultStatus.FAILURE, before.status());
            assertEquals(ToolResultStatus.FAILURE, after.status());
            assertEquals(ToolFailureCode.CANCELLED, before.failureCode().orElseThrow());
            assertEquals(ToolFailureCode.CANCELLED, after.failureCode().orElseThrow());
            assertTrue(before.evidence().summary().contains("cancelled"));
            assertTrue(after.evidence().summary().contains("cancelled"));
            assertEquals(1, invocations.get());
        }
    }

    @Test
    void convertsTimeoutAndToolExceptionsToBoundedFailures() {
        Tool slow = tool("slow", (request, policy) -> {
            Thread.sleep(500);
            return success("slow", "late");
        });
        Tool broken = tool("broken", (request, policy) -> {
            throw new IllegalStateException("boom");
        });

        try (ToolExecutor executor = new ToolExecutor(List.of(slow, broken))) {
            ToolResult timeout = executor.execute(
                    request("slow"),
                    policy(Set.of("slow"), Set.of(), CancellationToken.none(), Duration.ofMillis(20)));
            ToolResult failure = executor.execute(
                    request("broken"),
                    policy(
                            Set.of("broken"),
                            Set.of(),
                            CancellationToken.none(),
                            Duration.ofSeconds(1)));

            assertEquals(ToolResultStatus.FAILURE, timeout.status());
            assertEquals(ToolFailureCode.TIMED_OUT, timeout.failureCode().orElseThrow());
            assertTrue(timeout.evidence().summary().contains("timed out"));
            assertEquals(ToolResultStatus.FAILURE, failure.status());
            assertEquals(ToolFailureCode.EXECUTION_FAILED, failure.failureCode().orElseThrow());
            assertTrue(failure.evidence().outputTail().contains("boom"));
            assertFalse(failure.evidence().truncated());
        }
    }

    @Test
    void rejectsDuplicateRegistrationAndContradictoryToolResults() {
        Tool first = tool("duplicate", (request, policy) -> success("duplicate", "first"));
        Tool second = tool("duplicate", (request, policy) -> success("duplicate", "second"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ToolExecutor(List.of(first, second)));

        Tool wrongIdentity = tool("expected", (request, policy) -> success("other", "wrong"));
        Tool nullResult = tool("null-result", (request, policy) -> null);
        try (ToolExecutor executor = new ToolExecutor(List.of(wrongIdentity, nullResult))) {
            ToolResult wrong = executor.execute(
                    request("expected"),
                    policy(
                            Set.of("expected"),
                            Set.of(),
                            CancellationToken.none(),
                            Duration.ofSeconds(1)));
            ToolResult missing = executor.execute(
                    request("null-result"),
                    policy(
                            Set.of("null-result"),
                            Set.of(),
                            CancellationToken.none(),
                            Duration.ofSeconds(1)));

            assertEquals(ToolResultStatus.FAILURE, wrong.status());
            assertTrue(wrong.evidence().summary().contains("invalid result"));
            assertEquals(ToolResultStatus.FAILURE, missing.status());
            assertTrue(missing.evidence().summary().contains("invalid result"));
        }
    }

    private ToolRequest request(String toolName) {
        return new ToolRequest(toolName, "run-1", Map.of());
    }

    private ExecutionPolicy policy(
            Set<String> allowed,
            Set<String> denied,
            CancellationToken cancellationToken,
            Duration timeout) {
        return new ExecutionPolicy(
                projectRoot,
                allowed,
                denied,
                1024,
                timeout,
                cancellationToken);
    }

    private ToolResult success(String toolName, String output) {
        return new ToolResult(
                toolName,
                ToolResultStatus.SUCCESS,
                OptionalInt.empty(),
                VerificationEvidence.capture("Tool succeeded", output, Optional.empty()));
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
