package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationCode;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.PolicyDecision;
import com.enhancer.run.PolicyDecisionStatus;
import com.enhancer.run.RunRecord;
import com.enhancer.runtime.DurableAgentRuntime;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.FileSystemAgentRuntimeStateStore;
import com.enhancer.runtime.FileSystemPendingFinalizationStore;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.runtime.PendingFinalization;
import com.enhancer.runtime.QueuedWork;
import com.enhancer.runtime.WorkItem;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliSchedulerRecoveryStatusIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000971";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000972";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000973";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000974";
    private static final Instant NOW =
            Instant.parse("2026-07-23T08:00:00Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void reportsNoPendingCycleWithoutCreatingOrChangingAnyOtherStore()
            throws Exception {
        Roots roots = roots("none");
        DurableSingleWorkerSchedulerQueue.create(
                QUEUE_ID,
                4,
                new FileSystemSchedulerQueueStore(roots.queue()));
        Path queueArtifact = queueArtifact(roots);
        byte[] queueBefore = Files.readAllBytes(queueArtifact);

        Captured result = execute(roots);

        assertEquals(0, result.exitCode());
        assertEquals("NO_PENDING_CYCLE",
                value(result.stdout(), "status"));
        assertEquals("0", value(result.stdout(), "exitCode"));
        assertEquals(QUEUE_ID, value(result.stdout(), "queueId"));
        assertEquals("false",
                value(result.stdout(), "checkpointPresent"));
        assertEquals("UNKNOWN",
                value(result.stdout(), "workerLiveness"));
        assertEquals("", result.stderr());
        assertArrayEquals(queueBefore, Files.readAllBytes(queueArtifact));
        assertFalse(Files.exists(roots.runtime()));
        assertFalse(Files.exists(roots.checkpoint()));
        assertFalse(Files.exists(roots.records()));
    }

    @Test
    void reportsIntentAndRuntimePrefixesWithoutMutatingArtifacts()
            throws Exception {
        Roots roots = roots("prefixes");
        WorkItem workItem = workItem();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        4,
                        new FileSystemSchedulerQueueStore(roots.queue()));
        queue.enqueue(new QueuedWork(workItem, List.of()));
        FileSystemPendingFinalizationStore checkpointStore =
                new FileSystemPendingFinalizationStore(
                        roots.checkpoint());
        checkpointStore.record(new PendingFinalization(
                GOAL_ID, AGENT_RUN_ID, Optional.empty()));

        Captured intent = execute(roots);

        assertEquals(0, intent.exitCode());
        assertEquals("INTENT_RECORDED",
                value(intent.stdout(), "status"));
        assertEquals(GOAL_ID, value(intent.stdout(), "goalId"));
        assertEquals(AGENT_RUN_ID,
                value(intent.stdout(), "agentRunId"));
        assertFalse(Files.exists(roots.runtime()));
        assertFalse(Files.exists(roots.records()));

        DurableAgentRuntime.create(
                GOAL_ID,
                workItem,
                new FileSystemAgentRuntimeStateStore(roots.runtime()),
                Clock.fixed(NOW, ZoneOffset.UTC));
        byte[] queueBefore = Files.readAllBytes(queueArtifact(roots));
        byte[] checkpointBefore = Files.readAllBytes(
                roots.checkpoint().resolve("pending.finalization"));
        byte[] runtimeBefore = Files.readAllBytes(
                roots.runtime().resolve(GOAL_ID + ".agent-runtime"));

        Captured runtime = execute(roots);

        assertEquals(0, runtime.exitCode());
        assertEquals("RUNTIME_RECORDED",
                value(runtime.stdout(), "status"));
        assertEquals("ACCEPTED",
                value(runtime.stdout(), "goalStatus"));
        assertEquals("READY",
                value(runtime.stdout(), "queueWorkState"));
        assertEquals("UNKNOWN",
                value(runtime.stdout(), "workerLiveness"));
        assertArrayEquals(queueBefore,
                Files.readAllBytes(queueArtifact(roots)));
        assertArrayEquals(checkpointBefore, Files.readAllBytes(
                roots.checkpoint().resolve("pending.finalization")));
        assertArrayEquals(runtimeBefore, Files.readAllBytes(
                roots.runtime().resolve(GOAL_ID + ".agent-runtime")));
        assertFalse(Files.exists(roots.records()));
    }

    @Test
    void reportsAResolvedRunRecordPrefixFromRealStores()
            throws Exception {
        Roots roots = roots("run-record");
        WorkItem workItem = workItem();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        4,
                        new FileSystemSchedulerQueueStore(roots.queue()));
        queue.enqueue(new QueuedWork(workItem, List.of()));
        queue.claimNext();
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID,
                workItem,
                new FileSystemAgentRuntimeStateStore(roots.runtime()),
                Clock.fixed(NOW, ZoneOffset.UTC));
        runtime.beginAgentRun(AGENT_RUN_ID);
        runtime.markReady(AGENT_RUN_ID);
        runtime.acquireLease(
                AGENT_RUN_ID,
                "owner",
                Duration.ofMinutes(5));
        String reference = new FileSystemRunRecordStore(
                roots.records()).persist(runRecord()).reference();
        new FileSystemPendingFinalizationStore(
                roots.checkpoint()).record(new PendingFinalization(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        Optional.of(reference)));

        Captured result = execute(roots);

        assertEquals(0, result.exitCode());
        assertEquals("RUN_RECORD_RECORDED",
                value(result.stdout(), "status"));
        assertEquals("ACTIVE",
                value(result.stdout(), "queueWorkState"));
        assertEquals("EXECUTING",
                value(result.stdout(), "agentRunStatus"));
        assertEquals(reference,
                value(result.stdout(), "runRecordReference"));
        assertEquals("REJECTED",
                value(result.stdout(),
                        "runRecordVerificationStatus"));
        assertTrue(result.stdout().length()
                <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
        assertEquals("", result.stderr());
    }

    @Test
    void missingQueueIsConfigurationFailureAndCreatesNoRoot() {
        Roots roots = roots("missing");

        Captured result = execute(roots);

        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("exitCode=2"));
        assertFalse(Files.exists(roots.queue()));
        assertFalse(Files.exists(roots.runtime()));
        assertFalse(Files.exists(roots.checkpoint()));
        assertFalse(Files.exists(roots.records()));
    }

    @Test
    void corruptCheckpointFailsClosedWithoutBeingRewritten()
            throws Exception {
        Roots roots = roots("corrupt");
        DurableSingleWorkerSchedulerQueue.create(
                QUEUE_ID,
                4,
                new FileSystemSchedulerQueueStore(roots.queue()));
        FileSystemPendingFinalizationStore checkpoint =
                new FileSystemPendingFinalizationStore(
                        roots.checkpoint());
        checkpoint.record(new PendingFinalization(
                GOAL_ID, AGENT_RUN_ID, Optional.empty()));
        Path artifact =
                roots.checkpoint().resolve("pending.finalization");
        byte[] corrupt = new byte[] {1, 2, 3, 4};
        Files.write(artifact, corrupt);

        Captured result = execute(roots);

        assertEquals(70, result.exitCode());
        assertTrue(result.stderr().contains("exitCode=70"));
        assertArrayEquals(corrupt, Files.readAllBytes(artifact));
        assertFalse(Files.exists(roots.runtime()));
        assertFalse(Files.exists(roots.records()));
    }

    private Captured execute(Roots roots) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(
                new String[] {
                        "scheduler-recovery-status",
                        "--queue-root", roots.queue().toString(),
                        "--queue-id", QUEUE_ID,
                        "--runtime-root", roots.runtime().toString(),
                        "--cycle-checkpoint-root",
                        roots.checkpoint().toString(),
                        "--run-record-root", roots.records().toString()
                },
                new PrintStream(
                        stdout, true, StandardCharsets.UTF_8),
                new PrintStream(
                        stderr, true, StandardCharsets.UTF_8));
        return new Captured(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private Roots roots(String name) {
        Path root = temporaryRoot.resolve(name);
        return new Roots(
                root.resolve("queue"),
                root.resolve("runtime"),
                root.resolve("checkpoint"),
                root.resolve("records"));
    }

    private Path queueArtifact(Roots roots) {
        return roots.queue().resolve(
                QUEUE_ID + ".scheduler-queue");
    }

    private WorkItem workItem() {
        MessageEnvelope envelope = new MessageEnvelope(
                "00000000-0000-0000-0000-000000000975",
                "scheduler-recovery-cli-correlation",
                Optional.empty(),
                "scheduler-recovery-cli-run",
                "scheduler-recovery-cli-test",
                NOW,
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "scheduler-recovery-cli-test",
                                "CURRENT_TASK.md",
                                "a".repeat(64)),
                        "b".repeat(64),
                        Set.of("read-file")));
        return new WorkItem(
                WORK_ITEM_ID, "read-file-worker", envelope);
    }

    private RunRecord runRecord() {
        return new RunRecord(
                "evidence-run",
                NOW,
                new ApprovedTask(
                        "scheduler-recovery-cli-test",
                        "Inspect Scheduler recovery",
                        "Approved by test",
                        Set.of("read-file"),
                        "CURRENT_TASK.md"),
                new ToolRequest(
                        "read-file",
                        "evidence-run",
                        Map.of("path", "CURRENT_TASK.md")),
                new PolicyDecision(
                        PolicyDecisionStatus.ALLOWED,
                        "C:/project",
                        Set.of("read-file"),
                        Set.of(),
                        4096,
                        1000),
                new ToolResult(
                        "read-file",
                        ToolResultStatus.SUCCESS,
                        OptionalInt.empty(),
                        VerificationEvidence.capture(
                                "read succeeded",
                                "content",
                                Optional.empty())),
                Optional.of("a".repeat(64)),
                VerificationDecision.rejected(
                        VerificationCode.CONTENT_MISMATCH,
                        "content differed"),
                1,
                AgentLoopStopReason.AWAITING_VERIFICATION,
                AgentLoopStopReason.AWAITING_VERIFICATION);
    }

    private String value(String output, String key) {
        String prefix = key + "=";
        return output.lines()
                .filter(line -> line.startsWith(prefix))
                .map(line -> line.substring(prefix.length()))
                .findFirst()
                .orElseThrow();
    }

    private record Captured(
            int exitCode,
            String stdout,
            String stderr) {
    }

    private record Roots(
            Path queue,
            Path runtime,
            Path checkpoint,
            Path records) {
    }
}
