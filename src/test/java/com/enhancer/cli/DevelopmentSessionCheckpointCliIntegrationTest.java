package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DevelopmentSessionCheckpointCliIntegrationTest {
    @TempDir
    Path temporaryRoot;

    @Test
    void cliStartsRecordsShowsAndClearsARecoverableCheckpoint() throws Exception {
        writeTask();
        Files.writeString(temporaryRoot.resolve("artifact.txt"), "one");

        Invocation start = invoke(
                "checkpoint-start",
                "--project-root", temporaryRoot.toString(),
                "--step", "edit code",
                "--next-action", "record outcome",
                "--artifact", "artifact.txt");
        assertEquals(0, start.exitCode());
        String runId = value(start.stdout(), "runId");
        assertEquals("1", value(start.stdout(), "revision"));
        assertEquals("STEP_PENDING", value(start.stdout(), "state"));

        Files.writeString(temporaryRoot.resolve("artifact.txt"), "two");
        Invocation record = invoke(
                "checkpoint-record",
                "--project-root", temporaryRoot.toString(),
                "--run-id", runId,
                "--expected-revision", "1",
                "--state", "STEP_SUCCEEDED",
                "--step", "edit code",
                "--next-action", "run tests",
                "--artifact", "artifact.txt",
                "--evidence", "focused-green");
        assertEquals(0, record.exitCode());
        assertEquals("2", value(record.stdout(), "revision"));

        Invocation show = invoke(
                "checkpoint-show",
                "--project-root", temporaryRoot.toString());
        assertEquals(0, show.exitCode());
        assertEquals("ACTIVE", value(show.stdout(), "checkpointStatus"));
        assertEquals("edit code", value(show.stdout(), "lastSuccessfulStep"));
        assertEquals("run tests", value(show.stdout(), "nextAction"));
        assertEquals("true", value(show.stdout(), "taskContractMatches"));
        assertEquals("0", value(show.stdout(), "artifactMismatches"));

        Invocation stable = invoke(
                "checkpoint-record",
                "--project-root", temporaryRoot.toString(),
                "--run-id", runId,
                "--expected-revision", "2",
                "--state", "STABLE",
                "--step", "session close synchronized",
                "--next-action", "clear checkpoint",
                "--artifact", "artifact.txt",
                "--evidence", "full-build");
        assertEquals(0, stable.exitCode());

        Invocation clear = invoke(
                "checkpoint-clear",
                "--project-root", temporaryRoot.toString(),
                "--run-id", runId,
                "--expected-revision", "3");
        assertEquals(0, clear.exitCode());
        assertEquals("CLEARED", value(clear.stdout(), "checkpointStatus"));
        assertFalse(Files.exists(temporaryRoot.resolve(
                ".enhancer/session-checkpoint/session.checkpoint")));
    }

    @Test
    void showOnFreshProjectIsEmptyAndMalformedCommandsAreBoundedUsageFailures()
            throws Exception {
        writeTask();
        Invocation show = invoke(
                "checkpoint-show",
                "--project-root", temporaryRoot.toString());
        assertEquals(0, show.exitCode());
        assertEquals("EMPTY", value(show.stdout(), "checkpointStatus"));

        Invocation escapingArtifact = invoke(
                "checkpoint-start",
                "--project-root", temporaryRoot.toString(),
                "--step", "edit code",
                "--next-action", "record outcome",
                "--artifact", "../outside.txt");
        assertEquals(
                CliExitCode.USAGE_OR_CONFIGURATION.code(),
                escapingArtifact.exitCode());
        assertFalse(Files.exists(temporaryRoot.resolve(
                ".enhancer/session-checkpoint/session.checkpoint")));

        Invocation malformed = invoke(
                "checkpoint-record",
                "--project-root", temporaryRoot.toString());
        assertEquals(CliExitCode.USAGE_OR_CONFIGURATION.code(), malformed.exitCode());
        assertTrue(malformed.stderr().length() <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
    }

    private Invocation invoke(String... arguments) {
        ByteArrayOutputStream stdoutBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream stderrBytes = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(
                arguments,
                new PrintStream(stdoutBytes, true, StandardCharsets.UTF_8),
                new PrintStream(stderrBytes, true, StandardCharsets.UTF_8));
        return new Invocation(
                exitCode,
                stdoutBytes.toString(StandardCharsets.UTF_8),
                stderrBytes.toString(StandardCharsets.UTF_8));
    }

    private String value(String output, String name) {
        return output.lines()
                .filter(line -> line.startsWith(name + "="))
                .map(line -> line.substring(name.length() + 1))
                .findFirst()
                .orElseThrow();
    }

    private void writeTask() throws Exception {
        Files.writeString(
                temporaryRoot.resolve("CURRENT_TASK.md"),
                """
                # Current Task

                ## Status

                In Progress

                ## Task

                Persist one session checkpoint.

                ## Task ID

                persist-development-session-checkpoints

                ## Justified By

                - Accepted checkpoint decision

                ## Acceptance Criteria

                - Recover after interruption.

                ## Out Of Scope

                - Automatic commits.

                ## Approval

                Approved by the user.

                ## Verification

                Pending.

                ## Next

                Continue roadmap work.
                """,
                StandardCharsets.UTF_8);
    }

    private record Invocation(int exitCode, String stdout, String stderr) {
    }
}
