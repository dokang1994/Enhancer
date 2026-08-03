package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ResultPayload;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.run.RunRecord;
import com.enhancer.run.RunRecordStore;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Separates RunRecord-backed attempt result recording from Goal-terminal queue disposition.
 * Queue and runtime remain separate durable boundaries; no cross-store transaction is claimed.
 */
public final class DurableAgentRunFinalizer {
    private static final String EVENT_PRODUCER_ID =
            "durable-agent-run-finalizer";

    private final DurableSingleWorkerSchedulerQueue queue;
    private final AgentRuntimeStateStore runtimeStore;
    private final RunRecordStore runRecordStore;
    private final Clock clock;
    private final Optional<RuntimeEventRecorder> eventRecorder;

    public DurableAgentRunFinalizer(
            DurableSingleWorkerSchedulerQueue queue,
            AgentRuntimeStateStore runtimeStore,
            RunRecordStore runRecordStore,
            Clock clock) {
        this(queue, runtimeStore, runRecordStore, clock, Optional.empty());
    }

    public DurableAgentRunFinalizer(
            DurableSingleWorkerSchedulerQueue queue,
            AgentRuntimeStateStore runtimeStore,
            RunRecordStore runRecordStore,
            Clock clock,
            RuntimeEventRecorder eventRecorder) {
        this(
                queue,
                runtimeStore,
                runRecordStore,
                clock,
                Optional.of(Objects.requireNonNull(
                        eventRecorder, "eventRecorder must not be null")));
    }

    private DurableAgentRunFinalizer(
            DurableSingleWorkerSchedulerQueue queue,
            AgentRuntimeStateStore runtimeStore,
            RunRecordStore runRecordStore,
            Clock clock,
            Optional<RuntimeEventRecorder> eventRecorder) {
        this.queue = Objects.requireNonNull(queue, "queue must not be null");
        this.runtimeStore =
                Objects.requireNonNull(runtimeStore, "runtimeStore must not be null");
        this.runRecordStore =
                Objects.requireNonNull(runRecordStore, "runRecordStore must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.eventRecorder = Objects.requireNonNull(
                eventRecorder, "eventRecorder must not be null");
    }

    public WorkItemDisposition finalizeAgentRun(
            String goalId,
            String agentRunId,
            String runRecordReference) throws IOException {
        recordAgentRunResult(goalId, agentRunId, runRecordReference);
        return finalizeTerminalDisposition(goalId).orElseThrow(() ->
                new IllegalStateException(
                        "AgentRun result did not make the Goal terminal"));
    }

    public RuntimeGoalStatus recordAgentRunResult(
            String goalId,
            String agentRunId,
            String runRecordReference) throws IOException {
        String canonicalAgentRunId =
                RuntimeIdentity.canonicalUuid(agentRunId, "agentRunId");
        Objects.requireNonNull(
                runRecordReference, "runRecordReference must not be null");
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(goalId, runtimeStore, clock);
        RuntimeAgentRun run = requireRun(runtime, canonicalAgentRunId);
        WorkItem workItem = runtime.goal().workItem();

        switch (run.status()) {
            case AWAITING_VERIFICATION -> {
                ResolvedRunRecord resolved =
                        runRecordStore.resolve(runRecordReference);
                requireBinding(resolved.record(), workItem);
                VerificationStatus status = resolved.record().verification().status();
                MessageEnvelope result = buildResultEnvelope(
                        workItem, canonicalAgentRunId, runRecordReference, status);
                runtime.recordResult(canonicalAgentRunId, result);
            }
            case COMPLETED, FAILED ->
                    assertStoredResultReference(run, runRecordReference);
            default -> throw new IllegalStateException(
                    "AgentRun has not acknowledged execution");
        }
        if (eventRecorder.isPresent()) {
            eventRecorder.orElseThrow().recordAndPublish(
                    verificationRecordedEvent(
                            runtime, canonicalAgentRunId, runRecordReference));
        }
        return runtime.goal().status();
    }

