package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliSchedulerModelSelectionIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000002501";

    @TempDir
    Path temporaryRoot;

    @ParameterizedTest
    @ValueSource(strings = {
            "scheduler-cycle", "scheduler-drain", "scheduler-service"
    })
    void modelSelectionRetainsTheExactLegacyIdleOutput(String command)
            throws Exception {
        Layout legacy = layout(command + "-legacy");
        Layout model = layout(command + "-model");
        createEmptyQueue(legacy);
        createEmptyQueue(model);
        Files.createDirectories(legacy.projectRoot());
        Files.createDirectories(model.projectRoot());

        Execution legacyExecution = execute(arguments(command, legacy, false));
        Execution modelExecution = execute(arguments(command, model, true));

        assertEquals(0, legacyExecution.exitCode());
        assertEquals(legacyExecution, modelExecution);
    }

    private void createEmptyQueue(Layout layout) throws Exception {
        DurableSingleWorkerSchedulerQueue.create(
                QUEUE_ID,
                8,
                new FileSystemSchedulerQueueStore(layout.queueRoot()));
    }

    private String[] arguments(String command, Layout layout, boolean modelAware) {
        List<String> arguments = new ArrayList<>(List.of(
                command,
                "--project-root", layout.projectRoot().toString(),
                "--queue-root", layout.queueRoot().toString(),
                "--queue-id", QUEUE_ID,
                "--runtime-root", layout.runtimeRoot().toString(),
                "--external-effect-root", layout.effectRoot().toString(),
                "--cycle-checkpoint-root", layout.checkpointRoot().toString(),
                "--evidence-root", layout.evidenceRoot().toString(),
                "--run-record-root", layout.recordRoot().toString(),
                "--invocation-root", layout.invocationRoot().toString(),
                "--owner-id", "model-selection-owner",
                "--max-attempts", "2",
                "--lease-millis", "300000",
                "--process-timeout-millis", "20000"));
        if (command.equals("scheduler-drain")) {
            arguments.addAll(List.of("--max-cycles", "8"));
        } else if (command.equals("scheduler-service")) {
            arguments.addAll(List.of(
                    "--max-cycles", "8",
                    "--max-consecutive-idle-cycles", "1",
                    "--idle-wait-millis", "1"));
        }
        if (modelAware) {
            arguments.addAll(List.of(
                    "--model-execution", "deterministic-fake-v2",
                    "--model-gateway-timeout-millis", "1000",
                    "--model-maximum-response-characters", "20000",
                    "--model-maximum-read-bytes", "65536",
                    "--model-tool-timeout-millis", "2000"));
        }
        return arguments.toArray(String[]::new);
    }

    private Execution execute(String[] arguments) {
        ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream standardError = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(
                arguments,
                new PrintStream(standardOutput, true, StandardCharsets.UTF_8),
                new PrintStream(standardError, true, StandardCharsets.UTF_8));
        return new Execution(
                exitCode,
                standardOutput.toString(StandardCharsets.UTF_8),
                standardError.toString(StandardCharsets.UTF_8));
    }

    private Layout layout(String name) {
        Path root = temporaryRoot.resolve(name);
        return new Layout(
                root.resolve("project"),
                root.resolve("queue"),
                root.resolve("runtime"),
                root.resolve("effects"),
                root.resolve("checkpoint"),
                root.resolve("evidence"),
                root.resolve("records"),
                root.resolve("invocations"));
    }

    private record Layout(
            Path projectRoot,
            Path queueRoot,
            Path runtimeRoot,
            Path effectRoot,
            Path checkpointRoot,
            Path evidenceRoot,
            Path recordRoot,
            Path invocationRoot) {
    }

    private record Execution(int exitCode, String stdout, String stderr) {
    }
}
