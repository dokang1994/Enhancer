package com.enhancer.runtime;

import com.enhancer.run.ModelRunRecordStore;
import com.enhancer.run.RunRecordStore;
import com.enhancer.tool.EvidenceStore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Takes a bounded stable sequential sample of the independent recovery stores.
 */
public final class SchedulerRecoveryStatusReader {
    private final SchedulerQueueStore queueStore;
    private final AgentRuntimeStateStore runtimeStore;
    private final PendingFinalizationStore checkpointStore;
    private final AgentRunRecordResolver runRecordResolver;

    public SchedulerRecoveryStatusReader(
            SchedulerQueueStore queueStore,
            AgentRuntimeStateStore runtimeStore,
            PendingFinalizationStore checkpointStore,
            RunRecordStore runRecordStore) {
        this(
                queueStore,
                runtimeStore,
                checkpointStore,
                new AgentRunRecordResolver(runRecordStore));
    }

    SchedulerRecoveryStatusReader(
            SchedulerQueueStore queueStore,
            AgentRuntimeStateStore runtimeStore,
            PendingFinalizationStore checkpointStore,
            RunRecordStore runRecordStore,
            ModelRunRecordStore modelRunRecordStore,
            EvidenceStore evidenceStore,
            Path projectRoot,
            ModelProcessExecutionConfiguration configuration) {
        this(
                queueStore,
                runtimeStore,
                checkpointStore,
                new AgentRunRecordResolver(
                        runRecordStore,
                        modelRunRecordStore,
                        evidenceStore,
                        projectRoot,
                        configuration));
    }

    private SchedulerRecoveryStatusReader(
            SchedulerQueueStore queueStore,
            AgentRuntimeStateStore runtimeStore,
            PendingFinalizationStore checkpointStore,
            AgentRunRecordResolver runRecordResolver) {
        this.queueStore = Objects.requireNonNull(
                queueStore, "queueStore must not be null");
        this.runtimeStore = Objects.requireNonNull(
                runtimeStore, "runtimeStore must not be null");
        this.checkpointStore = Objects.requireNonNull(
                checkpointStore, "checkpointStore must not be null");
        this.runRecordResolver = Objects.requireNonNull(
                runRecordResolver, "runRecordResolver must not be null");
    }

    public SchedulerRecoveryStatus read(String queueId)
            throws IOException {
        SchedulerQueueState firstQueue =
                queueStore.resolve(queueId);
        Optional<PendingFinalization> firstCheckpoint =
                checkpointStore.findPending();
        Optional<AgentRuntimeState> firstRuntime =
                resolveRuntime(firstCheckpoint);
        Optional<AgentRunRecordResolver.Resolved> runRecord =
                resolveRunRecord(firstCheckpoint, firstRuntime);

        SchedulerQueueState secondQueue =
                queueStore.resolve(queueId);
        Optional<PendingFinalization> secondCheckpoint =
                checkpointStore.findPending();
        requireStableQueue(firstQueue, secondQueue);
        if (!firstCheckpoint.equals(secondCheckpoint)) {
            throw changed("cycle checkpoint differs between samples");
        }
        Optional<AgentRuntimeState> secondRuntime =
                resolveRuntime(secondCheckpoint);
        requireStableRuntime(firstRuntime, secondRuntime);

        return SchedulerRecoveryStatus.projectResolved(
                firstQueue,
                firstCheckpoint,
                firstRuntime,
                runRecord);
    }

    private Optional<AgentRuntimeState> resolveRuntime(
            Optional<PendingFinalization> checkpoint)
            throws IOException {
        if (checkpoint.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(runtimeStore.resolve(
                    checkpoint.orElseThrow().goalId()));
        } catch (MissingAgentRuntimeStateException exception) {
            return Optional.empty();
        }
    }

    private Optional<AgentRunRecordResolver.Resolved> resolveRunRecord(
            Optional<PendingFinalization> checkpoint,
            Optional<AgentRuntimeState> runtime)
            throws IOException {
        if (checkpoint.isEmpty()
                || runtime.isEmpty()
                || checkpoint.orElseThrow()
                        .runRecordReference().isEmpty()) {
            return Optional.empty();
        }
        PendingFinalization pending = checkpoint.orElseThrow();
        return Optional.of(runRecordResolver.resolve(
                pending.goalId(),
                pending.agentRunId(),
                runtime.orElseThrow().goal().workItem(),
                pending.runRecordReference().orElseThrow()));
    }

    private void requireStableQueue(
            SchedulerQueueState first,
            SchedulerQueueState second)
            throws ConcurrentSchedulerRecoveryInspectionException {
        if (!first.queueId().equals(second.queueId())
                || first.revision() != second.revision()) {
            throw changed("queue revision differs between samples");
        }
    }

    private void requireStableRuntime(
            Optional<AgentRuntimeState> first,
            Optional<AgentRuntimeState> second)
            throws ConcurrentSchedulerRecoveryInspectionException {
        if (first.isEmpty() != second.isEmpty()) {
            throw changed(
                    "AgentRuntime presence differs between samples");
        }
        if (first.isEmpty()) {
            return;
        }
        AgentRuntimeState firstState = first.orElseThrow();
        AgentRuntimeState secondState = second.orElseThrow();
        if (!firstState.goal().goalId().equals(
                        secondState.goal().goalId())
                || firstState.revision() != secondState.revision()) {
            throw changed(
                    "AgentRuntime revision differs between samples");
        }
    }

    private ConcurrentSchedulerRecoveryInspectionException changed(
            String reason) {
        return new ConcurrentSchedulerRecoveryInspectionException(
                reason);
    }
}
