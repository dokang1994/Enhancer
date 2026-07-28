package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.runtime.AgentRunDispatch;
import com.enhancer.runtime.DurableAgentRunDispatcher;
import com.enhancer.runtime.DurableAgentRuntime;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.FileSystemAgentRuntimeStateStore;
import com.enhancer.runtime.FileSystemPendingFinalizationStore;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.runtime.PendingFinalization;
import com.enhancer.runtime.QueuedWork;
import com.enhancer.runtime.RuntimeAgentRunStatus;
import com.enhancer.runtime.WorkItem;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliSchedulerServiceIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000001001";
    private static final String WORK_ID =
            "00000000-0000-0000-0000-000000001011";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000001021";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000001022";

    @TempDir
    Path temporaryRoot;

    @Test
    void stopsAnEmptyServiceAtTheConsecutiveIdleLimit() throws Exception {
        Layout layout = layout("empty");
        Files.createDirectories(layout.projectRoot());
        long initialRevision = createQueue(layout).revision();

        Execution execution = execute(layout, "8", "2", "1");

        assertEquals(0, execution.exitCode());
        assertTrue(execution.stdout().contains("status=IDLE_LIMIT"));
        assertTrue(execution.stdout().contains("cyclesInvoked=2"));
        assertTrue(execution.stdout().contains("verifiedCompletedCycles=0"));
        assertTrue(execution.stdout().contains("idleCycles=2"));
        assertTrue(execution.stdout().contains("failedCycles=0"));
        assertEquals(initialRevision, recoverQueue(layout).revision());
    }

    @Test
    void resumesAPersistedCycleIntentThenStopsAtIdle() throws Exception {
        Layout layout = layout("checkpoint-restart");
        String digest = writeTarget(layout.projectRoot(), "service target\n");
        createQueue(layout).enqueue(new QueuedWork(workItem(digest), List.of()));
        FileSystemPendingFinalizationStore checkpoint =
                new FileSystemPendingFinalizationStore(layout.checkpointRoot());
        checkpoint.record(new PendingFinalization(
                GOAL_ID, AGENT_RUN_ID, Optional.empty()));

        Execution execution = execute(layout, "8", "1", "1");

        assertEquals(0, execution.exitCode());
        assertTrue(execution.stdout().contains("status=IDLE_LIMIT"));
        assertTrue(execution.stdout().contains("cyclesInvoked=2"));
        assertTrue(execution.stdout().contains("verifiedCompletedCycles=1"));
        assertTrue(execution.stdout().contains("idleCycles=1"));
        assertEquals(Set.of(WORK_ID), recoverQueue(layout).completedWorkItemIds());
        assertEquals(1, new FileSystemRunRecordStore(layout.recordRoot())
                .references().size());
        assertTrue(checkpoint.findPending().isEmpty());
        DurableAgentRuntime runtime = DurableAgentRuntime.recover(
                GOAL_ID,
                new FileSystemAgentRuntimeStateStore(layout.runtimeRoot()),
                Clock.systemUTC());
        assertEquals(1, runtime.agentRuns().size());
        assertEquals(RuntimeAgentRunStatus.COMPLETED,
                runtime.agentRun().orElseThrow().status());
    }

    @Test
    void reclaimsAnExpiredExecutionLeaseWithTheSameAttemptAndGreaterFence()
            throws Exception {
        Layout layout = layout("expired-lease");
        String digest = writeTarget(layout.projectRoot(), "service target\n");
        DurableSingleWorkerSchedulerQueue queue = createQueue(layout);
        queue.enqueue(new QueuedWork(workItem(digest), List.of()));
        FileSystemPendingFinalizationStore checkpoint =
                new FileSystemPendingFinalizationStore(layout.checkpointRoot());
        checkpoint.record(new PendingFinalization(
                GOAL_ID, AGENT_RUN_ID, Optional.empty()));
        FileSystemAgentRuntimeStateStore runtimeStore =
                new FileSystemAgentRuntimeStateStore(layout.runtimeRoot());
        AgentRunDispatch interrupted = new DurableAgentRunDispatcher(
                queue,
                runtimeStore,
                Clock.systemUTC()).claimAndLease(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        "stopped-owner",
                        Duration.ofMillis(1)).orElseThrow();
        while (!Instant.now().isAfter(interrupted.lease().expiresAt())) {
            Thread.onSpinWait();
        }

        Execution execution = execute(layout, "8", "1", "1");

        assertEquals(0, execution.exitCode());
        assertTrue(execution.stdout().contains("status=IDLE_LIMIT"));
        assertTrue(execution.stdout().contains("verifiedCompletedCycles=1"));
        assertEquals(Set.of(WORK_ID), recoverQueue(layout).completedWorkItemIds());
        DurableAgentRuntime runtime = DurableAgentRuntime.recover(
                GOAL_ID, runtimeStore, Clock.systemUTC());
        assertEquals(1, runtime.agentRuns().size());
        assertEquals(RuntimeAgentRunStatus.COMPLETED,
                runtime.agentRun().orElseThrow().status());
        assertEquals(
                interrupted.lease().fenceToken() + 1,
                runtime.lastIssuedFenceToken());
        assertEquals(1, new FileSystemRunRecordStore(layout.recordRoot())
                .references().size());
        assertTrue(checkpoint.findPending().isEmpty());
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

    private WorkItem workItem(String expectedDigest) {
        return new WorkItem(
                WORK_ID,
                "read-file-worker",
                new MessageEnvelope(
                        "00000000-0000-0000-0000-000000001012",
                        "scheduler-service-correlation",
                        Optional.empty(),
                        "scheduler-service-logical-run",
                        "scheduler-service-cli-test",
                        Instant.parse("2026-07-28T04:00:00Z"),
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "connect-bounded-scheduler-service-cli",
                                        "CURRENT_TASK.md",
                                        "b".repeat(64)),
                                "c".repeat(64),
                                Set.of("read-file"),
                                Optional.of(new WorkPayload.ExecutionInput(
                                        "CURRENT_TASK.md",
                                        expectedDigest)))));
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
            String maxConsecutiveIdleCycles,
            String idleWaitMillis) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(
                arguments(
                        layout,
                        maxCycles,
                        maxConsecutiveIdleCycles,
                        idleWaitMillis),
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
            String maxConsecutiveIdleCycles,
            String idleWaitMillis) {
        return new String[] {
                "scheduler-service",
                "--project-root", layout.projectRoot().toString(),
                "--queue-root", layout.queueRoot().toString(),
                "--queue-id", QUEUE_ID,
                "--runtime-root", layout.runtimeRoot().toString(),
                "--external-effect-root", layout.effectRoot().toString(),
                "--cycle-checkpoint-root", layout.checkpointRoot().toString(),
                "--evidence-root", layout.evidenceRoot().toString(),
                "--run-record-root", layout.recordRoot().toString(),
                "--invocation-root", layout.invocationRoot().toString(),
                "--owner-id", "scheduler-service-owner",
                "--max-attempts", "2",
                "--lease-millis", "300000",
                "--process-timeout-millis", "30000",
                "--max-cycles", maxCycles,
                "--max-consecutive-idle-cycles", maxConsecutiveIdleCycles,
                "--idle-wait-millis", idleWaitMillis
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
