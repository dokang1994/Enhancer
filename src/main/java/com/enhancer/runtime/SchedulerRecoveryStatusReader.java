package com.enhancer.runtime;

import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.run.RunRecordStore;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Takes a bounded stable sequential sample of the independent recovery stores.
 */
public final class SchedulerRecoveryStatusReader {
    private final SchedulerQueueStore queueStore;
    private final AgentRuntimeStateStore runtimeStore;
    private final PendingFinalizationStore checkpointStore;
    private final RunRecordStore runRecordStore;

    public SchedulerRecoveryStatusReader(
            SchedulerQueueStore queueStore,
            AgentRuntimeStateStore runtimeStore,
            PendingFinalizationStore checkpointStore,
            RunRecordStore runRecordStore) {
        this.queueStore = Objects.requireNonNull(
                queueStore, "queueStore must not be null");
        this.runtimeStore = Objects.requireNonNull(
                runtimeStore, "runtimeStore must not be null");
        this.checkpointStore = Objects.requireNonNull(
                checkpointStore, "checkpointStore must not be null");
        this.runRecordStore = Objects.requireNonNull(
                runRecordStore, "runRecordStore must not be null");
    }

    public SchedulerRecoveryStatus read(String queueId)
            throws IOException {
        SchedulerQueueState firstQueue =
                queueStore.resolve(queueId);
        Optional<PendingFinalization> firstCheckpoint =
                checkpointStore.findPending();
        Optional<AgentRuntimeState> firstRuntime =
                resolveRuntime(firstCheckpoint);
        Optional<ResolvedRunRecord> runRecord =
                resolveRunRecord(firstCheckpoint);

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

        return SchedulerRecoveryStatus.project(
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

    private Optional<ResolvedRunRecord> resolveRunRecord(
            Optional<PendingFinalization> checkpoint)
            throws IOException {
        if (checkpoint.isEmpty()
                || checkpoint.orElseThrow()
                        .runRecordReference().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(runRecordStore.resolve(
                checkpoint.orElseThrow()
                        .runRecordReference().orElseThrow()));
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
