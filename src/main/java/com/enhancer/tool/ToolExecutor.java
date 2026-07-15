package com.enhancer.tool;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ToolExecutor implements AutoCloseable {
    private final Map<String, Tool> tools;
    private final Set<ExecutorService> workers = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicInteger workerSequence = new AtomicInteger();
    private final Object lifecycleLock = new Object();

    public ToolExecutor(Collection<? extends Tool> tools) {
        Objects.requireNonNull(tools, "tools must not be null");

        Map<String, Tool> registry = new LinkedHashMap<>();
        for (Tool tool : tools) {
            Objects.requireNonNull(tool, "tools must not contain null");
            String name = Objects.requireNonNull(tool.name(), "tool name must not be null");
            if (name.isBlank()) {
                throw new IllegalArgumentException("tool name must not be blank");
            }
            if (registry.putIfAbsent(name, tool) != null) {
                throw new IllegalArgumentException("duplicate Tool name: " + name);
            }
        }
        this.tools = Map.copyOf(registry);
    }

    public ToolResult execute(ToolRequest request, ExecutionPolicy policy) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(policy, "policy must not be null");

        if (!policy.allows(request.toolName())) {
            return failure(
                    request.toolName(),
                    ToolFailureCode.POLICY_DENIED,
                    "Tool execution denied by policy",
                    "Tool is not allowed: " + request.toolName());
        }

        Tool tool = tools.get(request.toolName());
        if (tool == null) {
            return failure(
                    request.toolName(),
                    ToolFailureCode.TOOL_NOT_REGISTERED,
                    "Tool is not registered",
                    "No registered Tool matches: " + request.toolName());
        }

        if (policy.cancellationToken().isCancellationRequested()) {
            return failure(
                    request.toolName(),
                    ToolFailureCode.CANCELLED,
                    "Tool execution cancelled",
                    "Cancellation was requested before Tool invocation");
        }

        ExecutorService worker = createWorker();
        Future<ToolResult> future = worker.submit(() -> tool.execute(request, policy));
        try {
            ToolResult result = future.get(policy.timeout().toNanos(), TimeUnit.NANOSECONDS);
            if (policy.cancellationToken().isCancellationRequested()) {
                return failure(
                        request.toolName(),
                        ToolFailureCode.CANCELLED,
                        "Tool execution cancelled",
                        "Cancellation was requested after Tool invocation");
            }

            if (result == null || !request.toolName().equals(result.toolName())) {
                return failure(
                        request.toolName(),
                        ToolFailureCode.INVALID_RESULT,
                        "Tool returned an invalid result",
                        result == null
                                ? "Tool returned null"
                                : "Result Tool name does not match request: " + result.toolName());
            }
            return result;
        } catch (TimeoutException exception) {
            future.cancel(true);
            return failure(
                    request.toolName(),
                    ToolFailureCode.TIMED_OUT,
                    "Tool execution timed out",
                    "Tool exceeded timeout " + policy.timeout());
        } catch (InterruptedException exception) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            return failure(
                    request.toolName(),
                    ToolFailureCode.INTERRUPTED,
                    "Tool execution interrupted",
                    "The executing thread was interrupted");
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            return failure(
                    request.toolName(),
                    failureCode(cause),
                    "Tool execution failed",
                    diagnostic(cause));
        } finally {
            worker.shutdownNow();
            if (worker.isTerminated()) {
                workers.remove(worker);
            }
        }
    }

    @Override
    public void close() {
        synchronized (lifecycleLock) {
            if (closed.compareAndSet(false, true)) {
                workers.forEach(ExecutorService::shutdownNow);
            }
        }
    }

    private ExecutorService createWorker() {
        synchronized (lifecycleLock) {
            if (closed.get()) {
                throw new IllegalStateException("ToolExecutor is closed");
            }
            workers.removeIf(ExecutorService::isTerminated);
            ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "enhancer-tool-executor-" + workerSequence.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            });
            workers.add(worker);
            return worker;
        }
    }

    private ToolResult failure(
            String toolName,
            ToolFailureCode failureCode,
            String summary,
            String diagnostic) {
        String boundedDiagnostic = Objects.requireNonNullElse(diagnostic, "");
        int limit = VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS;
        if (boundedDiagnostic.length() > limit) {
            boundedDiagnostic = boundedDiagnostic.substring(boundedDiagnostic.length() - limit);
        }
        return new ToolResult(
                toolName,
                ToolResultStatus.FAILURE,
                OptionalInt.empty(),
                Optional.of(failureCode),
                VerificationEvidence.capture(summary, boundedDiagnostic, Optional.empty()));
    }

    private ToolFailureCode failureCode(Throwable throwable) {
        return throwable instanceof IllegalArgumentException
                ? ToolFailureCode.INVALID_REQUEST
                : ToolFailureCode.EXECUTION_FAILED;
    }

    private String diagnostic(Throwable throwable) {
        String message = throwable.getMessage();
        return throwable.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }
}
