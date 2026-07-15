package com.enhancer.integration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.cli.CliExitCode;
import com.enhancer.cli.EnhancerCli;
import com.enhancer.context.ProjectContextReader;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.loop.ApprovedTaskReader;
import com.enhancer.loop.AssistedDevelopmentLoop;
import com.enhancer.loop.AssistedDevelopmentOutcome;
import com.enhancer.planner.RepositoryTaskPlanner;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.verification.VerificationStatus;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FoundationLifecycleIntegrationTest {
    private static final String TASK_ID = "gate-0-lifecycle-test";

    @TempDir
    Path temporaryRoot;

    @Test
    void connectsPlanningToExplicitlyActivatedVerifiedExecutionWithoutGrantingAuthority()
            throws Exception {
        Path projectRoot = temporaryRoot.resolve("project");
        String targetContent = "complete-foundation-evidence\n".repeat(600);
        writeGovernedProject(projectRoot, completedTask(), targetContent);
        Map<Path, byte[]> beforePlanning = snapshotDocuments(projectRoot);

        var planning = new AssistedDevelopmentLoop(
                new ProjectContextReader(),
                new RepositoryTaskPlanner())
                .run(projectRoot);

        assertEquals(AssistedDevelopmentOutcome.PROPOSAL_AVAILABLE, planning.outcome());
        assertEquals(
                "Delivery Gate 6: Workspace And Project Brain Foundation",
                planning.proposal().orElseThrow().title());
        beforePlanning.forEach((path, content) -> assertArrayEquals(
                content,
                readBytes(path),
                "planning must not mutate " + projectRoot.relativize(path)));

        Path evidenceRoot = temporaryRoot.resolve("evidence");
        Path runRecordRoot = temporaryRoot.resolve("run-records");
        Captured beforeActivation = execute(runArguments(
                projectRoot,
                evidenceRoot,
                runRecordRoot,
                sha256(targetContent)));
        assertEquals(CliExitCode.USAGE_OR_CONFIGURATION.code(), beforeActivation.exitCode());
        assertEquals("", beforeActivation.stdout());
        assertFalse(Files.exists(evidenceRoot));
        assertFalse(Files.exists(runRecordRoot));

        Files.writeString(
                projectRoot.resolve("CURRENT_TASK.md"),
                activeTask(),
                StandardCharsets.UTF_8);
        ApprovedTask approvedTask = new ApprovedTaskReader().read(
                new ProjectContextReader().read(projectRoot));
        assertEquals(TASK_ID, approvedTask.taskId());
        assertTrue(approvedTask.allows("read-file"));

        Captured run = execute(runArguments(
                projectRoot,
                evidenceRoot,
                runRecordRoot,
                sha256(targetContent)));
        assertEquals(CliExitCode.COMPLETED.code(), run.exitCode());
        assertTrue(run.stdout().contains("status=COMPLETED"));
        assertTrue(run.stdout().contains("verificationStatus=VERIFIED"));
        assertFalse(run.stdout().contains("complete-foundation-evidence"));

        String reference = value(run.stdout(), "runRecordReference");
        ResolvedRunRecord resolved = new FileSystemRunRecordStore(runRecordRoot)
                .resolve(reference);
        assertEquals(TASK_ID, resolved.record().approvedTask().taskId());
        assertEquals(AgentLoopStopReason.COMPLETED, resolved.record().finalStopReason());
        assertEquals(VerificationStatus.VERIFIED, resolved.record().verification().status());
        assertTrue(resolved.record().toolResult().evidence().truncated());
        assertTrue(resolved.record().toolResult().evidence().fullOutputReference().isPresent());

        Files.delete(projectRoot.resolve("target.txt"));
        Captured replay = execute(new String[] {
                "replay",
                "--run-record-root", runRecordRoot.toString(),
                "--reference", reference
        });
        assertEquals(CliExitCode.COMPLETED.code(), replay.exitCode());
        assertTrue(replay.stdout().contains("finalStopReason=COMPLETED"));
        assertTrue(replay.stdout().contains("verificationStatus=VERIFIED"));
    }

    private void writeGovernedProject(
            Path projectRoot,
            String currentTask,
            String targetContent) throws Exception {
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            String content = switch (document) {
                case CURRENT_TASK -> currentTask;
                case ROADMAP -> roadmap();
                default -> "# " + document.name() + "\n";
            };
            Files.writeString(path, content, StandardCharsets.UTF_8);
        }
        Files.writeString(
                projectRoot.resolve("target.txt"),
                targetContent,
                StandardCharsets.UTF_8);
    }

    private Map<Path, byte[]> snapshotDocuments(Path projectRoot) throws Exception {
        Map<Path, byte[]> snapshot = new LinkedHashMap<>();
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            snapshot.put(path, Files.readAllBytes(path));
        }
        return Map.copyOf(snapshot);
    }

    private byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
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

    private String[] runArguments(
            Path projectRoot,
            Path evidenceRoot,
            Path runRecordRoot,
            String expectedSha256) {
        return new String[] {
                "run",
                "--project-root", projectRoot.toString(),
                "--task-id", TASK_ID,
                "--target-path", "target.txt",
                "--expected-sha256", expectedSha256,
                "--evidence-root", evidenceRoot.toString(),
                "--run-record-root", runRecordRoot.toString()
        };
    }

    private String value(String output, String key) {
        return output.lines()
                .filter(line -> line.startsWith(key + "="))
                .map(line -> line.substring(key.length() + 1))
                .findFirst()
                .orElseThrow();
    }

    private String sha256(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private String completedTask() {
        return "# Current Task\n\n"
                + "## Status\n\nCompleted\n";
    }

    private String activeTask() {
        return "# Current Task\n\n"
                + "## Status\n\nIn Progress\n\n"
                + "## Task\n\nRun the authority-preserving foundation lifecycle.\n\n"
                + "## Task ID\n\n" + TASK_ID + "\n\n"
                + "## Approval\n\nApproved by the integration-test owner.\n\n"
                + "## Allowed Tools\n\n- read-file\n";
    }

    private String roadmap() {
        return "# Roadmap\n\n"
                + "## Delivery Gate 6: Workspace And Project Brain Foundation\n\n"
                + "Status: Specified - Next\n\n"
                + "Scope:\n\n"
                + "- immutable WorkspaceSnapshot and source freshness metadata;\n\n"
                + "Exit criteria:\n\n"
                + "- Workspace observations cannot override repository authority or grant Tool permission;\n";
    }

    private record Captured(int exitCode, String stdout, String stderr) {
    }
}
