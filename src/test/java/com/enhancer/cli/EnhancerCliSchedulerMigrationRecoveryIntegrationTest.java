package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.PolicyDecision;
import com.enhancer.run.PolicyDecisionStatus;
import com.enhancer.run.RunRecord;
import com.enhancer.runtime.AgentRunDispatch;
import com.enhancer.runtime.DurableAgentRunDispatcher;
import com.enhancer.runtime.DurableAgentRuntime;
import com.enhancer.runtime.DurableExternalEffectLedger;
import com.enhancer.runtime.DurableSingleWorkerSchedulerQueue;
import com.enhancer.runtime.ExternalEffectLedgerState;
import com.enhancer.runtime.FileSystemAgentRuntimeStateStore;
import com.enhancer.runtime.FileSystemExternalEffectLedgerStore;
import com.enhancer.runtime.FileSystemPendingFinalizationStore;
import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.runtime.PendingFinalization;
import com.enhancer.runtime.QueuedWork;
import com.enhancer.runtime.RuntimeAgentRunStatus;
import com.enhancer.runtime.WorkItem;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliSchedulerMigrationRecoveryIntegrationTest {
    private static final int ENVELOPE_MAGIC = 0x50464331;
    private static final int HEADER_BYTES =
            Integer.BYTES + Long.BYTES + Integer.BYTES + 32;
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000831";
    private static final String WORK_ID =
            "00000000-0000-0000-0000-000000000832";
    private static final String MESSAGE_ID =
            "00000000-0000-0000-0000-000000000833";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000834";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000835";
    private static final String OWNER_ID = "migration-recovery-owner";
    private static final String TASK_ID =
            "verify-migrated-checkpoint-scheduler-recovery";
    private static final String EXPECTED_DIGEST = "a".repeat(64);

    @TempDir
    Path temporaryRoot;

    @Test
    void migratedPostRecordCheckpointFinalizesWithoutDuplicateExecutionOrEffect()
            throws Exception {
        Layout layout = layout();
        Clock clock = Clock.systemUTC();
        Files.createDirectories(layout.projectRoot());

        FileSystemSchedulerQueueStore queueStore =
                new FileSystemSchedulerQueueStore(layout.queueRoot());
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID, 8, queueStore);
        WorkItem workItem = workItem();
        queue.enqueue(new QueuedWork(workItem, List.of()));

        FileSystemAgentRuntimeStateStore runtimeStore =
                new FileSystemAgentRuntimeStateStore(layout.runtimeRoot());
        AgentRunDispatch dispatch = new DurableAgentRunDispatcher(
                queue, runtimeStore, clock)
                .claimAndLease(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        OWNER_ID,
                        Duration.ofMinutes(5))
                .orElseThrow();
        assertEquals(AGENT_RUN_ID, dispatch.agentRunId());

        FileSystemRunRecordStore runRecordStore =
                new FileSystemRunRecordStore(layout.recordRoot());
        String reference = runRecordStore.persist(runRecord()).reference();
        assertEquals(List.of(reference), runRecordStore.references());

        FileSystemExternalEffectLedgerStore effectStore =
                new FileSystemExternalEffectLedgerStore(layout.effectRoot());
        DurableExternalEffectLedger.create(
                GOAL_ID, runtimeStore, effectStore, clock);
        ExternalEffectLedgerState effectBefore = effectStore.resolve(GOAL_ID);
        Path effectArtifact = layout.effectRoot()
                .resolve(GOAL_ID + ".external-effects");
        byte[] effectBytesBefore = Files.readAllBytes(effectArtifact);

        writeSchemaV1Checkpoint(layout.checkpointRoot(), reference);

        Captured migration = execute(new String[] {
                "scheduler-migrate-cycle-checkpoint",
                "--cycle-checkpoint-root", layout.checkpointRoot().toString()
        });

        assertEquals(0, migration.exitCode());
        assertTrue(migration.stdout().contains("status=MIGRATED"));
        assertEquals(
                Optional.of(new PendingFinalization(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        Optional.of(reference),
                        Optional.empty())),
                new FileSystemPendingFinalizationStore(
                        layout.checkpointRoot()).findPending());

        Captured cycle = execute(cycleArguments(layout));

        assertEquals(0, cycle.exitCode());
        assertTrue(cycle.stdout().contains("status=VERIFIED_COMPLETED"));
        DurableSingleWorkerSchedulerQueue recovered =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID, queueStore);
        assertEquals(Set.of(WORK_ID), recovered.completedWorkItemIds());
        assertTrue(recovered.failedWorkItemIds().isEmpty());
        assertEquals(List.of(reference), runRecordStore.references());
        assertFalse(Files.exists(layout.invocationRoot()));
        ExternalEffectLedgerState effectAfter =
                effectStore.resolve(GOAL_ID);
        assertEquals(effectBefore.revision(), effectAfter.revision());
        assertEquals(effectBefore.records(), effectAfter.records());
        assertArrayEquals(
                effectBytesBefore, Files.readAllBytes(effectArtifact));
        assertEquals(
                RuntimeAgentRunStatus.COMPLETED,
                DurableAgentRuntime.recover(
                        GOAL_ID, runtimeStore, clock)
                        .agentRun().orElseThrow().status());
        assertTrue(new FileSystemPendingFinalizationStore(
                layout.checkpointRoot()).findPending().isEmpty());
    }

    private WorkItem workItem() {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                TASK_ID, "CURRENT_TASK.md", EXPECTED_DIGEST);
        MessageEnvelope envelope = new MessageEnvelope(
                MESSAGE_ID,
                "migration-recovery-correlation",
                Optional.empty(),
                "migration-recovery-logical-run",
                "migration-recovery-test",
                Instant.parse("2026-07-24T12:00:00Z"),
                new WorkPayload(
                        revision,
                        "b".repeat(64),
                        Set.of("read-file")));
        return new WorkItem(WORK_ID, "read-file-worker", envelope);
    }

    private RunRecord runRecord() {
        ToolResult result = new ToolResult(
                "read-file",
                ToolResultStatus.SUCCESS,
                OptionalInt.empty(),
                VerificationEvidence.capture(
                        "read succeeded", "content", Optional.empty()));
        return new RunRecord(
                "migration-recovery-logical-run",
                Instant.parse("2026-07-24T12:01:00Z"),
                new ApprovedTask(
                        TASK_ID,
                        "Verify migrated Scheduler recovery",
                        "Approved by the active task",
                        Set.of("read-file"),
                        "CURRENT_TASK.md"),
                new ToolRequest(
                        "read-file",
                        "migration-recovery-correlation",
                        Map.of("path", "CURRENT_TASK.md")),
                new PolicyDecision(
                        PolicyDecisionStatus.ALLOWED,
                        temporaryRoot.toString(),
                        Set.of("read-file"),
                        Set.of(),
                        4096,
                        1000),
                result,
                Optional.of(EXPECTED_DIGEST),
                VerificationDecision.verified("content matched"),
                1,
                AgentLoopStopReason.AWAITING_VERIFICATION,
                AgentLoopStopReason.COMPLETED);
    }

    private static void writeSchemaV1Checkpoint(
            Path checkpointRoot,
            String reference) throws Exception {
        ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
        try (DataOutputStream output =
                new DataOutputStream(payloadBytes)) {
            output.writeInt(1);
            writeString(output, "pending-finalization");
            writeString(output, GOAL_ID);
            writeString(output, AGENT_RUN_ID);
            output.writeBoolean(true);
            writeString(output, reference);
        }
        byte[] payload = payloadBytes.toByteArray();
        long storedAtMillis = 3000L;
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                ByteBuffer.allocate(
                                Integer.BYTES
                                        + Long.BYTES
                                        + Integer.BYTES
                                        + payload.length)
                        .putInt(ENVELOPE_MAGIC)
                        .putLong(storedAtMillis)
                        .putInt(payload.length)
                        .put(payload)
                        .array());
        byte[] envelope = ByteBuffer.allocate(HEADER_BYTES + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAtMillis)
                .putInt(payload.length)
                .put(digest)
                .put(payload)
                .array();
        Files.createDirectories(checkpointRoot);
        Files.write(checkpointRoot.resolve("pending.finalization"), envelope);
    }

    private static void writeString(
            DataOutputStream output,
            String value) throws Exception {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(encoded.length);
        output.write(encoded);
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

    private static String[] cycleArguments(Layout layout) {
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
                "--owner-id", OWNER_ID,
                "--max-attempts", "1",
                "--lease-millis", "300000",
                "--process-timeout-millis", "30000"
        };
    }

    private Layout layout() {
        return new Layout(
                temporaryRoot.resolve("project"),
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
            Path queueRoot,
            Path runtimeRoot,
            Path effectRoot,
            Path checkpointRoot,
            Path evidenceRoot,
            Path recordRoot,
            Path invocationRoot) {
    }

    private record Captured(
            int exitCode,
            String stdout,
            String stderr) {
    }
}
