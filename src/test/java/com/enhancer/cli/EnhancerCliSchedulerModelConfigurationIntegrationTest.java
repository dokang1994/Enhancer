package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliSchedulerModelConfigurationIntegrationTest {
    @TempDir
    Path temporaryRoot;

    @Test
    void invalidModelGroupFailsBeforeStoreAccessForEverySchedulerExecutionCommand() {
        for (String command : List.of(
                "scheduler-cycle", "scheduler-drain", "scheduler-service")) {
            Path commandRoot = temporaryRoot.resolve(command);
            ByteArrayOutputStream standardError = new ByteArrayOutputStream();

            int exitCode = new EnhancerCli().execute(
                    partialModelArguments(command, commandRoot),
                    new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                    new PrintStream(standardError, true, StandardCharsets.UTF_8));

            assertEquals(CliExitCode.USAGE_OR_CONFIGURATION.code(), exitCode);
            assertFalse(Files.exists(commandRoot));
        }
    }

    private String[] partialModelArguments(String command, Path root) {
        List<String> arguments = new ArrayList<>(List.of(
                command,
                "--project-root", root.resolve("project").toString(),
                "--queue-root", root.resolve("queue").toString(),
                "--queue-id", "queue",
                "--runtime-root", root.resolve("runtime").toString(),
                "--external-effect-root", root.resolve("effects").toString(),
                "--cycle-checkpoint-root", root.resolve("checkpoints").toString(),
                "--evidence-root", root.resolve("evidence").toString(),
                "--run-record-root", root.resolve("records").toString(),
                "--invocation-root", root.resolve("invocations").toString(),
                "--owner-id", "owner",
                "--max-attempts", "2",
                "--lease-millis", "300000",
                "--process-timeout-millis", "20000"));
        if (command.equals("scheduler-drain")) {
            arguments.addAll(List.of("--max-cycles", "8"));
        } else if (command.equals("scheduler-service")) {
            arguments.addAll(List.of(
                    "--max-cycles", "8",
                    "--max-consecutive-idle-cycles", "3",
                    "--idle-wait-millis", "250"));
        }
        arguments.addAll(List.of(
                "--model-execution",
                SchedulerModelExecutionCliConfiguration.EXECUTION));
        return arguments.toArray(String[]::new);
    }
}
