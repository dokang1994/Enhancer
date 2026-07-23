package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.FileSystemPendingFinalizationStore;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.runtime.PendingFinalization;
import com.enhancer.runtime.QueuedWork;
import com.enhancer.runtime.WorkItem;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliSchedulerDrainIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000f01";
    private static final String FIRST_WORK_ID =
            "00000000-0000-0000-0000-000000000f11";
    private static final String SECOND_WORK_ID =
            "00000000-0000-0000-0000-000000000f12";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000f21";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000f22";

    @TempDir
    Path temporaryRoot;

    @Test
    void recoversACheckpointAndDrainsDependencyLinkedWorkUntilIdle()
            throws Exception {
        Layout layout = layout("dependency-recovery");
        String digest = writeTarget(layout.projectRoot(), "drain target\n");
        DurableSingleWorkerSchedulerQueue queue = createQueue(layout);
        queue.enqueue(new QueuedWork(
                workItem(FIRST_WORK_ID, 1, digest), List.of()));
        queue.enqueue(new QueuedWork(
                workItem(SECOND_WORK_ID, 2, digest), List.of(FIRST_WORK_ID)));
        FileSystemPendingFinalizationStore checkpoint =
                new FileSystemPendingFinalizationStore(layout.checkpointRoot());
        checkpoint.record(new PendingFinalization(
                GOAL_ID, AGENT_RUN_ID, Optional.empty()));

        Execution execution = execute(layout, "8", "2");

        assertEquals(0, execution.exitCode());
        assertTrue(execution.stdout().contains("status=IDLE"));
        assertTrue(execution.stdout().contains("cyclesInvoked=3"));
        assertTrue(execution.stdout().contains("verifiedCompletedCycles=2"));
        assertTrue(execution.stdout().contains("failedCycles=0"));
        DurableSingleWorkerSchedulerQueue recovered = recoverQueue(layout);
        assertEquals(2, recovered.completedWorkItemIds().size());
        assertEquals(0, recovered.pendingCount());
        assertEquals(2, new FileSystemRunRecordStore(layout.recordRoot())
                .references().size());
        assertTrue(checkpoint.findPending().isEmpty());
    }

    @Test
    void stopsAtTheLimitWithoutClaimingTheQueueIsEmpty() throws Exception {
        Layout layout = layout("limit");
        String digest = writeTarget(layout.projectRoot(), "drain target\n");
        DurableSingleWorkerSchedulerQueue queue = createQueue(layout);
        queue.enqueue(new QueuedWork(
                workItem(FIRST_WORK_ID, 1, digest), List.of()));
        queue.enqueue(new QueuedWork(
                workItem(SECOND_WORK_ID, 2, digest), List.of()));

        Execution execution = execute(layout, "1", "2");

        assertEquals(0, execution.exitCode());
        assertTrue(execution.stdout().contains("status=LIMIT_REACHED"));
        assertTrue(execution.stdout().contains("cyclesInvoked=1"));
        assertTrue(execution.stdout().contains("verifiedCompletedCycles=1"));
        DurableSingleWorkerSchedulerQueue recovered = recoverQueue(layout);
        assertEquals(1, recovered.completedWorkItemIds().size());
        assertEquals(1, recovered.pendingCount());
    }

    @Test
    void stopsAtTheFirstTerminalFailureAndLeavesLaterWorkPending()
            throws Exception {
        Layout layout = layout("failure");
        writeTarget(layout.projectRoot(), "drain target\n");
        DurableSingleWorkerSchedulerQueue queue = createQueue(layout);
        queue.enqueue(new QueuedWork(
                workItem(FIRST_WORK_ID, 1, "a".repeat(64)), List.of()));
        queue.enqueue(new QueuedWork(
                workItem(SECOND_WORK_ID, 2, "a".repeat(64)), List.of()));

        Execution execution = execute(layout, "8", "1");

        assertEquals(40, execution.exitCode());
        assertTrue(execution.stdout().contains("status=FAILED"));
        assertTrue(execution.stdout().contains("cyclesInvoked=1"));
        assertTrue(execution.stdout().contains("verifiedCompletedCycles=0"));
        assertTrue(execution.stdout().contains("failedCycles=1"));
        DurableSingleWorkerSchedulerQueue recovered = recoverQueue(layout);
        assertEquals(1, recovered.failedWorkItemIds().size());
        assertEquals(1, recovered.pendingCount());
    }

    @Test
    void rejectsAMissingQueueWithoutCreatingIt() throws Exception {
        Layout layout = layout("missing");
        Files.createDirectories(layout.projectRoot());

        Execution execution = execute(layout, "8", "2");

        assertEquals(2, execution.exitCode());
        assertTrue(execution.stderr().contains("status=ERROR"));
        assertTrue(execution.stderr().contains("exitCode=2"));
        assertTrue(Files.notExists(layout.queueRoot()));
    }

    private DurableSingleWorkerSchedulerQueue createQueue(Layout layout)
            throws Exception {
        return DurableSingleWorkerSchedulerQueue.create(
                QUEUE_ID,
                8,
                new FileSystemSchedulerQueueStore(layout.queueRoot()));
    }

    private DurableSingleWorkerSchedulerQueue recoverQueue(Layout layout)
            throws Exception {
        return DurableSingleWorkerSchedulerQueue.recover(
                QUEUE_ID,
                new FileSystemSchedulerQueueStore(layout.queueRoot()));
    }

    private WorkItem workItem(
            String workItemId,
            long identitySuffix,
            String expectedDigest) {
        MessageEnvelope message = new MessageEnvelope(
                String.format(
                        "00000000-0000-0000-0001-%012d",
                        identitySuffix),
                "scheduler-drain-correlation",
                Optional.empty(),
                "scheduler-drain-logical-run",
                "scheduler-drain-cli-test",
                Instant.parse("2026-07-23T03:00:00Z").plusSeconds(identitySuffix),
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "gate-8-bounded-foreground-scheduler-drain",
                                "CURRENT_TASK.md",
                                "b".repeat(64)),
                        "c".repeat(64),
                        Set.of("read-file"),
                        Optional.of(new WorkPayload.ExecutionInput(
                                "CURRENT_TASK.md",
                                expectedDigest))));
        return new WorkItem(workItemId, "read-file-worker", message);
    }

    private String writeTarget(Path projectRoot, String content) throws Exception {
        Files.createDirectories(projectRoot);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        Files.write(projectRoot.resolve("CURRENT_TASK.md"), bytes);
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private Execution execute(
            Layout layout,
            String maxCycles,
            String maxAttempts) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(
                arguments(layout, maxCycles, maxAttempts),
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Execution(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private String[] arguments(
            Layout layout,
            String maxCycles,
            String maxAttempts) {
        return new String[] {
                "scheduler-drain",
                "--project-root", layout.projectRoot().toString(),
                "--queue-root", layout.queueRoot().toString(),
                "--queue-id", QUEUE_ID,
                "--runtime-root", layout.runtimeRoot().toString(),
                "--external-effect-root", layout.effectRoot().toString(),
                "--cycle-checkpoint-root", layout.checkpointRoot().toString(),
                "--evidence-root", layout.evidenceRoot().toString(),
                "--run-record-root", layout.recordRoot().toString(),
                "--invocation-root", layout.invocationRoot().toString(),
                "--owner-id", "scheduler-drain-owner",
                "--max-attempts", maxAttempts,
                "--lease-millis", "300000",
                "--process-timeout-millis", "30000",
                "--max-cycles", maxCycles
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
