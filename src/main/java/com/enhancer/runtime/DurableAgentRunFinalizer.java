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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Connects a resolved RunRecord to the terminal AgentRun/Goal state and the matching Scheduler
 * queue disposition, in a recoverable, idempotent order. Queue and runtime remain separate durable
 * boundaries; no cross-store transaction is claimed. Consumer: the future Scheduler worker/driver.
 */
public final class DurableAgentRunFinalizer {
    private final DurableSingleWorkerSchedulerQueue queue;
    private final AgentRuntimeStateStore runtimeStore;
    private final RunRecordStore runRecordStore;
    private final Clock clock;

    public DurableAgentRunFinalizer(
            DurableSingleWorkerSchedulerQueue queue,
            AgentRuntimeStateStore runtimeStore,
            RunRecordStore runRecordStore,
            Clock clock) {
        this.queue = Objects.requireNonNull(queue, "queue must not be null");
        this.runtimeStore =
                Objects.requireNonNull(runtimeStore, "runtimeStore must not be null");
        this.runRecordStore =
                Objects.requireNonNull(runRecordStore, "runRecordStore must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public WorkItemDisposition finalizeAgentRun(
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
        return applyQueueDisposition(
                workItem.workItemId(),
                runtime.agentRun().orElseThrow().status());
    }

    public Optional<WorkItemDisposition> recoverFinalization(String goalId)
            throws IOException {
        DurableAgentRuntime runtime =
                DurableAgentRuntime.recover(goalId, runtimeStore, clock);
        RuntimeAgentRun run = runtime.agentRun().orElseThrow(() ->
                new IllegalStateException("no AgentRun exists"));
        if (!run.status().isTerminal()) {
            return Optional.empty();
        }
        return Optional.of(applyQueueDisposition(
                runtime.goal().workItem().workItemId(),
                run.status()));
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
            RuntimeAgentRunStatus terminalStatus) throws IOException {
        WorkItemDisposition disposition =
                terminalStatus == RuntimeAgentRunStatus.COMPLETED
                        ? WorkItemDisposition.VERIFIED_COMPLETED
                        : WorkItemDisposition.FAILED;
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
