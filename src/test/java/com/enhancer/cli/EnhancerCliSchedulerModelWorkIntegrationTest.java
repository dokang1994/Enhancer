package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.WorkPayload;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.runtime.DurableSubmissionManifest;
import com.enhancer.runtime.FileSystemSubmissionManifestStore;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.FileSystemEvidenceStore;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The promoting integration test for scheduler-executed model invocations: one
 * governed CLI submission of a model-scoped WorkItem and one real-filesystem
 * Scheduler cycle in a real child process to its verified terminal disposition,
 * with the persisted RunRecord and its evidence reference resolvable and exact
 * re-entry creating no second execution.
 */
class EnhancerCliSchedulerModelWorkIntegrationTest {
    private static final String TASK_ID = "scheduler-model-work-cli-test";
    private static final String MODEL_CLASS = "reasoning-standard";
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000e01";
    private static final String MESSAGE_ID =
            "00000000-0000-0000-0000-000000000e02";

    @TempDir
    Path temporaryRoot;

    @Test
    void submitsAndExecutesAModelWorkItemThroughARealSchedulerCycle()
            throws Exception {
        Layout layout = layout("verified");
        writeGovernedProject(layout.projectRoot(), "- model-invoke\n");
        String prompt = "model prompt content ".repeat(300);
        Files.createDirectories(layout.projectRoot().resolve("docs"));
        Files.writeString(
                layout.projectRoot().resolve("docs/prompt.md"),
                prompt,
                StandardCharsets.UTF_8);
        String expectedResponse = deterministicResponse(prompt, MODEL_CLASS);

        Execution submit = run(submitArguments(layout, sha256(expectedResponse)));

        assertEquals(0, submit.exitCode());
        assertTrue(submit.stdout().contains("status=ADMITTED"));
        DurableSubmissionManifest manifest = new FileSystemSubmissionManifestStore(
                layout.submissionRoot()).resolve(MESSAGE_ID);
        WorkPayload payload = (WorkPayload) manifest.workMessage().payload();
        assertEquals(List.of("model-invoke"), payload.allowedTools().stream().toList());

        Execution cycle = run(cycleArguments(layout));

        assertEquals(0, cycle.exitCode(), () -> cycle.stderr() + cycle.stdout());
        assertTrue(cycle.stdout().contains("status=VERIFIED_COMPLETED"));
        FileSystemRunRecordStore recordStore =
                new FileSystemRunRecordStore(layout.recordRoot());
        assertEquals(1, recordStore.references().size());
        ResolvedRunRecord resolved = recordStore.resolve(
                recordStore.references().iterator().next());
        assertEquals("model-invoke", resolved.record().toolRequest().toolName());
        assertEquals(
                VerificationStatus.VERIFIED,
                resolved.record().verification().status());
        assertTrue(resolved.record().toolResult().evidence().truncated());
        String evidenceReference = resolved.record().toolResult().evidence()
                .fullOutputReference().orElseThrow();
        String completeEvidence = new FileSystemEvidenceStore(
                layout.evidenceRoot(),
                new EvidenceStoragePolicy(EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES))
                .resolve(evidenceReference)
                .content();
        assertEquals(expectedResponse, completeEvidence);

        Execution idle = run(cycleArguments(layout));

        assertEquals(0, idle.exitCode());
        assertTrue(idle.stdout().contains("status=IDLE"));
        assertEquals(1, recordStore.references().size());

        Execution replay = run(submitArguments(layout, sha256(expectedResponse)));

        assertEquals(0, replay.exitCode());
        assertTrue(replay.stdout().contains("status=REPLAYED"));
    }

    @Test
    void rejectsATaskAllowingNoExecutableTool() throws Exception {
        Layout layout = layout("no-executable-tool");
        writeGovernedProject(layout.projectRoot(), "- write-docs\n");
        Files.createDirectories(layout.projectRoot().resolve("docs"));
        Files.writeString(
                layout.projectRoot().resolve("docs/prompt.md"),
                "prompt",
                StandardCharsets.UTF_8);

        Execution submit = run(submitArguments(layout, "0".repeat(64)));

        assertEquals(2, submit.exitCode());
        assertTrue(submit.stderr().contains("executable tool"));
        assertTrue(Files.notExists(layout.queueRoot()));
    }

    private String[] submitArguments(Layout layout, String expectedSha256) {
        return new String[] {
                "scheduler-submit",
                "--project-root", layout.projectRoot().toString(),
                "--submission-root", layout.submissionRoot().toString(),
                "--queue-root", layout.queueRoot().toString(),
                "--task-id", TASK_ID,
                "--queue-id", QUEUE_ID,
                "--max-work-items", "8",
                "--required-capability", MODEL_CLASS,
                "--message-id", MESSAGE_ID,
                "--correlation-id", "scheduler-model-work-correlation",
                "--logical-run-id", "scheduler-model-work-logical-run",
                "--producer", "scheduler-model-work-cli-test",
                "--occurred-at", "2026-08-19T09:00:00Z",
                "--target-path", "docs/prompt.md",
                "--expected-sha256", expectedSha256
        };
    }

    private String[] cycleArguments(Layout layout) {
        return new String[] {
                "scheduler-cycle",
                "--project-root", layout.projectRoot().toString(),
                "--queue-root", layout.queueRoot().toString(),
                "--queue-id", QUEUE_ID,
                "--runtime-root", layout.runtimeRoot().toString(),
                "--external-effect-root", layout.effectRoot().toString(),
                "--cycle-checkpoint-root", layout.checkpointRoot().toString(),
                "--evidence-root", layout.evidenceRoot().toString(),
                "--run-record-root", layout.recordRoot().toString(),
                "--invocation-root", layout.invocationRoot().toString(),
                "--owner-id", "scheduler-model-work-owner",
                "--max-attempts", "2",
                "--lease-millis", "300000",
                "--process-timeout-millis", "30000"
        };
    }

    private Execution run(String[] arguments) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(
                arguments,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Execution(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private void writeGovernedProject(Path projectRoot, String allowedToolBullets)
            throws Exception {
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            Files.writeString(
                    path,
                    document == RequiredProjectDocument.CURRENT_TASK
                            ? "# Current Task\n\n"
                                    + "## Status\n\nIn Progress\n\n"
                                    + "## Task\n\nExecute governed model work.\n\n"
                                    + "## Task ID\n\n" + TASK_ID + "\n\n"
                                    + "## Approval\n\nApproved by the test owner.\n\n"
                                    + "## Allowed Tools\n\n" + allowedToolBullets
                            : "# " + document.name() + "\n",
                    StandardCharsets.UTF_8);
        }
    }

    private String deterministicResponse(String prompt, String modelClass)
            throws Exception {
        return "deterministic-fake-v1\n"
                + "model-class=" + modelClass + "\n"
                + "prompt-sha256=" + sha256(prompt) + "\n"
                + "prompt-length=" + prompt.length() + "\n"
                + "echo=" + prompt;
    }

    private String sha256(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private Layout layout(String name) {
        Path root = temporaryRoot.resolve(name);
        return new Layout(
                root.resolve("project"),
                root.resolve("submissions"),
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
            Path submissionRoot,
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