    public Optional<WorkItemDisposition> finalizeTerminalDisposition(
            String goalId) throws IOException {
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(goalId, runtimeStore, clock);
        RuntimeAgentRun latest = runtime.agentRun().orElseThrow(() ->
                new IllegalStateException("no AgentRun exists"));
        WorkItemDisposition disposition;
        if (runtime.goal().status() == RuntimeGoalStatus.COMPLETED) {
            if (latest.status() != RuntimeAgentRunStatus.COMPLETED) {
                throw new IllegalStateException(
                        "completed Goal does not have a completed latest AgentRun");
            }
            disposition = WorkItemDisposition.VERIFIED_COMPLETED;
        } else if (runtime.goal().status() == RuntimeGoalStatus.FAILED) {
            if (latest.status() != RuntimeAgentRunStatus.FAILED
                    || runtime.retryDecisions().isEmpty()
                    || runtime.retryDecisions()
                            .get(runtime.retryDecisions().size() - 1)
                            .decision().isAdmitted()) {
                throw new IllegalStateException(
                        "failed Goal requires a refused latest retry decision");
            }
            disposition = WorkItemDisposition.FAILED;
        } else {
            return Optional.empty();
        }
        String workItemId = runtime.goal().workItem().workItemId();
        WorkItemDisposition recorded = applyQueueDisposition(
                workItemId, disposition);
        requireQueueDisposition(workItemId, recorded);
        if (eventRecorder.isPresent()) {
            eventRecorder.orElseThrow().recordAndPublishUsingFirstOccurrence(
                    workItemTerminatedEvent(runtime, latest, recorded));
        }
        return Optional.of(recorded);
    }

    public Optional<WorkItemDisposition> recoverFinalization(String goalId)
            throws IOException {
        return finalizeTerminalDisposition(goalId);
    }

    private RuntimeAgentRun requireRun(
            DurableAgentRuntime runtime,
            String agentRunId) {
        RuntimeAgentRun run = runtime.agentRun().orElseThrow(() ->
                new IllegalStateException("no AgentRun exists"));
        if (!run.agentRunId().equals(agentRunId)) {
            throw new IllegalArgumentException(
                    "agentRunId does not match the Goal's AgentRun");
        }
        return run;
    }

    static void requireBinding(RunRecord record, WorkItem workItem) {
        ApprovedTaskRevision revision = workItem.taskRevision();
        if (!record.approvedTask().taskId().equals(revision.taskId())) {
            throw new IllegalArgumentException(
                    "RunRecord task identity does not match the Goal work");
        }
        if (!record.approvedTask().sourceDocument().equals(revision.sourceDocument())) {
            throw new IllegalArgumentException(
                    "RunRecord source document does not match the Goal work");
        }
    }

    private void assertStoredResultReference(
            RuntimeAgentRun run,
            String runRecordReference) {
        MessageEnvelope stored = run.resultMessage().orElseThrow(() ->
                new IllegalStateException("terminal AgentRun has no result"));
        ResultPayload payload = (ResultPayload) stored.payload();
        if (!payload.runRecordReference().equals(runRecordReference)) {
            throw new IllegalStateException(
                    "terminal AgentRun was finalized with a different RunRecord");
        }
    }

