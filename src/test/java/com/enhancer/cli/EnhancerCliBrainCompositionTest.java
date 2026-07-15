package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.context.RequiredProjectDocument;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliBrainCompositionTest {
    private static final String TASK_ID = "gate-6-composition-test";

    @TempDir
    Path temporaryRoot;

    @Test
    void reportsBoundedSnapshotIdentityAndFreshnessForACompletedRun() throws Exception {
        Path projectRoot = temporaryRoot.resolve("project");
        String targetContent = "compose-the-brain-view\n";
        writeProject(projectRoot, targetContent);

        Captured captured = execute(runArguments(projectRoot, sha256(targetContent)));

        assertEquals(CliExitCode.COMPLETED.code(), captured.exitCode());
        String snapshotId = value(captured.stdout(), "workspaceSnapshotId");
        assertTrue(snapshotId.matches("[0-9a-f]{64}"));
        assertEquals(
                Integer.toString(RequiredProjectDocument.values().length + 3),
                value(captured.stdout(), "workspaceObservations"));
        assertEquals(
                "matched=" + RequiredProjectDocument.values().length
                        + ",diverged=0,notObserved=0",
                value(captured.stdout(), "memoryFreshness"));
        assertTrue(captured.stdout().length() <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
        assertFalse(captured.stdout().contains("compose-the-brain-view"));
    }

    @Test
    void composesTheViewForAFailedRunThatStillProducesARecord() throws Exception {
        Path projectRoot = temporaryRoot.resolve("failed-project");
        writeProject(projectRoot, "actual-content");

        Captured captured = execute(runArguments(projectRoot, sha256("different-content")));

        assertEquals(CliExitCode.VERIFICATION_FAILED.code(), captured.exitCode());
        assertTrue(captured.stdout().contains("verificationStatus=REJECTED"));
        assertTrue(value(captured.stdout(), "workspaceSnapshotId").matches("[0-9a-f]{64}"));
        assertEquals(
                "matched=" + RequiredProjectDocument.values().length
                        + ",diverged=0,notObserved=0",
                value(captured.stdout(), "memoryFreshness"));
    }

    private Captured execute(String[] arguments) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(
                arguments,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Captured(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private String[] runArguments(Path projectRoot, String expectedSha256) {
        return new String[] {
                "run",
                "--project-root", projectRoot.toString(),
                "--task-id", TASK_ID,
                "--target-path", "target.txt",
                "--expected-sha256", expectedSha256,
                "--evidence-root", temporaryRoot.resolve(
                        projectRoot.getFileName() + "-evidence").toString(),
                "--run-record-root", temporaryRoot.resolve(
                        projectRoot.getFileName() + "-records").toString()
        };
    }

    private void writeProject(Path projectRoot, String targetContent) throws Exception {
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            String content = document == RequiredProjectDocument.CURRENT_TASK
                    ? "# Current Task\n\n"
                            + "## Status\n\nIn Progress\n\n"
                            + "## Task\n\nCompose the Project Brain view on the run path.\n\n"
                            + "## Task ID\n\n" + TASK_ID + "\n\n"
                            + "## Approval\n\nApproved by the integration-test owner.\n\n"
                            + "## Allowed Tools\n\n- read-file\n"
                    : "# " + document.name() + "\n";
            Files.writeString(path, content, StandardCharsets.UTF_8);
        }
        Files.writeString(projectRoot.resolve("target.txt"), targetContent, StandardCharsets.UTF_8);
    }

    private String value(String output, String key) {
        return output.lines()
                .filter(line -> line.startsWith(key + "="))
                .map(line -> line.substring(key.length() + 1))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "output does not contain " + key + "=\n" + output));
    }

    private String sha256(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private record Captured(int exitCode, String stdout, String stderr) {
    }
}
