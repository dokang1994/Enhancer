package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.kernel.VerificationStatus;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliIntegrationTest {
    @TempDir
    Path temporaryRoot;

    @Test
    void runsTheGovernedReadOnlyPipelineAndReplaysTheDurableRecord() throws Exception {
        Path projectRoot = temporaryRoot.resolve("project");
        String secretContent = "never-print-complete-evidence\n".repeat(600);
        writeProject(projectRoot, secretContent);
        Path evidenceRoot = temporaryRoot.resolve("evidence");
        Path runRecordRoot = temporaryRoot.resolve("records");
        Captured first = execute(runArguments(
                projectRoot,
                evidenceRoot,
                runRecordRoot,
                sha256(secretContent),
                "gate-5-run-test"));

        assertEquals(CliExitCode.COMPLETED.code(), first.exitCode());
        assertTrue(first.stdout().contains("status=COMPLETED"));
        assertTrue(first.stdout().contains("verificationStatus=VERIFIED"));
        assertTrue(first.stdout().contains("runRecordReference=run-record/"));
        assertTrue(first.stdout().length() <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
        assertFalse(first.stdout().contains("never-print-complete-evidence"));
        assertEquals("", first.stderr());

        String reference = value(first.stdout(), "runRecordReference");
        ResolvedRunRecord resolved = new FileSystemRunRecordStore(runRecordRoot)
                .resolve(reference);
        assertEquals("gate-5-run-test", resolved.record().approvedTask().taskId());
        assertEquals(VerificationStatus.VERIFIED, resolved.record().verification().status());

        Captured replay = execute(new String[] {
                "replay",
                "--run-record-root", runRecordRoot.toString(),
                "--reference", reference
        });

        assertEquals(CliExitCode.COMPLETED.code(), replay.exitCode());
        assertTrue(replay.stdout().contains("taskId=gate-5-run-test"));
        assertTrue(replay.stdout().contains("finalStopReason=COMPLETED"));
        assertTrue(replay.stdout().contains("verificationStatus=VERIFIED"));
        assertFalse(replay.stdout().contains("never-print-complete-evidence"));
        assertTrue(replay.stdout().length() <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
    }

    @Test
    void recordsVerificationMismatchAndReturnsTheStableFailureCode() throws Exception {
        Path projectRoot = temporaryRoot.resolve("mismatch-project");
        writeProject(projectRoot, "actual-content");
        Path recordRoot = temporaryRoot.resolve("mismatch-records");

        Captured captured = execute(runArguments(
                projectRoot,
                temporaryRoot.resolve("mismatch-evidence"),
                recordRoot,
                sha256("different-content"),
                "gate-5-run-test"));

        assertEquals(CliExitCode.VERIFICATION_FAILED.code(), captured.exitCode());
        assertTrue(captured.stdout().contains("status=AWAITING_VERIFICATION"));
        assertTrue(captured.stdout().contains("verificationStatus=REJECTED"));
        String reference = value(captured.stdout(), "runRecordReference");
        ResolvedRunRecord resolved = new FileSystemRunRecordStore(recordRoot).resolve(reference);
        assertEquals(VerificationStatus.REJECTED, resolved.record().verification().status());
    }

    @Test
    void rejectsTaskMismatchAndKeepsDiagnosticsBounded() throws Exception {
        Path projectRoot = temporaryRoot.resolve("task-project");
        writeProject(projectRoot, "content");

        Captured captured = execute(runArguments(
                projectRoot,
                temporaryRoot.resolve("task-evidence"),
                temporaryRoot.resolve("task-records"),
                sha256("content"),
                "different-task"));

        assertEquals(CliExitCode.USAGE_OR_CONFIGURATION.code(), captured.exitCode());
        assertEquals("", captured.stdout());
        assertTrue(captured.stderr().contains("task-id"));
        assertTrue(captured.stderr().length() <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
    }

    @Test
    void persistsAndReplaysAToolFailure() throws Exception {
        Path projectRoot = temporaryRoot.resolve("tool-failure-project");
        writeProject(projectRoot, "content");
        Files.delete(projectRoot.resolve("target.txt"));
        Path recordRoot = temporaryRoot.resolve("tool-failure-records");

        Captured captured = execute(runArguments(
                projectRoot,
                temporaryRoot.resolve("tool-failure-evidence"),
                recordRoot,
                sha256("content"),
                "gate-5-run-test"));

        assertEquals(CliExitCode.TOOL_FAILED.code(), captured.exitCode());
        assertTrue(captured.stdout().contains("status=FAILED"));
        String reference = value(captured.stdout(), "runRecordReference");
        ResolvedRunRecord resolved = new FileSystemRunRecordStore(recordRoot).resolve(reference);
        assertTrue(resolved.record().toolResult().failureCode().isPresent());

        Captured replay = execute(new String[] {
                "replay",
                "--run-record-root", recordRoot.toString(),
                "--reference", reference
        });
        assertEquals(CliExitCode.TOOL_FAILED.code(), replay.exitCode());
        assertTrue(replay.stdout().contains("finalStopReason=FAILED"));
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
            String expectedSha256,
            String taskId) {
        return new String[] {
                "run",
                "--project-root", projectRoot.toString(),
                "--task-id", taskId,
                "--target-path", "target.txt",
                "--expected-sha256", expectedSha256,
                "--evidence-root", evidenceRoot.toString(),
                "--run-record-root", runRecordRoot.toString()
        };
    }

    private void writeProject(Path projectRoot, String targetContent) throws Exception {
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            String content = document == RequiredProjectDocument.CURRENT_TASK
                    ? "# Current Task\n\n"
                            + "## Status\n\nIn Progress\n\n"
                            + "## Task\n\nRun the first operational read-only CLI.\n\n"
                            + "## Task ID\n\ngate-5-run-test\n\n"
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
                .orElseThrow();
    }

    private String sha256(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private record Captured(int exitCode, String stdout, String stderr) {
    }
}
