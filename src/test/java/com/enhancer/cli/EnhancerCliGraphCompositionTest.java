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
import java.util.stream.Stream;
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
                Integer.toString(DOCUMENTS + 3),
                value(first.stdout(), "workspaceObservations"));
        assertEquals("2", value(first.stdout(), "graphDecisions"));
        assertEquals(
                Integer.toString(DOCUMENTS + 1 + 2 + 2),
                value(first.stdout(), "graphNodes"));
        assertEquals("2", value(first.stdout(), "graphEdges"));
        assertEquals("1", value(first.stdout(), "impactExecutions"));
        assertEquals("1", value(first.stdout(), "impactDecisions"));
        assertTrue(first.stdout().length() <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
        assertFalse(first.stdout().contains("Adopt The First Rule"));

        Captured second = execute(runArguments(projectRoot, sha256(targetContent)));

        assertEquals(CliExitCode.COMPLETED.code(), second.exitCode());
        assertEquals(
                Integer.toString(DOCUMENTS + 3 + 1),
                value(second.stdout(), "workspaceObservations"));
        assertEquals(
                Integer.toString(DOCUMENTS + 1 + 2 + 2),
                value(second.stdout(), "graphNodes"));
        assertEquals("1", value(second.stdout(), "impactExecutions"));
    }

    @Test
    void completesWhenTheTargetIsAlreadyARequiredRepositoryDocument() throws Exception {
        Path projectRoot = temporaryRoot.resolve("required-document-project");
        writeProject(projectRoot, "unused-target\n");
        String currentTask = Files.readString(
                projectRoot.resolve("CURRENT_TASK.md"), StandardCharsets.UTF_8);

        Captured captured = execute(runArguments(
                projectRoot,
                "CURRENT_TASK.md",
                sha256(currentTask)));

        assertEquals(CliExitCode.COMPLETED.code(), captured.exitCode());
        assertEquals("AVAILABLE", value(captured.stdout(), "brainStatus"));
        assertEquals(1, storedRecordCount());
    }

    @Test
    void rejectsDuplicateDecisionMetadataBeforePersistingARun() throws Exception {
        Path projectRoot = temporaryRoot.resolve("duplicate-decision-project");
        String targetContent = "duplicate-decision-target\n";
        writeProject(projectRoot, targetContent);
        Files.writeString(
                projectRoot.resolve("DECISION_LOG.md"),
                "\n### 2026-07-14: Adopt The First Rule\n\nStatus: Accepted Decision\n",
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND);

        Captured captured = execute(runArguments(projectRoot, sha256(targetContent)));

        assertEquals(CliExitCode.USAGE_OR_CONFIGURATION.code(), captured.exitCode());
        assertEquals(0, storedRecordCount());
    }

    @Test
    void preservesTheDurableExitCodeWhenPostPersistBrainReportingFails() throws Exception {
        Path projectRoot = temporaryRoot.resolve("reporting-failure-project");
        String targetContent = "reporting-failure-target\n";
        writeProject(projectRoot, targetContent);
        EnhancerCli cli = new EnhancerCli(input -> {
            throw new IllegalStateException("injected brain reporting failure");
        });

        Captured captured = execute(cli, runArguments(projectRoot, sha256(targetContent)));

        assertEquals(CliExitCode.COMPLETED.code(), captured.exitCode());
        assertEquals("UNAVAILABLE", value(captured.stdout(), "brainStatus"));
        assertTrue(value(captured.stdout(), "brainReason").contains("injected brain"));
        assertEquals(1, storedRecordCount());
    }

    private Captured execute(String[] arguments) {
        return execute(new EnhancerCli(), arguments);
    }

    private Captured execute(EnhancerCli cli, String[] arguments) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = cli.execute(
                arguments,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Captured(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private String[] runArguments(Path projectRoot, String expectedSha256) {
        return runArguments(projectRoot, "target.txt", expectedSha256);
    }

    private String[] runArguments(
            Path projectRoot,
            String targetPath,
            String expectedSha256) {
        return new String[] {
                "run",
                "--project-root", projectRoot.toString(),
                "--task-id", TASK_ID,
                "--target-path", targetPath,
                "--expected-sha256", expectedSha256,
                "--evidence-root", temporaryRoot.resolve("evidence").toString(),
                "--run-record-root", temporaryRoot.resolve("records").toString()
        };
    }

    private long storedRecordCount() throws Exception {
        Path root = temporaryRoot.resolve("records");
        if (!Files.exists(root)) {
            return 0;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile).count();
        }
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
                        + "## Justified By\n\n- 2026-07-14: Adopt The First Rule\n\n"
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