    private RuntimeEvent verificationRecordedEvent(
            DurableAgentRuntime runtime,
            String agentRunId,
            String runRecordReference) {
        RuntimeAgentRun run = requireRun(runtime, agentRunId);
        MessageEnvelope result = run.resultMessage().orElseThrow(() ->
                new IllegalStateException(
                        "verification event requires a durable Result message"));
        ResultPayload payload = (ResultPayload) result.payload();
        if (!payload.runRecordReference().equals(runRecordReference)) {
            throw new IllegalStateException(
                    "verification event RunRecord reference does not match the durable Result");
        }
        WorkItem workItem = runtime.goal().workItem();
        RuntimeEventBinding binding = new RuntimeEventBinding(
                runtime.goal().goalId(),
                workItem.workItemId(),
                workItem.taskRevision(),
                workItem.snapshotId(),
                workItem.logicalRunId(),
                workItem.workMessage().correlationId());
        return RuntimeEvent.create(
                result.occurredAt(),
                binding,
                agentRunId,
                Optional.of(result.messageId()),
                EVENT_PRODUCER_ID,
                new RuntimeEventDetail.VerificationRecorded(
                        payload.verificationStatus()),
                List.of(
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.RESULT_MESSAGE,
                                "result-message/" + result.messageId(),
                                Optional.empty()),
                        new RuntimeEventReference(
                                RuntimeEventReferenceKind.RUN_RECORD,
                                runRecordReference,
                                Optional.empty())));
    }

    private RuntimeEvent workItemTerminatedEvent(
            DurableAgentRuntime runtime,
            RuntimeAgentRun run,
            WorkItemDisposition disposition) {
        WorkItem workItem = runtime.goal().workItem();
        MessageEnvelope result = run.resultMessage().orElseThrow(() ->
                new IllegalStateException(
                        "work termination event requires a durable Result message"));
        RuntimeEventBinding binding = new RuntimeEventBinding(
                runtime.goal().goalId(),
                workItem.workItemId(),
                workItem.taskRevision(),
                workItem.snapshotId(),
                workItem.logicalRunId(),
                workItem.workMessage().correlationId());
        return RuntimeEvent.create(
                clock.instant(),
                binding,
                run.agentRunId(),
                Optional.of(result.messageId()),
                EVENT_PRODUCER_ID,
                new RuntimeEventDetail.WorkItemTerminated(disposition),
                List.of(new RuntimeEventReference(
                        RuntimeEventReferenceKind.SCHEDULER_QUEUE,
                        "scheduler-queue/"
                                + queue.queueId()
                                + "/work-item/"
                                + workItem.workItemId()
                                + "/disposition/"
                                + disposition.name(),
                        Optional.empty())));
    }

    private void requireQueueDisposition(
            String workItemId,
            WorkItemDisposition disposition) {
        boolean recorded = disposition == WorkItemDisposition.VERIFIED_COMPLETED
                ? queue.completedWorkItemIds().contains(workItemId)
                : queue.failedWorkItemIds().contains(workItemId);
        if (!recorded) {
            throw new IllegalStateException(
                    "terminal WorkItem disposition is not durable in the Scheduler queue");
        }
    }

    private MessageEnvelope buildResultEnvelope(
            WorkItem workItem,
            String agentRunId,
            String runRecordReference,
            VerificationStatus status) {
        MessageEnvelope work = workItem.workMessage();
        String messageId = UUID.nameUUIDFromBytes(
                ("agent-run-result:" + agentRunId).getBytes(StandardCharsets.UTF_8))
                .toString();
        return new MessageEnvelope(
                messageId,
                work.correlationId(),
                Optional.of(work.messageId()),
                work.logicalRunId(),
                "agent-run-finalizer",
                clock.instant(),
                new ResultPayload(
                        workItem.taskRevision().taskId(),
                        runRecordReference,
                        status));
    }

    private WorkItemDisposition applyQueueDisposition(
            String workItemId,
            WorkItemDisposition disposition) throws IOException {
        if (queue.completedWorkItemIds().contains(workItemId)
                || queue.failedWorkItemIds().contains(workItemId)) {
            return disposition;  // disposition already recorded (idempotent)
        }
        // The durable queue's recovery contract requeues in-flight work to pending; re-claim it so
        // it is the active work item before recording the terminal disposition.
        if (queue.activeWork().isEmpty()) {
            queue.claimNext();
        }
        Optional<WorkItem> active = queue.activeWork();
        if (active.isPresent() && active.orElseThrow().workItemId().equals(workItemId)) {
            if (disposition == WorkItemDisposition.VERIFIED_COMPLETED) {
                queue.completeActiveVerified(workItemId);
            } else {
                queue.failActive(workItemId);
            }
        }
        return disposition;
    }
}
