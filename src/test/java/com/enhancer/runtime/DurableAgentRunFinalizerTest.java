package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationDecision;
import com.enhancer.kernel.VerificationCode;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.loop.AgentLoopStopReason;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.run.FileSystemRunRecordStore;
import com.enhancer.run.PolicyDecision;
import com.enhancer.run.PolicyDecisionStatus;
import com.enhancer.run.RunRecord;
import com.enhancer.tool.ToolFailureCode;
import com.enhancer.tool.ToolRequest;
import com.enhancer.tool.ToolResult;
import com.enhancer.tool.ToolResultStatus;
import com.enhancer.tool.VerificationEvidence;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

class DurableAgentRunFinalizerTest {
    private static final String QUEUE_ID = "00000000-0000-0000-0000-000000000401";
    private static final String GOAL_ID = "00000000-0000-0000-0000-000000000402";
    private static final String AGENT_RUN_ID = "00000000-0000-0000-0000-000000000403";
    private static final String WORK_ID = "00000000-0000-0000-0000-000000000411";
    private static final String DEP_ID = "00000000-0000-0000-0000-000000000412";
    private static final String LATER_ID = "00000000-0000-0000-0000-000000000413";
    private static final String OWNER_ID = "00000000-0000-0000-0000-000000000421";
    private static final String TASK_ID = "gate-8-result-path-finalization";
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    @Test
    void verifiedTypedModelRecordCompletesThroughTheV2Port() throws Exception {
        Path projectRoot = tempDir.resolve("model-project");
        java.nio.file.Files.createDirectories(projectRoot);
        ModelProcessValidationTestFixture.Prepared prepared =
                ModelProcessValidationTestFixture.valid(projectRoot);
        FileSystemSchedulerQueueStore queueStore =
                new FileSystemSchedulerQueueStore(tempDir.resolve("model-queue"));
        FileSystemAgentRuntimeStateStore runtimeStore =
                new FileSystemAgentRuntimeStateStore(tempDir.resolve("model-runtime"));
        FileSystemRunRecordStore recordStore =
                new FileSystemRunRecordStore(tempDir.resolve("model-records"));
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, queueStore);
        queue.enqueue(new QueuedWork(prepared.workItem(), List.of()));
        AgentRunDispatch dispatch = new DurableAgentRunDispatcher(
                queue, runtimeStore, CLOCK)
                .claimAndLease(
                        ModelAttemptTestFixture.GOAL_ID,
                        ModelAttemptTestFixture.AGENT_RUN_ID,
                        OWNER_ID,
                        Duration.ofMinutes(5))
                .orElseThrow();
        DurableAgentRuntime.recover(
                        ModelAttemptTestFixture.GOAL_ID, runtimeStore, CLOCK)
                .completeExecution(
                        ModelAttemptTestFixture.AGENT_RUN_ID,
                        OWNER_ID,
                        dispatch.lease().fenceToken());
        String recordId = AgentRunRecordIdentity.recordId(
                ModelAttemptTestFixture.GOAL_ID,
                ModelAttemptTestFixture.AGENT_RUN_ID);
        String reference = recordStore.persistModel(
                recordId, prepared.resolved().record()).reference();
        DurableAgentRunFinalizer finalizer = new DurableAgentRunFinalizer(
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, queueStore),
                runtimeStore,
                recordStore,
                recordStore,
                prepared.evidenceStore(),
                projectRoot,
                prepared.configuration(),
                CLOCK);

        assertEquals(
                WorkItemDisposition.VERIFIED_COMPLETED,
                finalizer.finalizeAgentRun(
                        ModelAttemptTestFixture.GOAL_ID,
                        ModelAttemptTestFixture.AGENT_RUN_ID,
                        reference));
        assertEquals(
                RuntimeAgentRunStatus.COMPLETED,
                DurableAgentRuntime.recover(
                                ModelAttemptTestFixture.GOAL_ID,
                                runtimeStore,
                                CLOCK)
                        .agentRun().orElseThrow().status());
    }

    @Test
    void verifiedOutcomeCompletesRuntimeAndReleasesDependent() throws Exception {
        Setup s = awaitingVerification(true);
        String reference = persistRunRecord(s, true);

        DurableAgentRunFinalizer finalizer = finalizer(s);
        WorkItemDisposition disposition =
                finalizer.finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, reference);

        assertEquals(WorkItemDisposition.VERIFIED_COMPLETED, disposition);
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        assertEquals(RuntimeAgentRunStatus.COMPLETED,
                runtime.agentRun().orElseThrow().status());
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore);
        assertEquals(Set.of(WORK_ID), queue.completedWorkItemIds());
        assertEquals(DEP_ID, queue.claimNext().orElseThrow().workItemId());
    }

    @Test
    void failedAttemptStopsAtRetryPendingWithoutQueueDisposition() throws Exception {
        Setup s = awaitingVerification(true);
        String reference = persistRunRecord(s, false);

        DurableAgentRunFinalizer finalizer = finalizer(s);
        RuntimeGoalStatus status =
                finalizer.recordAgentRunResult(GOAL_ID, AGENT_RUN_ID, reference);

        assertEquals(RuntimeGoalStatus.RETRY_PENDING, status);
        assertTrue(finalizer.finalizeTerminalDisposition(GOAL_ID).isEmpty());
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        assertEquals(RuntimeGoalStatus.RETRY_PENDING, runtime.goal().status());
        assertEquals(RuntimeAgentRunStatus.FAILED,
                runtime.agentRun().orElseThrow().status());
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore);
        assertTrue(queue.failedWorkItemIds().isEmpty());
        assertTrue(queue.completedWorkItemIds().isEmpty());
        assertEquals(WORK_ID, queue.claimNext().orElseThrow().workItemId());
    }

    @Test
    void refusedRetryDecisionPermitsOneTerminalFailedDisposition() throws Exception {
        Setup s = awaitingVerification(false);
        String reference = persistRunRecord(s, false);
        DurableAgentRunFinalizer finalizer = finalizer(s);
        assertEquals(
                RuntimeGoalStatus.RETRY_PENDING,
                finalizer.recordAgentRunResult(
                        GOAL_ID, AGENT_RUN_ID, reference));

        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        runtime.recordRetryDecision(new AgentRunRetryDecisionRecord(
                AGENT_RUN_ID,
                1,
                1,
                0,
                0,
                "c".repeat(64),
                AgentRunRetryDecision.refused(
                        AgentRunRetryRefusalReason.ATTEMPTS_EXHAUSTED)));
        runtime.abandonGoal();

        assertEquals(
                Optional.of(WorkItemDisposition.FAILED),
                finalizer.finalizeTerminalDisposition(GOAL_ID));
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore);
        assertEquals(Set.of(WORK_ID), queue.failedWorkItemIds());
    }

    @Test
    void recoverFinalizationAppliesDispositionFromTerminalRuntimeWithoutReference()
            throws Exception {
        Setup s = awaitingVerification(true);
        String reference = persistRunRecord(s, true);
        // Simulate a crash after the runtime terminal transition, before the queue disposition.
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        runtime.recordResult(
                AGENT_RUN_ID,
                terminalResultEnvelope(s.work, reference, VerificationStatus.VERIFIED));

        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore);
        DurableAgentRunFinalizer finalizer = new DurableAgentRunFinalizer(
                queue, s.runtimeStore, s.runRecordStore, CLOCK);

        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                finalizer.recoverFinalization(GOAL_ID));
        assertEquals(DEP_ID, queue.claimNext().orElseThrow().workItemId());
    }

    @Test
    void reFinalizeAfterTerminalIsIdempotent() throws Exception {
        Setup s = awaitingVerification(false);
        String reference = persistRunRecord(s, true);
        finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, reference);

        // A second finalize with the same reference must not throw and must not change state.
        WorkItemDisposition disposition =
                finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, reference);
        assertEquals(WorkItemDisposition.VERIFIED_COMPLETED, disposition);
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore);
        assertEquals(Set.of(WORK_ID), queue.completedWorkItemIds());

        // recoverFinalization on a fully finalized run is also a no-op disposition report.
        assertEquals(Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                finalizer(s).recoverFinalization(GOAL_ID));
    }

    @Test
    void missingRunRecordFailsClosedAndLeavesRunRecoverable() throws Exception {
        Setup s = awaitingVerification(false);
        String missing = "run-record/00000000-0000-0000-0000-0000000009ff";

        assertThrows(com.enhancer.run.MissingRunRecordException.class, () ->
                finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, missing));

        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        assertEquals(RuntimeAgentRunStatus.AWAITING_VERIFICATION,
                runtime.agentRun().orElseThrow().status());
        // Fail-closed: no disposition recorded and the work stays recoverable (the durable queue's
        // recovery contract requeues in-flight work to pending, so it is claimable again).
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore);
        assertTrue(queue.completedWorkItemIds().isEmpty());
        assertTrue(queue.failedWorkItemIds().isEmpty());
        assertEquals(WORK_ID, queue.claimNext().orElseThrow().workItemId());
    }

    @Test
    void runRecordForADifferentTaskIsRejected() throws Exception {
        Setup s = awaitingVerification(false);
        String reference = s.runRecordStore.persist(new RunRecord(
                "logical-run-finalizer-1",
                Instant.parse("2026-07-17T11:00:00Z"),
                new ApprovedTask(
                        "a-different-task",
                        "Different task",
                        "Approved by test owner",
                        Set.of("read-file"),
                        "CURRENT_TASK.md"),
                new ToolRequest("read-file", "correlation-1", Map.of("path", "t.txt")),
                new PolicyDecision(PolicyDecisionStatus.ALLOWED, "C:/project",
                        Set.of("read-file"), Set.of(), 4096, 1000),
                success(),
                Optional.of("a".repeat(64)),
                VerificationDecision.verified("content matched"),
                1,
                AgentLoopStopReason.AWAITING_VERIFICATION,
                AgentLoopStopReason.COMPLETED)).reference();

        assertThrows(IllegalArgumentException.class, () ->
                finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, reference));
    }

    @Test
    void reFinalizeWithDifferentReferenceIsRejected() throws Exception {
        Setup s = awaitingVerification(false);
        String first = persistRunRecord(s, true);
        String second = persistRunRecord(s, true);
        finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, first);

        assertThrows(IllegalStateException.class, () ->
                finalizer(s).finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, second));
    }

    @Test
    void finalizeBeforeExecutionAcknowledgementIsRejected() throws Exception {
        // Reach EXECUTING (leased) but do NOT completeExecution.
        FileSystemSchedulerQueueStore queueStore =
                new FileSystemSchedulerQueueStore(tempDir.resolve("queue"));
        FileSystemAgentRuntimeStateStore runtimeStore =
                new FileSystemAgentRuntimeStateStore(tempDir.resolve("runtime"));
        FileSystemRunRecordStore runRecordStore =
                new FileSystemRunRecordStore(tempDir.resolve("records"));
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, queueStore);
        queue.enqueue(new QueuedWork(workItem(WORK_ID), List.of()));
        new DurableAgentRunDispatcher(queue, runtimeStore, CLOCK)
                .claimAndLease(GOAL_ID, AGENT_RUN_ID, OWNER_ID, Duration.ofMinutes(5))
                .orElseThrow();
        String reference =
                runRecordStore.persist(runRecord(true)).reference();

        DurableAgentRunFinalizer finalizer = new DurableAgentRunFinalizer(
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, queueStore),
                runtimeStore, runRecordStore, CLOCK);
        assertThrows(IllegalStateException.class, () ->
                finalizer.finalizeAgentRun(GOAL_ID, AGENT_RUN_ID, reference));
    }

    @Test
    void recordsVerificationEventBeforeTheDurableQueueTerminationEvent()
            throws Exception {
        Setup s = awaitingVerification(true);
        String reference = persistRunRecord(s, true);
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(tempDir.resolve("events"));
        List<RuntimeEventPublicationReference> published = new ArrayList<>();
        DurableAgentRunFinalizer finalizer = eventAwareFinalizer(
                s, new RuntimeEventRecorder(eventStore, published::add));

        assertEquals(
                RuntimeGoalStatus.COMPLETED,
                finalizer.recordAgentRunResult(GOAL_ID, AGENT_RUN_ID, reference));

        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        MessageEnvelope result = runtime.agentRun()
                .orElseThrow()
                .resultMessage()
                .orElseThrow();
        RuntimeEventBinding binding = new RuntimeEventBinding(
                GOAL_ID,
                WORK_ID,
                s.work.taskRevision(),
                s.work.snapshotId(),
                s.work.logicalRunId(),
                s.work.workMessage().correlationId());
        RuntimeEvent expected = RuntimeEvent.create(
                result.occurredAt(),
                binding,
                AGENT_RUN_ID,
                Optional.of(result.messageId()),
                "durable-agent-run-finalizer",
                new RuntimeEventDetail.VerificationRecorded(
                        VerificationStatus.VERIFIED),
                List.of(
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.RESULT_MESSAGE,
                                "result-message/" + result.messageId(),
                                Optional.empty()),
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.RUN_RECORD,
                                reference,
                                Optional.empty())));
        RuntimeEventStream events = eventStore.resolve(GOAL_ID);
        assertEquals(1, events.revision());
        assertEquals(List.of(expected), events.events());
        assertEquals(
                List.of(RuntimeEventPublicationReference.from(expected)),
                published);

        assertEquals(
                Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                finalizer.finalizeTerminalDisposition(GOAL_ID));
        RuntimeEvent terminated = RuntimeEvent.create(
                CLOCK.instant(),
                binding,
                AGENT_RUN_ID,
                Optional.of(result.messageId()),
                "durable-agent-run-finalizer",
                new RuntimeEventDetail.WorkItemTerminated(
                        WorkItemDisposition.VERIFIED_COMPLETED),
                List.of(new RuntimeEventReference(
                        RuntimeEventReferenceKind.SCHEDULER_QUEUE,
                        "scheduler-queue/"
                                + QUEUE_ID
                                + "/work-item/"
                                + WORK_ID
                                + "/disposition/VERIFIED_COMPLETED",
                        Optional.empty())));
        RuntimeEventStream afterDisposition = eventStore.resolve(GOAL_ID);
        assertEquals(2, afterDisposition.revision());
        assertEquals(List.of(expected, terminated), afterDisposition.events());
        assertEquals(
                List.of(
                        RuntimeEventPublicationReference.from(expected),
                        RuntimeEventPublicationReference.from(terminated)),
                published);
    }

    @Test
    void recordsStagnationAfterTheDurableResultTransition() throws Exception {
        Setup s = awaitingVerification(false);
        RunRecord stagnated = stagnatedRunRecord();
        String reference = s.runRecordStore.persist(stagnated).reference();
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(tempDir.resolve("events"));
        List<RuntimeEventPublicationReference> published = new ArrayList<>();
        DurableAgentRunFinalizer finalizer = eventAwareFinalizer(
                s, new RuntimeEventRecorder(eventStore, published::add));

        assertEquals(
                RuntimeGoalStatus.RETRY_PENDING,
                finalizer.recordAgentRunResult(GOAL_ID, AGENT_RUN_ID, reference));

        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        MessageEnvelope result = runtime.agentRun()
                .orElseThrow()
                .resultMessage()
                .orElseThrow();
        RuntimeEventBinding binding = new RuntimeEventBinding(
                GOAL_ID,
                WORK_ID,
                s.work.taskRevision(),
                s.work.snapshotId(),
                s.work.logicalRunId(),
                s.work.workMessage().correlationId());
        RuntimeEvent verification = RuntimeEvent.create(
                result.occurredAt(),
                binding,
                AGENT_RUN_ID,
                Optional.of(result.messageId()),
                "durable-agent-run-finalizer",
                new RuntimeEventDetail.VerificationRecorded(
                        VerificationStatus.NOT_PERFORMED),
                List.of(
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.RESULT_MESSAGE,
                                "result-message/" + result.messageId(),
                                Optional.empty()),
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.RUN_RECORD,
                                reference,
                                Optional.empty())));
        RuntimeEvent stagnation = RuntimeEvent.create(
                stagnated.recordedAt(),
                binding,
                AGENT_RUN_ID,
                Optional.of(result.messageId()),
                "durable-agent-run-finalizer",
                new RuntimeEventDetail.StagnationDetected(3, 3),
                List.of(
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.RESULT_MESSAGE,
                                "result-message/" + result.messageId(),
                                Optional.empty()),
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.RUN_RECORD,
                                reference,
                                Optional.empty())));

        RuntimeEventStream events = eventStore.resolve(GOAL_ID);
        assertEquals(2, events.revision());
        assertEquals(List.of(verification, stagnation), events.events());
        assertEquals(
                List.of(
                        RuntimeEventPublicationReference.from(verification),
                        RuntimeEventPublicationReference.from(stagnation)),
                published);
    }

    @Test
    void recordsToolTimeoutAfterTheDurableResultTransition() throws Exception {
        Setup s = awaitingVerification(false);
        RunRecord timedOut = timedOutRunRecord(
                AgentLoopStopReason.MAX_ITERATIONS, 5);
        String reference = s.runRecordStore.persist(timedOut).reference();
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(tempDir.resolve("timeout-events"));
        List<RuntimeEventPublicationReference> published = new ArrayList<>();

        assertEquals(
                RuntimeGoalStatus.RETRY_PENDING,
                eventAwareFinalizer(
                                s,
                                new RuntimeEventRecorder(
                                        eventStore, published::add))
                        .recordAgentRunResult(
                                GOAL_ID, AGENT_RUN_ID, reference));

        RuntimeAgentRun run = DurableAgentRuntime.recover(
                        GOAL_ID, s.runtimeStore, CLOCK)
                .agentRun()
                .orElseThrow();
        MessageEnvelope result = run.resultMessage().orElseThrow();
        RuntimeEventStream stream = eventStore.resolve(GOAL_ID);
        assertEquals(2, stream.revision());
        assertEquals(
                List.of(
                        RuntimeEventKind.VERIFICATION_RECORDED,
                        RuntimeEventKind.TIMEOUT_DETECTED),
                stream.events().stream().map(RuntimeEvent::kind).toList());
        RuntimeEvent timeout = stream.events().get(1);
        assertEquals(timedOut.recordedAt(), timeout.occurredAt());
        assertEquals(Optional.of(result.messageId()), timeout.causationId());
        assertEquals(
                new RuntimeEventDetail.TimeoutDetected(RuntimeTimeoutKind.TOOL),
                timeout.detail());
        assertEquals(
                List.of(
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.RESULT_MESSAGE,
                                "result-message/" + result.messageId(),
                                Optional.empty()),
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.RUN_RECORD,
                                reference,
                                Optional.empty())),
                timeout.authoritativeReferences());
        assertEquals(
                stream.events().stream()
                        .map(RuntimeEventPublicationReference::from)
                        .toList(),
                published);
    }

    @Test
    void recordsToolTimeoutBeforeTheSeparateStagnationFact() throws Exception {
        Setup s = awaitingVerification(false);
        String reference = s.runRecordStore.persist(timedOutRunRecord(
                AgentLoopStopReason.STAGNATED, 3)).reference();
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(
                        tempDir.resolve("timeout-stagnation-events"));

        eventAwareFinalizer(
                        s,
                        new RuntimeEventRecorder(eventStore, ignored -> { }))
                .recordAgentRunResult(GOAL_ID, AGENT_RUN_ID, reference);

        assertEquals(
                List.of(
                        RuntimeEventKind.VERIFICATION_RECORDED,
                        RuntimeEventKind.TIMEOUT_DETECTED,
                        RuntimeEventKind.STAGNATION_DETECTED),
                eventStore.resolve(GOAL_ID).events().stream()
                        .map(RuntimeEvent::kind)
                        .toList());
    }

    @Test
    void missingToolTimeoutEventRepairsFromTheDurableResult()
            throws Exception {
        Setup s = awaitingVerification(false);
        String reference = s.runRecordStore.persist(timedOutRunRecord(
                AgentLoopStopReason.MAX_ITERATIONS, 5)).reference();
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(
                        tempDir.resolve("timeout-append-recovery"));
        AtomicInteger appends = new AtomicInteger();
        RuntimeEventStore failSecondAppend = new RuntimeEventStore() {
            @Override
            public RuntimeEventAppendResult append(RuntimeEvent event)
                    throws IOException {
                if (appends.incrementAndGet() == 2) {
                    throw new IOException("timeout event append unavailable");
                }
                return eventStore.append(event);
            }

            @Override
            public RuntimeEventStream resolve(String goalId)
                    throws IOException {
                return eventStore.resolve(goalId);
            }
        };
        List<RuntimeEventPublicationReference> firstPublications =
                new ArrayList<>();

        assertThrows(
                IOException.class,
                () -> eventAwareFinalizer(
                                s,
                                new RuntimeEventRecorder(
                                        failSecondAppend,
                                        firstPublications::add))
                        .recordAgentRunResult(
                                GOAL_ID, AGENT_RUN_ID, reference));

        DurableAgentRuntime runtime = DurableAgentRuntime.recover(
                GOAL_ID, s.runtimeStore, CLOCK);
        assertEquals(RuntimeGoalStatus.RETRY_PENDING, runtime.goal().status());
        long resultRevision = runtime.revision();
        assertEquals(
                List.of(RuntimeEventKind.VERIFICATION_RECORDED),
                eventStore.resolve(GOAL_ID).events().stream()
                        .map(RuntimeEvent::kind)
                        .toList());
        assertEquals(1, firstPublications.size());

        List<RuntimeEventPublicationReference> recoveredPublications =
                new ArrayList<>();
        eventAwareFinalizer(
                        s,
                        new RuntimeEventRecorder(
                                eventStore,
                                recoveredPublications::add))
                .recordAgentRunResult(GOAL_ID, AGENT_RUN_ID, reference);

        assertEquals(
                resultRevision,
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK)
                        .revision());
        RuntimeEventStream recovered = eventStore.resolve(GOAL_ID);
        assertEquals(2, recovered.revision());
        assertEquals(
                List.of(
                        RuntimeEventKind.VERIFICATION_RECORDED,
                        RuntimeEventKind.TIMEOUT_DETECTED),
                recovered.events().stream().map(RuntimeEvent::kind).toList());
        assertEquals(
                recovered.events().stream()
                        .map(RuntimeEventPublicationReference::from)
                        .toList(),
                recoveredPublications);
    }

    @Test
    void toolTimeoutPublicationFailureReplaysTheExactPersistedEvent()
            throws Exception {
        Setup s = awaitingVerification(false);
        String reference = s.runRecordStore.persist(timedOutRunRecord(
                AgentLoopStopReason.MAX_ITERATIONS, 5)).reference();
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(
                        tempDir.resolve("timeout-publication-recovery"));
        AtomicInteger attempts = new AtomicInteger();
        RuntimeEventRecorder failingRecorder = new RuntimeEventRecorder(
                eventStore,
                ignored -> {
                    if (attempts.incrementAndGet() == 2) {
                        throw new IOException("timeout publication unavailable");
                    }
                });

        assertThrows(
                IOException.class,
                () -> eventAwareFinalizer(s, failingRecorder)
                        .recordAgentRunResult(
                                GOAL_ID, AGENT_RUN_ID, reference));

        DurableAgentRuntime runtime = DurableAgentRuntime.recover(
                GOAL_ID, s.runtimeStore, CLOCK);
        long resultRevision = runtime.revision();
        RuntimeEventStream persisted = eventStore.resolve(GOAL_ID);
        assertEquals(2, persisted.revision());
        assertEquals(RuntimeEventKind.TIMEOUT_DETECTED,
                persisted.events().get(1).kind());

        List<RuntimeEventPublicationReference> replayed = new ArrayList<>();
        eventAwareFinalizer(
                        s,
                        new RuntimeEventRecorder(eventStore, replayed::add))
                .recordAgentRunResult(GOAL_ID, AGENT_RUN_ID, reference);

        assertEquals(
                resultRevision,
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK)
                        .revision());
        RuntimeEventStream afterReplay = eventStore.resolve(GOAL_ID);
        assertEquals(persisted.revision(), afterReplay.revision());
        assertEquals(persisted.binding(), afterReplay.binding());
        assertEquals(persisted.events(), afterReplay.events());
        assertEquals(
                persisted.events().stream()
                        .map(RuntimeEventPublicationReference::from)
                        .toList(),
                replayed);
    }

    @Test
    void stagnationPublisherFailureRepairsAfterLaterRuntimeRevision()
            throws Exception {
        Setup s = awaitingVerification(false);
        String reference = s.runRecordStore.persist(stagnatedRunRecord()).reference();
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(tempDir.resolve("events"));
        AtomicInteger attempts = new AtomicInteger();
        RuntimeEventRecorder failingRecorder = new RuntimeEventRecorder(
                eventStore,
                ignored -> {
                    if (attempts.incrementAndGet() == 2) {
                        throw new IOException("stagnation publication unavailable");
                    }
                });

        assertThrows(
                IOException.class,
                () -> eventAwareFinalizer(s, failingRecorder)
                        .recordAgentRunResult(GOAL_ID, AGENT_RUN_ID, reference));
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        assertEquals(RuntimeGoalStatus.RETRY_PENDING, runtime.goal().status());
        assertEquals(2, eventStore.resolve(GOAL_ID).revision());
        assertEquals(2, attempts.get());

        runtime.recordRetryDecision(new AgentRunRetryDecisionRecord(
                AGENT_RUN_ID,
                1,
                1,
                0,
                0,
                "c".repeat(64),
                AgentRunRetryDecision.refused(
                        AgentRunRetryRefusalReason.ATTEMPTS_EXHAUSTED)));
        runtime.abandonGoal();
        long laterRuntimeRevision = runtime.revision();

        List<RuntimeEventPublicationReference> published = new ArrayList<>();
        assertEquals(
                RuntimeGoalStatus.FAILED,
                eventAwareFinalizer(
                                s,
                                new RuntimeEventRecorder(eventStore, published::add),
                                Clock.offset(CLOCK, Duration.ofHours(2)))
                        .recordAgentRunResult(GOAL_ID, AGENT_RUN_ID, reference));

        RuntimeEventStream replayed = eventStore.resolve(GOAL_ID);
        assertEquals(2, replayed.revision());
        assertEquals(2, replayed.events().size());
        assertEquals(
                new RuntimeEventDetail.StagnationDetected(3, 3),
                replayed.events().get(1).detail());
        assertEquals(
                List.of(
                        RuntimeEventPublicationReference.from(replayed.events().get(0)),
                        RuntimeEventPublicationReference.from(replayed.events().get(1))),
                published);
        assertEquals(
                laterRuntimeRevision,
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK)
                        .revision());
    }

    @Test
    void exactResultReplayRepairsAndRepublishesAfterLaterRuntimeRevisions()
            throws Exception {
        Setup s = awaitingVerification(false);
        String reference = persistRunRecord(s, false);
        assertEquals(
                RuntimeGoalStatus.RETRY_PENDING,
                finalizer(s).recordAgentRunResult(
                        GOAL_ID, AGENT_RUN_ID, reference));
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        runtime.recordRetryDecision(new AgentRunRetryDecisionRecord(
                AGENT_RUN_ID,
                1,
                1,
                0,
                0,
                "c".repeat(64),
                AgentRunRetryDecision.refused(
                        AgentRunRetryRefusalReason.ATTEMPTS_EXHAUSTED)));
        runtime.abandonGoal();
        long laterRuntimeRevision = runtime.revision();

        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(tempDir.resolve("events"));
        List<RuntimeEventPublicationReference> published = new ArrayList<>();
        DurableAgentRunFinalizer finalizer = eventAwareFinalizer(
                s, new RuntimeEventRecorder(eventStore, published::add));

        assertEquals(
                RuntimeGoalStatus.FAILED,
                finalizer.recordAgentRunResult(GOAL_ID, AGENT_RUN_ID, reference));
        assertEquals(
                RuntimeGoalStatus.FAILED,
                finalizer.recordAgentRunResult(GOAL_ID, AGENT_RUN_ID, reference));

        RuntimeEventStream repaired = eventStore.resolve(GOAL_ID);
        assertEquals(1, repaired.revision());
        assertEquals(1, repaired.events().size());
        assertEquals(
                new RuntimeEventDetail.VerificationRecorded(
                        VerificationStatus.REJECTED),
                repaired.events().get(0).detail());
        assertEquals(
                List.of(
                        RuntimeEventPublicationReference.from(
                                repaired.events().get(0)),
                        RuntimeEventPublicationReference.from(
                                repaired.events().get(0))),
                published);
        assertEquals(
                laterRuntimeRevision,
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK)
                        .revision());
    }

    @Test
    void publisherFailureLeavesResultAndEventRecoverableForExactReplay()
            throws Exception {
        Setup s = awaitingVerification(false);
        String reference = persistRunRecord(s, true);
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(tempDir.resolve("events"));
        AtomicInteger attempts = new AtomicInteger();
        RuntimeEventRecorder failingRecorder = new RuntimeEventRecorder(
                eventStore,
                ignored -> {
                    attempts.incrementAndGet();
                    throw new IOException("publication unavailable");
                });

        assertThrows(
                IOException.class,
                () -> eventAwareFinalizer(s, failingRecorder)
                        .recordAgentRunResult(GOAL_ID, AGENT_RUN_ID, reference));
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK);
        assertEquals(RuntimeGoalStatus.COMPLETED, runtime.goal().status());
        long runtimeRevision = runtime.revision();
        assertEquals(1, eventStore.resolve(GOAL_ID).revision());

        List<RuntimeEventPublicationReference> published = new ArrayList<>();
        assertEquals(
                RuntimeGoalStatus.COMPLETED,
                eventAwareFinalizer(
                                s,
                                new RuntimeEventRecorder(
                                        eventStore, published::add))
                        .recordAgentRunResult(GOAL_ID, AGENT_RUN_ID, reference));

        RuntimeEventStream replayed = eventStore.resolve(GOAL_ID);
        assertEquals(1, replayed.revision());
        assertEquals(1, replayed.events().size());
        assertEquals(1, attempts.get());
        assertEquals(
                List.of(RuntimeEventPublicationReference.from(
                        replayed.events().get(0))),
                published);
        assertEquals(
                runtimeRevision,
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK)
                        .revision());
    }

    @Test
    void repairsMissingTerminationAfterLaterQueueRevisionsAndReusesFirstOccurrence()
            throws Exception {
        Setup s = awaitingVerification(true);
        String reference = persistRunRecord(s, true);
        DurableAgentRunFinalizer sourceFinalizer = finalizer(s);
        sourceFinalizer.recordAgentRunResult(GOAL_ID, AGENT_RUN_ID, reference);
        assertEquals(
                Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                sourceFinalizer.finalizeTerminalDisposition(GOAL_ID));

        DurableSingleWorkerSchedulerQueue advancedQueue =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID, s.queueStore);
        assertEquals(DEP_ID, advancedQueue.claimNext().orElseThrow().workItemId());
        advancedQueue.completeActiveVerified(DEP_ID);

        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(tempDir.resolve("events"));
        List<RuntimeEventPublicationReference> published = new ArrayList<>();
        RuntimeEventRecorder recorder =
                new RuntimeEventRecorder(eventStore, published::add);
        Clock firstRecoveryClock = Clock.offset(CLOCK, Duration.ofHours(1));
        assertEquals(
                Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                eventAwareFinalizer(s, recorder, firstRecoveryClock)
                        .recoverFinalization(GOAL_ID));

        RuntimeEvent first = eventStore.resolve(GOAL_ID).events().get(0);
        assertEquals(RuntimeEventKind.WORK_ITEM_TERMINATED, first.kind());
        assertEquals(firstRecoveryClock.instant(), first.occurredAt());
        assertEquals(
                List.of(new RuntimeEventReference(
                        RuntimeEventReferenceKind.SCHEDULER_QUEUE,
                        "scheduler-queue/"
                                + QUEUE_ID
                                + "/work-item/"
                                + WORK_ID
                                + "/disposition/VERIFIED_COMPLETED",
                        Optional.empty())),
                first.authoritativeReferences());

        DurableSingleWorkerSchedulerQueue laterQueue =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID, s.queueStore);
        laterQueue.enqueue(new QueuedWork(workItem(LATER_ID), List.of()));
        Clock laterRecoveryClock = Clock.offset(CLOCK, Duration.ofHours(2));
        assertEquals(
                Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                eventAwareFinalizer(s, recorder, laterRecoveryClock)
                        .recoverFinalization(GOAL_ID));

        RuntimeEventStream replayed = eventStore.resolve(GOAL_ID);
        assertEquals(1, replayed.revision());
        assertEquals(List.of(first), replayed.events());
        assertEquals(firstRecoveryClock.instant(), replayed.events().get(0).occurredAt());
        assertEquals(
                List.of(
                        RuntimeEventPublicationReference.from(first),
                        RuntimeEventPublicationReference.from(first)),
                published);
    }

    @Test
    void terminationPublisherFailureLeavesQueueAndEventRecoverable()
            throws Exception {
        Setup s = awaitingVerification(false);
        String reference = persistRunRecord(s, true);
        finalizer(s).recordAgentRunResult(GOAL_ID, AGENT_RUN_ID, reference);
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(tempDir.resolve("events"));
        Clock firstClock = Clock.offset(CLOCK, Duration.ofHours(1));
        RuntimeEventRecorder failingRecorder = new RuntimeEventRecorder(
                eventStore,
                ignored -> {
                    throw new IOException("publication unavailable");
                });

        assertThrows(
                IOException.class,
                () -> eventAwareFinalizer(s, failingRecorder, firstClock)
                        .finalizeTerminalDisposition(GOAL_ID));
        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID, s.queueStore);
        assertEquals(Set.of(WORK_ID), queue.completedWorkItemIds());
        RuntimeEvent first = eventStore.resolve(GOAL_ID).events().get(0);
        assertEquals(firstClock.instant(), first.occurredAt());

        queue.enqueue(new QueuedWork(workItem(LATER_ID), List.of()));
        List<RuntimeEventPublicationReference> published = new ArrayList<>();
        RuntimeEventRecorder recoveryRecorder = new RuntimeEventRecorder(
                eventStore, published::add);
        assertEquals(
                Optional.of(WorkItemDisposition.VERIFIED_COMPLETED),
                eventAwareFinalizer(
                                s,
                                recoveryRecorder,
                                Clock.offset(CLOCK, Duration.ofHours(2)))
                        .recoverFinalization(GOAL_ID));

        RuntimeEventStream replayed = eventStore.resolve(GOAL_ID);
        assertEquals(1, replayed.revision());
        assertEquals(List.of(first), replayed.events());
        assertEquals(
                List.of(RuntimeEventPublicationReference.from(first)),
                published);
    }

    @Test
    void queuePersistenceFailureCreatesNoTerminationEventOrPublication()
            throws Exception {
        Setup s = awaitingVerification(false);
        String reference = persistRunRecord(s, true);
        finalizer(s).recordAgentRunResult(GOAL_ID, AGENT_RUN_ID, reference);
        SchedulerQueueStore failingQueueStore = new SchedulerQueueStore() {
            @Override
            public void create(SchedulerQueueState initialState) throws IOException {
                s.queueStore.create(initialState);
            }

            @Override
            public void update(SchedulerQueueState nextState) throws IOException {
                throw new IOException("queue persistence unavailable");
            }

            @Override
            public SchedulerQueueState resolve(String queueId) throws IOException {
                return s.queueStore.resolve(queueId);
            }
        };
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(tempDir.resolve("events"));
        List<RuntimeEventPublicationReference> published = new ArrayList<>();
        DurableAgentRunFinalizer finalizer = new DurableAgentRunFinalizer(
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID, failingQueueStore),
                s.runtimeStore,
                s.runRecordStore,
                CLOCK,
                new RuntimeEventRecorder(eventStore, published::add));

        assertThrows(
                IOException.class,
                () -> finalizer.finalizeTerminalDisposition(GOAL_ID));

        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID, s.queueStore);
        assertTrue(queue.completedWorkItemIds().isEmpty());
        assertTrue(queue.failedWorkItemIds().isEmpty());
        assertThrows(
                MissingRuntimeEventStreamException.class,
                () -> eventStore.resolve(GOAL_ID));
        assertTrue(published.isEmpty());
    }

    @Test
    void resultPersistenceFailureCreatesNoDerivedEventOrPublication()
            throws Exception {
        Setup s = awaitingVerification(false);
        String reference = s.runRecordStore.persist(timedOutRunRecord(
                AgentLoopStopReason.MAX_ITERATIONS, 5)).reference();
        AgentRuntimeStateStore failingRuntimeStore = new AgentRuntimeStateStore() {
            @Override
            public void create(AgentRuntimeState initialState) throws IOException {
                s.runtimeStore.create(initialState);
            }

            @Override
            public void update(AgentRuntimeState nextState) throws IOException {
                throw new IOException("runtime persistence unavailable");
            }

            @Override
            public AgentRuntimeState resolve(String goalId) throws IOException {
                return s.runtimeStore.resolve(goalId);
            }
        };
        FileSystemRuntimeEventStore eventStore =
                new FileSystemRuntimeEventStore(tempDir.resolve("events"));
        List<RuntimeEventPublicationReference> published = new ArrayList<>();
        DurableAgentRunFinalizer finalizer = new DurableAgentRunFinalizer(
                DurableSingleWorkerSchedulerQueue.recover(
                        QUEUE_ID, s.queueStore),
                failingRuntimeStore,
                s.runRecordStore,
                CLOCK,
                new RuntimeEventRecorder(eventStore, published::add));

        assertThrows(
                IOException.class,
                () -> finalizer.recordAgentRunResult(
                        GOAL_ID, AGENT_RUN_ID, reference));

        assertEquals(
                RuntimeAgentRunStatus.AWAITING_VERIFICATION,
                DurableAgentRuntime.recover(GOAL_ID, s.runtimeStore, CLOCK)
                        .agentRun()
                        .orElseThrow()
                        .status());
        assertThrows(
                MissingRuntimeEventStreamException.class,
                () -> eventStore.resolve(GOAL_ID));
        assertTrue(published.isEmpty());
    }

    private MessageEnvelope terminalResultEnvelope(
            WorkItem work, String reference, VerificationStatus status) {
        MessageEnvelope workMessage = work.workMessage();
        return new MessageEnvelope(
                java.util.UUID.nameUUIDFromBytes(
                        ("agent-run-result:" + AGENT_RUN_ID)
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(),
                workMessage.correlationId(),
                Optional.of(workMessage.messageId()),
                workMessage.logicalRunId(),
                "agent-run-finalizer",
                Instant.parse("2026-07-17T12:00:00Z"),
                new com.enhancer.bus.ResultPayload(TASK_ID, reference, status));
    }

    // ---- shared helpers ----

    private DurableAgentRunFinalizer finalizer(Setup s) throws IOException {
        return new DurableAgentRunFinalizer(
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore),
                s.runtimeStore,
                s.runRecordStore,
                CLOCK);
    }

    private DurableAgentRunFinalizer eventAwareFinalizer(
            Setup s,
            RuntimeEventRecorder eventRecorder) throws IOException {
        return eventAwareFinalizer(s, eventRecorder, CLOCK);
    }

    private DurableAgentRunFinalizer eventAwareFinalizer(
            Setup s,
            RuntimeEventRecorder eventRecorder,
            Clock clock) throws IOException {
        return new DurableAgentRunFinalizer(
                DurableSingleWorkerSchedulerQueue.recover(QUEUE_ID, s.queueStore),
                s.runtimeStore,
                s.runRecordStore,
                clock,
                eventRecorder);
    }

    private Setup awaitingVerification(boolean withDependent) throws IOException {
        FileSystemSchedulerQueueStore queueStore =
                new FileSystemSchedulerQueueStore(tempDir.resolve("queue"));
        FileSystemAgentRuntimeStateStore runtimeStore =
                new FileSystemAgentRuntimeStateStore(tempDir.resolve("runtime"));
        FileSystemRunRecordStore runRecordStore =
                new FileSystemRunRecordStore(tempDir.resolve("records"));

        DurableSingleWorkerSchedulerQueue queue =
                DurableSingleWorkerSchedulerQueue.create(QUEUE_ID, 8, queueStore);
        WorkItem work = workItem(WORK_ID);
        queue.enqueue(new QueuedWork(work, List.of()));
        if (withDependent) {
            queue.enqueue(new QueuedWork(workItem(DEP_ID), List.of(WORK_ID)));
        }

        DurableAgentRunDispatcher dispatcher =
                new DurableAgentRunDispatcher(queue, runtimeStore, CLOCK);
        AgentRunDispatch dispatch = dispatcher.claimAndLease(
                GOAL_ID, AGENT_RUN_ID, OWNER_ID, Duration.ofMinutes(5)).orElseThrow();

        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(GOAL_ID, runtimeStore, CLOCK);
        runtime.completeExecution(
                AGENT_RUN_ID, OWNER_ID, dispatch.lease().fenceToken());

        return new Setup(queueStore, runtimeStore, runRecordStore, work);
    }

    private String persistRunRecord(Setup s, boolean verified) throws IOException {
        return s.runRecordStore.persist(runRecord(verified)).reference();
    }

    private RunRecord runRecord(boolean verified) {
        return new RunRecord(
                "logical-run-finalizer-1",
                Instant.parse("2026-07-17T11:00:00Z"),
                new ApprovedTask(
                        TASK_ID,
                        "Finalize the Gate 8 result path",
                        "Approved by test owner",
                        Set.of("read-file"),
                        "CURRENT_TASK.md"),
                new ToolRequest("read-file", "correlation-1", Map.of("path", "target.txt")),
                new PolicyDecision(
                        PolicyDecisionStatus.ALLOWED,
                        "C:/project",
                        Set.of("read-file"),
                        Set.of(),
                        4096,
                        1000),
                verified ? success() : success(),
                Optional.of("a".repeat(64)),
                verified
                        ? VerificationDecision.verified("content matched")
                        : VerificationDecision.rejected(
                                VerificationCode.CONTENT_MISMATCH, "content differed"),
                1,
                AgentLoopStopReason.AWAITING_VERIFICATION,
                verified
                        ? AgentLoopStopReason.COMPLETED
                        : AgentLoopStopReason.AWAITING_VERIFICATION);
    }

    private RunRecord stagnatedRunRecord() {
        return new RunRecord(
                "logical-run-finalizer-1",
                Instant.parse("2026-07-17T11:00:00Z"),
                new ApprovedTask(
                        TASK_ID,
                        "Finalize the Gate 8 result path",
                        "Approved by test owner",
                        Set.of("read-file"),
                        "CURRENT_TASK.md"),
                new ToolRequest(
                        "read-file",
                        "correlation-1",
                        Map.of("path", "target.txt")),
                new PolicyDecision(
                        PolicyDecisionStatus.ALLOWED,
                        "C:/project",
                        Set.of("read-file"),
                        Set.of(),
                        4096,
                        1000),
                new ToolResult(
                        "read-file",
                        ToolResultStatus.FAILURE,
                        OptionalInt.empty(),
                        VerificationEvidence.capture(
                                "temporary failure",
                                "same failure",
                                Optional.empty())),
                Optional.empty(),
                VerificationDecision.notPerformed(
                        "stagnated before verification"),
                3,
                AgentLoopStopReason.STAGNATED,
                AgentLoopStopReason.STAGNATED);
    }

    private RunRecord timedOutRunRecord(
            AgentLoopStopReason stopReason,
            int iterations) {
        return new RunRecord(
                "logical-run-finalizer-1",
                Instant.parse("2026-07-17T11:00:00Z"),
                new ApprovedTask(
                        TASK_ID,
                        "Finalize the Gate 8 result path",
                        "Approved by test owner",
                        Set.of("read-file"),
                        "CURRENT_TASK.md"),
                new ToolRequest(
                        "read-file",
                        "correlation-1",
                        Map.of("path", "target.txt")),
                new PolicyDecision(
                        PolicyDecisionStatus.ALLOWED,
                        "C:/project",
                        Set.of("read-file"),
                        Set.of(),
                        4096,
                        1000),
                new ToolResult(
                        "read-file",
                        ToolResultStatus.FAILURE,
                        OptionalInt.empty(),
                        Optional.of(ToolFailureCode.TIMED_OUT),
                        VerificationEvidence.capture(
                                "Tool invocation timed out",
                                "timeout",
                                Optional.empty())),
                Optional.empty(),
                VerificationDecision.notPerformed(
                        "Tool timeout stopped before verification"),
                iterations,
                stopReason,
                stopReason);
    }

    private ToolResult success() {
        return new ToolResult(
                "read-file",
                ToolResultStatus.SUCCESS,
                OptionalInt.empty(),
                VerificationEvidence.capture("read succeeded", "content", Optional.empty()));
    }

    private static WorkItem workItem(String workItemId) {
        ApprovedTaskRevision revision = new ApprovedTaskRevision(
                TASK_ID, "CURRENT_TASK.md", "a".repeat(64));
        MessageEnvelope envelope = new MessageEnvelope(
                incrementUuid(workItemId),
                "correlation-finalizer-1",
                Optional.empty(),
                "logical-run-finalizer-1",
                "finalizer-test",
                Instant.parse("2026-07-17T10:00:00Z"),
                new WorkPayload(revision, "b".repeat(64), Set.of("read-file")));
        return new WorkItem(workItemId, "read-file-worker", envelope);
    }

    private static String incrementUuid(String workItemId) {
        long suffix = Long.parseLong(workItemId.substring(workItemId.length() - 12));
        return String.format("00000000-0000-0000-0002-%012d", suffix);
    }

    private record Setup(
            FileSystemSchedulerQueueStore queueStore,
            FileSystemAgentRuntimeStateStore runtimeStore,
            FileSystemRunRecordStore runRecordStore,
            WorkItem work) {
    }
}
