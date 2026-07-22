package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.WorkPayload;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.DurableSubmissionManifest;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.runtime.FileSystemSubmissionManifestStore;
import com.enhancer.runtime.GeneratedSubmissionIdentities;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliSchedulerGeneratedSubmitIntegrationTest {
    private static final String TASK_ID = "generated-submit-cli-test";
    private static final String SUBMISSION_ID =
            "00000000-0000-0000-0000-000000000e01";
    private static final GeneratedSubmissionIdentities IDENTITIES =
            GeneratedSubmissionIdentities.derive(SUBMISSION_ID);

    @TempDir
    Path temporaryRoot;

    @Test
    void generatesIdentitiesAndReplaysExactlyAcrossFreshCliInstances()
            throws Exception {
        Layout layout = layout();
        String taskDocument = writeGovernedProject(layout.projectRoot());
        String targetDigest = writeTarget(layout.projectRoot(), "generated submit target\n");

        Execution first = execute(layout, TASK_ID, "generated-submit-cli", targetDigest);

        assertEquals(0, first.exitCode());
        assertTrue(first.stdout().contains("status=ADMITTED"));
        assertTrue(first.stdout().contains("manifestCreated=true"));
        assertTrue(first.stdout().contains("queueCreated=true"));
        assertTrue(first.stdout().contains("workAdmitted=true"));
        assertTrue(first.stdout().contains("queueId=" + IDENTITIES.queueId()));
        assertTrue(first.stdout().contains("correlationId=" + IDENTITIES.correlationId()));
        assertTrue(first.stdout().contains("logicalRunId=" + IDENTITIES.logicalRunId()));

        byte[] manifestBytes = soleManifestBytes(layout.submissionRoot());
        DurableSubmissionManifest manifest = new FileSystemSubmissionManifestStore(
                layout.submissionRoot()).resolve(SUBMISSION_ID);
        assertEquals(IDENTITIES.queueId(), manifest.queueId());
        WorkPayload payload = (WorkPayload) manifest.workMessage().payload();
        assertEquals(TASK_ID, payload.taskRevision().taskId());
        assertEquals(sha256(taskDocument), payload.taskRevision().sourceSha256());
        assertEquals(List.of("read-file"), payload.allowedTools().stream().sorted().toList());
        assertEquals(Optional.of(new WorkPayload.ExecutionInput(
                "target.txt", targetDigest)), payload.executionInput());
        DurableSingleWorkerSchedulerQueue admitted = recoverQueue(layout);
        assertEquals(1L, admitted.revision());
        assertEquals(1, admitted.pendingCount());

        // A fresh CLI instance resolves the manifest first and reuses the exact generated
        // occurrence time and envelope, so nothing is rewritten or re-revised.
        Execution replay = execute(layout, TASK_ID, "generated-submit-cli", targetDigest);

        assertEquals(0, replay.exitCode());
        assertTrue(replay.stdout().contains("status=REPLAYED"));
        assertTrue(replay.stdout().contains("manifestCreated=false"));
        assertTrue(replay.stdout().contains("queueCreated=false"));
        assertTrue(replay.stdout().contains("workAdmitted=false"));
        assertArrayEquals(manifestBytes, soleManifestBytes(layout.submissionRoot()));
        assertEquals(1L, recoverQueue(layout).revision());

        // Conflicting caller-owned intent under the same submission UUID fails closed.
        Execution conflict = execute(layout, TASK_ID, "changed-producer", targetDigest);
        assertEquals(2, conflict.exitCode());
        assertTrue(conflict.stderr().contains("status=ERROR"));
        assertArrayEquals(manifestBytes, soleManifestBytes(layout.submissionRoot()));
        assertEquals(1L, recoverQueue(layout).revision());
    }

    @Test
    void firstUseWithMismatchedTaskFailsClosed() throws Exception {
        Layout layout = layout();
        writeGovernedProject(layout.projectRoot());
        String targetDigest = writeTarget(layout.projectRoot(), "generated submit target\n");

        Execution mismatched = execute(
                layout, "different-task", "generated-submit-cli", targetDigest);

        assertEquals(2, mismatched.exitCode());
        assertTrue(mismatched.stderr().contains("task-id does not match"));
    }

    private Execution execute(
            Layout layout,
            String taskId,
            String producer,
            String targetDigest) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(
                arguments(layout, taskId, producer, targetDigest),
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Execution(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private String[] arguments(
            Layout layout,
            String taskId,
            String producer,
            String targetDigest) {
        return new String[] {
                "scheduler-submit-generated",
                "--project-root", layout.projectRoot().toString(),
                "--submission-root", layout.submissionRoot().toString(),
                "--queue-root", layout.queueRoot().toString(),
                "--task-id", taskId,
                "--submission-id", SUBMISSION_ID,
                "--max-work-items", "8",
                "--required-capability", "read-file-worker",
                "--producer", producer,
                "--target-path", "target.txt",
                "--expected-sha256", targetDigest
        };
    }

    private String writeGovernedProject(Path projectRoot) throws Exception {
        String taskDocument = activeTask();
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            Files.writeString(
                    path,
                    document == RequiredProjectDocument.CURRENT_TASK
                            ? taskDocument
                            : "# " + document.name() + "\n",
                    StandardCharsets.UTF_8);
        }
        return taskDocument;
    }

    private String activeTask() {
        return "# Current Task\n\n"
                + "## Status\n\nIn Progress\n\n"
                + "## Task\n\nSubmit governed work durably with generated inputs.\n\n"
                + "## Task ID\n\n" + TASK_ID + "\n\n"
                + "## Approval\n\nApproved by the CLI integration-test owner.\n\n"
                + "## Allowed Tools\n\n- read-file\n";
    }

    private String writeTarget(Path projectRoot, String content) throws Exception {
        Files.writeString(projectRoot.resolve("target.txt"), content, StandardCharsets.UTF_8);
        return sha256(content);
    }

    private String sha256(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                content.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] soleManifestBytes(Path submissionRoot) throws Exception {
        try (Stream<Path> files = Files.list(submissionRoot)) {
            Path artifact = files.filter(Files::isRegularFile).findFirst().orElseThrow();
            return Files.readAllBytes(artifact);
        }
    }

    private DurableSingleWorkerSchedulerQueue recoverQueue(Layout layout) throws Exception {
        return DurableSingleWorkerSchedulerQueue.recover(
                IDENTITIES.queueId(),
                new FileSystemSchedulerQueueStore(layout.queueRoot()));
    }

    private Layout layout() {
        return new Layout(
                temporaryRoot.resolve("project"),
                temporaryRoot.resolve("submissions"),
                temporaryRoot.resolve("queue"));
    }

    private record Layout(Path projectRoot, Path submissionRoot, Path queueRoot) {
    }

    private record Execution(int exitCode, String stdout, String stderr) {
    }
}
