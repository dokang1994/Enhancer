package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.FileSystemPendingFinalizationStore;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
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

class EnhancerCliSchedulerOperatorWorkflowIntegrationTest {
    private static final String TASK_ID =
            "scheduler-operator-workflow-test";
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000e01";
    private static final String MESSAGE_ID =
            "00000000-0000-0000-0000-000000000e02";
    private static final String TARGET_CONTENT =
            "explicit operator workflow target\n";

    @TempDir
    Path temporaryRoot;

    @Test
    void submitsThenSeparatelyRunsOneRecoverableCycleWithoutDuplicateExecution()
            throws Exception {
        Layout layout = layout();
        writeGovernedProject(layout.projectRoot());
        String digest = writeTarget(layout.projectRoot(), TARGET_CONTENT);

        Execution submitted = execute(submitArguments(layout, digest));

        assertEquals(0, submitted.exitCode());
        assertTrue(submitted.stdout().contains("status=ADMITTED"));
        assertTrue(submitted.stdout().contains("workAdmitted=true"));
        byte[] manifestBytes = soleManifestBytes(layout.submissionRoot());
        DurableSingleWorkerSchedulerQueue pending = recoverQueue(layout);
        assertEquals(1L, pending.revision());
        assertEquals(1, pending.pendingCount());
        assertTrue(pending.completedWorkItemIds().isEmpty());
        assertTrue(Files.notExists(layout.recordRoot()));
        assertTrue(Files.notExists(layout.runtimeRoot()));
        assertTrue(Files.notExists(layout.checkpointRoot()));

        Execution completed = execute(cycleArguments(layout));

        assertEquals(0, completed.exitCode());
        assertTrue(completed.stdout().contains("status=VERIFIED_COMPLETED"));
        DurableSingleWorkerSchedulerQueue terminal = recoverQueue(layout);
        assertEquals(1, terminal.completedWorkItemIds().size());
        assertTrue(terminal.failedWorkItemIds().isEmpty());
        assertEquals(0, terminal.pendingCount());
        long terminalRevision = terminal.revision();
        assertEquals(1, runRecordCount(layout));
        assertTrue(regularFileCount(layout.runtimeRoot()) > 0);
        assertTrue(regularFileCount(layout.effectRoot()) > 0);
        FileSystemRunRecordStore records =
                new FileSystemRunRecordStore(layout.recordRoot());
        String reference = records.references().get(0);
        assertEquals(
                TARGET_CONTENT,
                records.resolve(reference).record().toolResult().evidence().outputTail());
        assertTrue(records.resolve(reference).record().toolResult().evidence()
                .fullOutputReference().isEmpty());
        assertEquals(0, regularFileCount(layout.evidenceRoot()));
        assertEquals(0, regularFileCount(layout.invocationRoot()));
        assertTrue(new FileSystemPendingFinalizationStore(layout.checkpointRoot())
                .findPending().isEmpty());
        assertArrayEquals(manifestBytes, soleManifestBytes(layout.submissionRoot()));

        Execution replayedSubmission = execute(submitArguments(layout, digest));

        assertEquals(0, replayedSubmission.exitCode());
        assertTrue(replayedSubmission.stdout().contains("status=REPLAYED"));
        assertTrue(replayedSubmission.stdout().contains("workAdmitted=false"));
        assertEquals(terminalRevision, recoverQueue(layout).revision());
        assertEquals(1, runRecordCount(layout));
        assertArrayEquals(manifestBytes, soleManifestBytes(layout.submissionRoot()));

        Execution idle = execute(cycleArguments(layout));

        assertEquals(0, idle.exitCode());
        assertTrue(idle.stdout().contains("status=IDLE"));
        assertEquals(terminalRevision, recoverQueue(layout).revision());
        assertEquals(1, runRecordCount(layout));
        assertTrue(new FileSystemPendingFinalizationStore(layout.checkpointRoot())
                .findPending().isEmpty());
    }

    private Execution execute(String[] arguments) {
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

    private String[] submitArguments(Layout layout, String digest) {
        return new String[] {
                "scheduler-submit",
                "--project-root", layout.projectRoot().toString(),
                "--submission-root", layout.submissionRoot().toString(),
                "--queue-root", layout.queueRoot().toString(),
                "--task-id", TASK_ID,
                "--queue-id", QUEUE_ID,
                "--max-work-items", "8",
                "--required-capability", "read-file-worker",
                "--message-id", MESSAGE_ID,
                "--correlation-id", "scheduler-operator-correlation",
                "--logical-run-id", "scheduler-operator-logical-run",
                "--producer", "scheduler-operator-test",
                "--occurred-at", "2026-07-22T17:00:00Z",
                "--target-path", "target.txt",
                "--expected-sha256", digest
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
                "--owner-id", "scheduler-operator-owner",
                "--max-attempts", "2",
                "--lease-millis", "300000",
                "--process-timeout-millis", "30000"
        };
    }

    private void writeGovernedProject(Path projectRoot) throws Exception {
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            Files.writeString(
                    path,
                    document == RequiredProjectDocument.CURRENT_TASK
                            ? activeTask()
                            : "# " + document.name() + "\n",
                    StandardCharsets.UTF_8);
        }
    }

    private String activeTask() {
        return "# Current Task\n\n"
                + "## Status\n\nIn Progress\n\n"
                + "## Task\n\nProve the explicit Scheduler operator workflow.\n\n"
                + "## Task ID\n\n" + TASK_ID + "\n\n"
                + "## Approval\n\nApproved by the integration-test owner.\n\n"
                + "## Allowed Tools\n\n- read-file\n";
    }

    private String writeTarget(Path projectRoot, String content) throws Exception {
        Files.writeString(
                projectRoot.resolve("target.txt"),
                content,
                StandardCharsets.UTF_8);
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                content.getBytes(StandardCharsets.UTF_8)));
    }

    private DurableSingleWorkerSchedulerQueue recoverQueue(Layout layout)
            throws Exception {
        return DurableSingleWorkerSchedulerQueue.recover(
                QUEUE_ID,
                new FileSystemSchedulerQueueStore(layout.queueRoot()));
    }

    private int runRecordCount(Layout layout) throws Exception {
        return new FileSystemRunRecordStore(layout.recordRoot()).references().size();
    }

    private long regularFileCount(Path root) throws Exception {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile).count();
        }
    }

    private byte[] soleManifestBytes(Path submissionRoot) throws Exception {
        try (Stream<Path> files = Files.list(submissionRoot)) {
            Path artifact = files.filter(Files::isRegularFile).findFirst().orElseThrow();
            return Files.readAllBytes(artifact);
        }
    }

    private Layout layout() {
        return new Layout(
                temporaryRoot.resolve("project"),
                temporaryRoot.resolve("submissions"),
                temporaryRoot.resolve("queue"),
                temporaryRoot.resolve("runtime"),
                temporaryRoot.resolve("effects"),
                temporaryRoot.resolve("checkpoint"),
                temporaryRoot.resolve("evidence"),
                temporaryRoot.resolve("records"),
                temporaryRoot.resolve("invocations"));
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
