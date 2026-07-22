package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.DurableWorkItemAdmissionHandler;
import com.enhancer.runtime.FileSystemPendingFinalizationStore;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.runtime.PendingFinalization;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliSchedulerCycleIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000801";
    private static final String MESSAGE_ID =
            "00000000-0000-0000-0000-000000000802";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000803";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000804";

    @TempDir
    Path temporaryRoot;

    @Test
    void recoversAWorkerPrefixAndCompletesAdmittedWorkInARealChildProcess()
            throws Exception {
        Layout layout = layout("verified");
        String digest = writeTarget(layout.projectRoot(), "scheduler target\n");
        DurableSingleWorkerSchedulerQueue queue = createQueue(layout);
        new DurableWorkItemAdmissionHandler("read-file-worker", queue)
                .handle(workMessage(digest));
        FileSystemPendingFinalizationStore checkpoint =
                new FileSystemPendingFinalizationStore(layout.checkpointRoot());
        checkpoint.record(new PendingFinalization(
                GOAL_ID, AGENT_RUN_ID, Optional.empty()));

        Execution execution = execute(layout, "2");

        assertEquals(0, execution.exitCode());
        assertTrue(execution.stdout().contains("status=VERIFIED_COMPLETED"));
        DurableSingleWorkerSchedulerQueue recovered = recoverQueue(layout);
        assertEquals(1, recovered.completedWorkItemIds().size());
        assertTrue(recovered.failedWorkItemIds().isEmpty());
        assertEquals(1, new FileSystemRunRecordStore(layout.recordRoot())
                .references().size());
        assertTrue(checkpoint.findPending().isEmpty());
    }

    @Test
    void returnsSchedulerFailureAfterTheExplicitAttemptBudgetIsExhausted()
            throws Exception {
        Layout layout = layout("failed");
        writeTarget(layout.projectRoot(), "scheduler target\n");
        DurableSingleWorkerSchedulerQueue queue = createQueue(layout);
        new DurableWorkItemAdmissionHandler("read-file-worker", queue)
                .handle(workMessage("a".repeat(64)));

        Execution execution = execute(layout, "1");

        assertEquals(40, execution.exitCode());
        assertTrue(execution.stdout().contains("status=FAILED"));
        assertEquals(1, recoverQueue(layout).failedWorkItemIds().size());
        assertEquals(1, new FileSystemRunRecordStore(layout.recordRoot())
                .references().size());
        assertTrue(new FileSystemPendingFinalizationStore(layout.checkpointRoot())
                .findPending().isEmpty());
    }

    @Test
    void reportsIdleWithoutLeavingACycleCheckpoint() throws Exception {
        Layout layout = layout("idle");
        Files.createDirectories(layout.projectRoot());
        createQueue(layout);

        Execution execution = execute(layout, "2");

        assertEquals(0, execution.exitCode());
        assertTrue(execution.stdout().contains("status=IDLE"));
        assertEquals(0, recoverQueue(layout).pendingCount());
        assertTrue(new FileSystemPendingFinalizationStore(layout.checkpointRoot())
                .findPending().isEmpty());
    }

    @Test
    void rejectsAMissingQueueAsConfigurationWithoutCreatingIt() throws Exception {
        Layout layout = layout("missing");
        Files.createDirectories(layout.projectRoot());

        Execution execution = execute(layout, "2");

        assertEquals(2, execution.exitCode());
        assertTrue(execution.stderr().contains("status=ERROR"));
        assertTrue(execution.stderr().contains("exitCode=2"));
        assertTrue(Files.notExists(layout.queueRoot()));
    }

    private DurableSingleWorkerSchedulerQueue createQueue(Layout layout) throws Exception {
        return DurableSingleWorkerSchedulerQueue.create(
                QUEUE_ID, 8, new FileSystemSchedulerQueueStore(layout.queueRoot()));
    }

    private DurableSingleWorkerSchedulerQueue recoverQueue(Layout layout) throws Exception {
        return DurableSingleWorkerSchedulerQueue.recover(
                QUEUE_ID, new FileSystemSchedulerQueueStore(layout.queueRoot()));
    }

    private MessageEnvelope workMessage(String expectedDigest) {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                "expose-durable-scheduler-cycle-cli",
                "CURRENT_TASK.md",
                expectedDigest);
        return new MessageEnvelope(
                MESSAGE_ID,
                "scheduler-cycle-correlation",
                Optional.empty(),
                "scheduler-cycle-logical-run",
                "scheduler-cycle-cli-test",
                Instant.parse("2026-07-22T12:00:00Z"),
                new WorkPayload(
                        revision,
                        "b".repeat(64),
                        Set.of("read-file")));
    }

    private String writeTarget(Path projectRoot, String content) throws Exception {
        Files.createDirectories(projectRoot);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        Files.write(projectRoot.resolve("CURRENT_TASK.md"), bytes);
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private Execution execute(Layout layout, String maxAttempts) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(
                arguments(layout, maxAttempts),
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Execution(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private String[] arguments(Layout layout, String maxAttempts) {
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
                "--owner-id", "scheduler-cycle-owner",
                "--max-attempts", maxAttempts,
                "--lease-millis", "300000",
                "--process-timeout-millis", "30000"
        };
    }

    private Layout layout(String name) {
        Path root = temporaryRoot.resolve(name);
        return new Layout(
                root.resolve("project"),
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
