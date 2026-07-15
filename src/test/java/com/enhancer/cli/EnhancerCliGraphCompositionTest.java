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

class EnhancerCliGraphCompositionTest {
    private static final String TASK_ID = "gate-6-graph-composition-test";
    private static final int DOCUMENTS = RequiredProjectDocument.values().length;

    @TempDir
    Path temporaryRoot;

    @Test
    void reportsGraphAndImpactCountsAndObservesPriorRunRecords() throws Exception {
        Path projectRoot = temporaryRoot.resolve("project");
        String targetContent = "graph-composition-target\n";
        writeProject(projectRoot, targetContent);

        Captured first = execute(runArguments(projectRoot, sha256(targetContent)));

        assertEquals(CliExitCode.COMPLETED.code(), first.exitCode());
        assertEquals(
                Integer.toString(DOCUMENTS),
                value(first.stdout(), "workspaceObservations"));
        assertEquals("2", value(first.stdout(), "graphDecisions"));
        assertEquals(
                Integer.toString(DOCUMENTS + 2 + 2),
                value(first.stdout(), "graphNodes"));
        assertEquals("1", value(first.stdout(), "graphEdges"));
        assertEquals("1", value(first.stdout(), "impactExecutions"));
        assertTrue(first.stdout().length() <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
        assertFalse(first.stdout().contains("Adopt The First Rule"));

        Captured second = execute(runArguments(projectRoot, sha256(targetContent)));

        assertEquals(CliExitCode.COMPLETED.code(), second.exitCode());
        assertEquals(
                Integer.toString(DOCUMENTS + 1),
                value(second.stdout(), "workspaceObservations"));
        assertEquals(
                Integer.toString(DOCUMENTS + 2 + 2),
                value(second.stdout(), "graphNodes"));
        assertEquals("1", value(second.stdout(), "impactExecutions"));
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
                "--evidence-root", temporaryRoot.resolve("evidence").toString(),
                "--run-record-root", temporaryRoot.resolve("records").toString()
        };
    }

    private void writeProject(Path projectRoot, String targetContent) throws Exception {
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            String content = switch (document) {
                case CURRENT_TASK -> "# Current Task\n\n"
                        + "## Status\n\nIn Progress\n\n"
                        + "## Task\n\nCompose the production graph.\n\n"
                        + "## Task ID\n\n" + TASK_ID + "\n\n"
                        + "## Approval\n\nApproved by the integration-test owner.\n\n"
                        + "## Allowed Tools\n\n- read-file\n";
                case DECISION_LOG -> "# Decision Log\n\n"
                        + "### 2026-07-14: Adopt The First Rule\n\n"
                        + "Status: Accepted Decision\n\n"
                        + "### 2026-07-15: Explore A Candidate\n\n"
                        + "Status: Proposal\n\n"
                        + "### 2026-07-15: Adopt The Second Rule\n\n"
                        + "Status: Accepted Decision\n";
                default -> "# " + document.name() + "\n";
            };
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
