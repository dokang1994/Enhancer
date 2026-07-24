package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.BackpressurePolicy;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ResultPayload;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.RunRecordStore;
import com.enhancer.tool.EvidenceStoragePolicy;
import com.enhancer.tool.FileSystemEvidenceStore;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SchedulerInvocationRecoveryStatusReaderTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000b01";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000000b02";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000b03";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000b04";
    private static final Instant NOW =
            Instant.parse("2026-07-24T10:00:00Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void reportsAbsentInvocationWithoutCreatingItsRoot() throws Exception {
        Path queueRoot = temporaryRoot.resolve("queue");
        Path runtimeRoot = temporaryRoot.resolve("runtime");
        Path checkpointRoot = temporaryRoot.resolve("checkpoint");
        Path recordRoot = temporaryRoot.resolve("records");
        Path invocationRoot = temporaryRoot.resolve("invocations");
        WorkItem work = workItem();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        16,
                        new FileSystemSchedulerQueueStore(queueRoot));
        queue.enqueue(new QueuedWork(work, List.of()));
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID,
                work,
                new FileSystemAgentRuntimeStateStore(runtimeRoot),
                Clock.fixed(NOW, ZoneOffset.UTC));
        runtime.beginAgentRun(AGENT_RUN_ID);
        new FileSystemPendingFinalizationStore(checkpointRoot).record(
                new PendingFinalization(
                        GOAL_ID, AGENT_RUN_ID, Optional.empty()));

        SchedulerInvocationRecoveryStatus status =
                new SchedulerInvocationRecoveryStatusReader(
                        new SchedulerRecoveryStatusReader(
                                new FileSystemSchedulerQueueStore(queueRoot),
                                new FileSystemAgentRuntimeStateStore(runtimeRoot),
                                new FileSystemPendingFinalizationStore(
                                        checkpointRoot),
                                new FileSystemRunRecordStore(recordRoot)),
                        new FileSystemAgentRuntimeStateStore(runtimeRoot),
                        new FileSystemRunRecordStore(recordRoot),
                        invocationRoot)
                        .read(QUEUE_ID);

        assertEquals(
                SchedulerInvocationRecoveryStatus.RecoveryPhase
                        .INVOCATION_ABSENT,
                status.phase());
        assertFalse(java.nio.file.Files.exists(invocationRoot));
    }

    @Test
    void reportsAValidatedWorkMessageAwaitingAResult() throws Exception {
        Path queueRoot = temporaryRoot.resolve("work-queue");
        Path runtimeRoot = temporaryRoot.resolve("work-runtime");
        Path checkpointRoot = temporaryRoot.resolve("work-checkpoint");
        Path recordRoot = temporaryRoot.resolve("work-records");
        Path invocationRoot = temporaryRoot.resolve("work-invocations");
        WorkItem work = workItem();
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        16,
                        new FileSystemSchedulerQueueStore(queueRoot));
        queue.enqueue(new QueuedWork(work, List.of()));
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID,
                work,
                new FileSystemAgentRuntimeStateStore(runtimeRoot),
                Clock.fixed(NOW, ZoneOffset.UTC));
        runtime.beginAgentRun(AGENT_RUN_ID);
        new FileSystemPendingFinalizationStore(checkpointRoot).record(
                new PendingFinalization(
                        GOAL_ID, AGENT_RUN_ID, Optional.empty()));
        assertTrue(new FileSpoolMessageTransport(
                invocationRoot.resolve(GOAL_ID).resolve(AGENT_RUN_ID)
                        .resolve(IsolatedWorkerMain.WORK_SPOOL),
                BackpressurePolicy.of(1))
                .send(new TransportMessage(
                        DeliveryDestination.queue(IsolatedWorkerMain.WORK_SPOOL),
                        work.workMessage()))
                .status()
                .isAccepted());

        SchedulerInvocationRecoveryStatus status =
                new SchedulerInvocationRecoveryStatusReader(
                        new SchedulerRecoveryStatusReader(
                                new FileSystemSchedulerQueueStore(queueRoot),
                                new FileSystemAgentRuntimeStateStore(runtimeRoot),
                                new FileSystemPendingFinalizationStore(
                                        checkpointRoot),
                                new FileSystemRunRecordStore(recordRoot)),
                        new FileSystemAgentRuntimeStateStore(runtimeRoot),
                        new FileSystemRunRecordStore(recordRoot),
                        invocationRoot)
                        .read(QUEUE_ID);

        assertEquals(
                SchedulerInvocationRecoveryStatus.RecoveryPhase
                        .WORK_MESSAGE_AWAITING_RESULT,
                status.phase());
        assertTrue(status.workMessagePresent());
        assertFalse(status.resultMessagePresent());
    }

    @Test
    void rejectsCorruptAndSeveralWorkMessages() throws Exception {
        ReaderFixture corrupt = fixture("corrupt");
        Path corruptSpool = corrupt.workSpool();
        Files.createDirectories(corruptSpool);
        Files.write(
                corruptSpool.resolve("corrupt.transport"),
                new byte[] {1, 2, 3});

        assertThrows(
                IOException.class,
                () -> corrupt.reader().read(QUEUE_ID));

        ReaderFixture several = fixture("several");
        spoolWork(several, 2);
        spoolWork(several, 2);

        IOException refused = assertThrows(
                IOException.class,
                () -> several.reader().read(QUEUE_ID));
        assertTrue(refused.getMessage().contains("several messages"));
    }

    @Test
    void rejectsAWorkSpoolThatChangesBetweenSamples() throws Exception {
        ReaderFixture fixture = fixture("drift");
        spoolWork(fixture, 1);
        FileSystemAgentRuntimeStateStore durableStore =
                new FileSystemAgentRuntimeStateStore(fixture.runtimeRoot());
        MutatingRuntimeStore mutatingStore = new MutatingRuntimeStore(
                durableStore,
                4,
                () -> Files.delete(soleTransport(fixture.workSpool())));
        SchedulerInvocationRecoveryStatusReader reader =
                new SchedulerInvocationRecoveryStatusReader(
                        new SchedulerRecoveryStatusReader(
                                new FileSystemSchedulerQueueStore(
                                        fixture.queueRoot()),
                                mutatingStore,
                                new FileSystemPendingFinalizationStore(
                                        fixture.checkpointRoot()),
                                new FileSystemRunRecordStore(
                                        fixture.recordRoot())),
                        mutatingStore,
                        new FileSystemRunRecordStore(fixture.recordRoot()),
                        fixture.invocationRoot());

        assertThrows(
                ConcurrentSchedulerInvocationInspectionException.class,
                () -> reader.read(QUEUE_ID));
    }

    @Test
    void validatesPublishedResultAgainstItsRunRecord() throws Exception {
        Path base = temporaryRoot.resolve("result");
        Path projectRoot = Files.createDirectories(base.resolve("project"));
        Path target = projectRoot.resolve("TARGET.md");
        Files.writeString(target, "invocation result target\n");
        String digest = sha256(target);
        WorkItem work = new WorkItem(
                WORK_ITEM_ID,
                "read-file-worker",
                new MessageEnvelope(
                        "00000000-0000-0000-0000-000000000b05",
                        "invocation-status-correlation",
                        Optional.empty(),
                        "invocation-status-run",
                        "invocation-status-test",
                        NOW,
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "invocation-status-test",
                                        "TARGET.md",
                                        digest),
                                "b".repeat(64),
                                Set.of("read-file"),
                                Optional.of(new WorkPayload.ExecutionInput(
                                        "TARGET.md", digest)))));
        ReaderFixture fixture = fixture("result-fixture", work);
        spoolWork(fixture, 1);
        RunRecordStore records = new FileSystemRunRecordStore(
                fixture.recordRoot());
        String reference = new AgentLoopAgentRunExecution(
                projectRoot,
                new FileSystemEvidenceStore(
                        base.resolve("evidence"),
                        new EvidenceStoragePolicy(
                                EvidenceStoragePolicy.MAX_SUPPORTED_CONTENT_BYTES)),
                records,
                Clock.fixed(NOW, ZoneOffset.UTC))
                .executeWork(work, GOAL_ID, AGENT_RUN_ID);
        VerificationStatus recorded = records.resolve(reference)
                .record().verification().status();
        Path resultSpool = fixture.invocationRoot()
                .resolve(GOAL_ID)
                .resolve(AGENT_RUN_ID)
                .resolve(IsolatedWorkerMain.RESULT_SPOOL);
        MessageEnvelope result = new MessageEnvelope(
                UUID.randomUUID().toString(),
                work.workMessage().correlationId(),
                Optional.of(work.workMessage().messageId()),
                work.workMessage().logicalRunId(),
                "isolated-worker",
                NOW,
                new ResultPayload(
                        work.taskRevision().taskId(),
                        reference,
                        recorded));
        assertTrue(new FileSpoolMessageTransport(
                resultSpool, BackpressurePolicy.of(1))
                .send(new TransportMessage(
                        DeliveryDestination.queue(
                                IsolatedWorkerMain.RESULT_DESTINATION),
                        result))
                .status()
                .isAccepted());

        SchedulerInvocationRecoveryStatus status =
                fixture.reader().read(QUEUE_ID);

        assertEquals(
                SchedulerInvocationRecoveryStatus.RecoveryPhase
                        .RESULT_MESSAGE_PUBLISHED,
                status.phase());
        assertTrue(status.resultMessagePresent());
    }

    private ReaderFixture fixture(String name) throws Exception {
        return fixture(name, workItem());
    }

    private ReaderFixture fixture(String name, WorkItem work) throws Exception {
        Path base = temporaryRoot.resolve(name);
        Path queueRoot = base.resolve("queue");
        Path runtimeRoot = base.resolve("runtime");
        Path checkpointRoot = base.resolve("checkpoint");
        Path recordRoot = base.resolve("records");
        Path invocationRoot = base.resolve("invocations");
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(
                        QUEUE_ID,
                        16,
                        new FileSystemSchedulerQueueStore(queueRoot));
        queue.enqueue(new QueuedWork(work, List.of()));
        DurableAgentRuntime runtime = DurableAgentRuntime.create(
                GOAL_ID,
                work,
                new FileSystemAgentRuntimeStateStore(runtimeRoot),
                Clock.fixed(NOW, ZoneOffset.UTC));
        runtime.beginAgentRun(AGENT_RUN_ID);
        new FileSystemPendingFinalizationStore(checkpointRoot).record(
                new PendingFinalization(
                        GOAL_ID, AGENT_RUN_ID, Optional.empty()));
        SchedulerInvocationRecoveryStatusReader reader =
                new SchedulerInvocationRecoveryStatusReader(
                        new SchedulerRecoveryStatusReader(
                                new FileSystemSchedulerQueueStore(queueRoot),
                                new FileSystemAgentRuntimeStateStore(runtimeRoot),
                                new FileSystemPendingFinalizationStore(
                                        checkpointRoot),
                                new FileSystemRunRecordStore(recordRoot)),
                        new FileSystemAgentRuntimeStateStore(runtimeRoot),
                        new FileSystemRunRecordStore(recordRoot),
                        invocationRoot);
        return new ReaderFixture(
                queueRoot,
                runtimeRoot,
                checkpointRoot,
                recordRoot,
                invocationRoot,
                work,
                reader);
    }

    private static void spoolWork(ReaderFixture fixture, int capacity) {
        assertTrue(new FileSpoolMessageTransport(
                fixture.workSpool(), BackpressurePolicy.of(capacity))
                .send(new TransportMessage(
                        DeliveryDestination.queue(IsolatedWorkerMain.WORK_SPOOL),
                        fixture.work().workMessage()))
                .status()
                .isAccepted());
    }

    private static Path soleTransport(Path spool) throws IOException {
        try (var entries = Files.list(spool)) {
            return entries
                    .filter(path -> path.getFileName().toString()
                            .endsWith(FileSpoolMessageTransport.FILE_SUFFIX))
                    .findFirst()
                    .orElseThrow();
        }
    }

    private WorkItem workItem() {
        return new WorkItem(
                WORK_ITEM_ID,
                "read-file-worker",
                new MessageEnvelope(
                        "00000000-0000-0000-0000-000000000b05",
                        "invocation-status-correlation",
                        Optional.empty(),
                        "invocation-status-run",
                        "invocation-status-test",
                        NOW,
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "invocation-status-test",
                                        "CURRENT_TASK.md",
                                        "a".repeat(64)),
                                "b".repeat(64),
                                Set.of("read-file"))));
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readString(path, StandardCharsets.UTF_8)
                        .getBytes(StandardCharsets.UTF_8)));
    }

    private record ReaderFixture(
            Path queueRoot,
            Path runtimeRoot,
            Path checkpointRoot,
            Path recordRoot,
            Path invocationRoot,
            WorkItem work,
            SchedulerInvocationRecoveryStatusReader reader) {
        private Path workSpool() {
            return invocationRoot.resolve(GOAL_ID).resolve(AGENT_RUN_ID)
                    .resolve(IsolatedWorkerMain.WORK_SPOOL);
        }
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }

    private static final class MutatingRuntimeStore
            implements AgentRuntimeStateStore {
        private final AgentRuntimeStateStore delegate;
        private final int mutationResolution;
        private final IoAction mutation;
        private int resolutions;

        private MutatingRuntimeStore(
                AgentRuntimeStateStore delegate,
                int mutationResolution,
                IoAction mutation) {
            this.delegate = delegate;
            this.mutationResolution = mutationResolution;
            this.mutation = mutation;
        }

        @Override
        public void create(AgentRuntimeState initialState) {
            throw new AssertionError("inspection must not create runtime state");
        }

        @Override
        public void update(AgentRuntimeState nextState) {
            throw new AssertionError("inspection must not update runtime state");
        }

        @Override
        public AgentRuntimeState resolve(String goalId) throws IOException {
            resolutions++;
            if (resolutions == mutationResolution) {
                mutation.run();
            }
            return delegate.resolve(goalId);
        }
    }
}
