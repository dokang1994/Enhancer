package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.FileSystemEvidenceStore;
import com.enhancer.tool.ToolFailureCode;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The RFC-0013 promoting integration test: one governed CLI run executes
 * {@code model-invoke} against the deterministic fake and atomically persists a
 * lifecycle-valid replayable RunRecord whose evidence reference resolves.
 */
class EnhancerCliModelInvokeIntegrationTest {
    private static final String TASK_ID = "gate-9-model-invoke-test";

    @TempDir
    Path temporaryRoot;

    @Test
    void runsTheGovernedModelInvokePipelineAndReplaysTheDurableRecord() throws Exception {
        Path projectRoot = temporaryRoot.resolve("project");
        writeProject(projectRoot, "- read-file\n- model-invoke\n");
        Path evidenceRoot = temporaryRoot.resolve("evidence");
        Path runRecordRoot = temporaryRoot.resolve("records");
        String prompt = "prompt-content-".repeat(400);
        String expectedResponse = deterministicResponse(prompt, "reasoning-standard");

        Captured run = execute(new String[] {
                "model-invoke",
                "--project-root", projectRoot.toString(),
                "--task-id", TASK_ID,
                "--prompt", prompt,
                "--model-class", "reasoning-standard",
                "--expected-sha256", sha256(expectedResponse),
                "--evidence-root", evidenceRoot.toString(),
                "--run-record-root", runRecordRoot.toString()
        });

        assertEquals(CliExitCode.COMPLETED.code(), run.exitCode());
        assertTrue(run.stdout().contains("status=COMPLETED"));
        assertTrue(run.stdout().contains("verificationStatus=VERIFIED"));
        assertTrue(run.stdout().contains("modelClass=reasoning-standard"));
        assertTrue(run.stdout().contains("runRecordReference=run-record/"));
        assertFalse(run.stdout().contains("prompt-content-"));
        assertTrue(run.stdout().length() <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
        assertEquals("", run.stderr());

        String reference = value(run.stdout(), "runRecordReference");
        ResolvedRunRecord resolved = new FileSystemRunRecordStore(runRecordRoot)
                .resolve(reference);
        assertEquals(TASK_ID, resolved.record().approvedTask().taskId());
        assertEquals("model-invoke", resolved.record().toolRequest().toolName());
        assertEquals(
                VerificationStatus.VERIFIED,
                resolved.record().verification().status());
        assertTrue(resolved.record().toolResult().evidence().truncated());

        String evidenceReference = resolved.record().toolResult().evidence()
                .fullOutputReference().orElseThrow();
        String completeEvidence = new FileSystemEvidenceStore(
                evidenceRoot,
                new EvidenceStoragePolicy(EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES))
                .resolve(evidenceReference)
                .content();
        assertEquals(expectedResponse, completeEvidence);
        assertEquals(
                sha256(completeEvidence),
                resolved.record().toolResult().evidence().contentSha256().orElseThrow());

        Captured replay = execute(new String[] {
                "replay",
                "--run-record-root", runRecordRoot.toString(),
                "--reference", reference
        });

        assertEquals(CliExitCode.COMPLETED.code(), replay.exitCode());
        assertTrue(replay.stdout().contains("taskId=" + TASK_ID));
        assertTrue(replay.stdout().contains("toolName=model-invoke"));
        assertTrue(replay.stdout().contains("finalStopReason=COMPLETED"));
        assertTrue(replay.stdout().contains("verificationStatus=VERIFIED"));
    }

    @Test
    void recordsVerificationMismatchAndReturnsTheStableFailureCode() throws Exception {
        Path projectRoot = temporaryRoot.resolve("mismatch-project");
        writeProject(projectRoot, "- model-invoke\n");
        Path runRecordRoot = temporaryRoot.resolve("mismatch-records");

        Captured run = execute(new String[] {
                "model-invoke",
                "--project-root", projectRoot.toString(),
                "--task-id", TASK_ID,
                "--prompt", "a governed prompt",
                "--model-class", "reasoning-standard",
                "--expected-sha256", sha256("a different response"),
                "--evidence-root", temporaryRoot.resolve("mismatch-evidence").toString(),
                "--run-record-root", runRecordRoot.toString()
        });

        assertEquals(CliExitCode.VERIFICATION_FAILED.code(), run.exitCode());
        assertTrue(run.stdout().contains("verificationStatus=REJECTED"));
        String reference = value(run.stdout(), "runRecordReference");
        ResolvedRunRecord resolved = new FileSystemRunRecordStore(runRecordRoot)
                .resolve(reference);
        assertEquals(
                VerificationStatus.REJECTED,
                resolved.record().verification().status());
    }

    @Test
    void persistsAnExplicitBudgetRefusalFailure() throws Exception {
        Path projectRoot = temporaryRoot.resolve("budget-project");
        writeProject(projectRoot, "- model-invoke\n");
        Path runRecordRoot = temporaryRoot.resolve("budget-records");

        Captured run = execute(new String[] {
                "model-invoke",
                "--project-root", projectRoot.toString(),
                "--task-id", TASK_ID,
                "--prompt", "a prompt whose response cannot fit the declared budget",
                "--model-class", "reasoning-standard",
                "--expected-sha256", "0".repeat(64),
                "--evidence-root", temporaryRoot.resolve("budget-evidence").toString(),
                "--run-record-root", runRecordRoot.toString(),
                "--max-response-length", "16"
        });

        assertEquals(CliExitCode.TOOL_FAILED.code(), run.exitCode());
        assertTrue(run.stdout().contains("status=FAILED"));
        String reference = value(run.stdout(), "runRecordReference");
        ResolvedRunRecord resolved = new FileSystemRunRecordStore(runRecordRoot)
                .resolve(reference);
        assertEquals(
                ToolFailureCode.TOOL_REPORTED_FAILURE,
                resolved.record().toolResult().failureCode().orElseThrow());
    }

    @Test
    void rejectsATaskThatDoesNotAllowModelInvoke() throws Exception {
        Path projectRoot = temporaryRoot.resolve("scope-project");
        writeProject(projectRoot, "- read-file\n");

        Captured run = execute(new String[] {
                "model-invoke",
                "--project-root", projectRoot.toString(),
                "--task-id", TASK_ID,
                "--prompt", "a governed prompt",
                "--model-class", "reasoning-standard",
                "--expected-sha256", "0".repeat(64),
                "--evidence-root", temporaryRoot.resolve("scope-evidence").toString(),
                "--run-record-root", temporaryRoot.resolve("scope-records").toString()
        });

        assertEquals(CliExitCode.USAGE_OR_CONFIGURATION.code(), run.exitCode());
        assertEquals("", run.stdout());
        assertTrue(run.stderr().contains("model-invoke"));
        assertTrue(run.stderr().length() <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
    }

    @Test
    void rejectsAMalformedExpectedDigestBeforeExecution() throws Exception {
        Path projectRoot = temporaryRoot.resolve("digest-project");
        writeProject(projectRoot, "- model-invoke\n");

        Captured run = execute(new String[] {
                "model-invoke",
                "--project-root", projectRoot.toString(),
                "--task-id", TASK_ID,
                "--prompt", "a governed prompt",
                "--model-class", "reasoning-standard",
                "--expected-sha256", "not-a-digest",
                "--evidence-root", temporaryRoot.resolve("digest-evidence").toString(),
                "--run-record-root", temporaryRoot.resolve("digest-records").toString()
        });

        assertEquals(CliExitCode.USAGE_OR_CONFIGURATION.code(), run.exitCode());
        assertTrue(run.stderr().contains("expected-sha256"));
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

    private void writeProject(Path projectRoot, String allowedToolBullets) throws Exception {
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            String content = document == RequiredProjectDocument.CURRENT_TASK
                    ? "# Current Task\n\n"
                            + "## Status\n\nIn Progress\n\n"
                            + "## Task\n\nRun the governed model-invoke slice.\n\n"
                            + "## Task ID\n\n" + TASK_ID + "\n\n"
                            + "## Approval\n\nApproved by the integration-test owner.\n\n"
                            + "## Allowed Tools\n\n" + allowedToolBullets
                    : "# " + document.name() + "\n";
            Files.writeString(path, content, StandardCharsets.UTF_8);
        }
    }

    private String deterministicResponse(String prompt, String modelClass) throws Exception {
        return "deterministic-fake-v1\n"
                + "model-class=" + modelClass + "\n"
                + "prompt-sha256=" + sha256(prompt) + "\n"
                + "prompt-length=" + prompt.length() + "\n"
                + "echo=" + prompt;
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
