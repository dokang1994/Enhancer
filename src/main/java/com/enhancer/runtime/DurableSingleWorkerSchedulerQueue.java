package com.enhancer.runtime;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Persist-before-exposure wrapper for the deterministic single-worker queue.
 */
public final class DurableSingleWorkerSchedulerQueue {
    private final String queueId;
    private final SchedulerQueueStore store;
    private SingleWorkerSchedulerQueue queue;
    private long revision;

    private DurableSingleWorkerSchedulerQueue(
            String queueId,
            SchedulerQueueStore store,
            SingleWorkerSchedulerQueue queue,
            long revision) {
        this.queueId = queueId;
        this.store = store;
        this.queue = queue;
        this.revision = revision;
    }

    public static DurableSingleWorkerSchedulerQueue create(
            String queueId,
            int maxWorkItems,
            SchedulerQueueStore store) throws IOException {
        Objects.requireNonNull(store, "store must not be null");
        SchedulerQueueState initial =
                SchedulerQueueState.initial(queueId, maxWorkItems);
        store.create(initial);
        return new DurableSingleWorkerSchedulerQueue(
                initial.queueId(),
                store,
                new SingleWorkerSchedulerQueue(initial),
                initial.revision());
    }

    public static DurableSingleWorkerSchedulerQueue recover(
            String queueId,
            SchedulerQueueStore store) throws IOException {
        Objects.requireNonNull(store, "store must not be null");
        String canonicalQueueId =
                SchedulerQueueState.requireCanonicalQueueId(queueId);
        SchedulerQueueState state = store.resolve(canonicalQueueId);
        SingleWorkerSchedulerQueue recovered =
                new SingleWorkerSchedulerQueue(state);
        long revision = state.revision();
        if (recovered.requeueActiveForRecovery()) {
            SchedulerQueueState recoveryState =
                    recovered.snapshot(canonicalQueueId, revision + 1);
            store.update(recoveryState);
            revision = recoveryState.revision();
        }
        return new DurableSingleWorkerSchedulerQueue(
                canonicalQueueId,
                store,
                recovered,
                revision);
    }

    public void enqueue(QueuedWork queuedWork) throws IOException {
        SingleWorkerSchedulerQueue candidate = copyQueue();
        candidate.enqueue(queuedWork);
        adoptAfterPersistence(candidate);
    }

    public boolean admitIdempotently(QueuedWork queuedWork)
            throws IOException {
        Objects.requireNonNull(queuedWork, "queuedWork must not be null");
        Optional<QueuedWork> existing = queue.admittedWork(
                queuedWork.workItem().workItemId());
        if (existing.isPresent()) {
            if (existing.orElseThrow().equals(queuedWork)) {
                return false;
            }
            throw new IllegalArgumentException(
                    "work item identity was admitted with different content");
        }
        SingleWorkerSchedulerQueue candidate = copyQueue();
        candidate.enqueue(queuedWork);
        adoptAfterPersistence(candidate);
        return true;
    }

    public Optional<WorkItem> claimNext() throws IOException {
        SingleWorkerSchedulerQueue candidate = copyQueue();
        Optional<WorkItem> claimed = candidate.claimNext();
        if (claimed.isEmpty()) {
            return Optional.empty();
        }
        adoptAfterPersistence(candidate);
        return claimed;
    }

    public void completeActiveVerified(String workItemId) throws IOException {
        SingleWorkerSchedulerQueue candidate = copyQueue();
        candidate.completeActiveVerified(workItemId);
        adoptAfterPersistence(candidate);
    }

    public void failActive(String workItemId) throws IOException {
        SingleWorkerSchedulerQueue candidate = copyQueue();
        candidate.failActive(workItemId);
        adoptAfterPersistence(candidate);
    }

    public String queueId() {
        return queueId;
    }

    public long revision() {
        return revision;
    }

    public Optional<WorkItem> activeWork() {
        return queue.activeWork();
    }

    public int pendingCount() {
        return queue.pendingCount();
    }

    public Optional<String> logicalRunId() {
        return queue.logicalRunId();
    }

    public Set<String> completedWorkItemIds() {
        return queue.completedWorkItemIds();
    }

    public Set<String> failedWorkItemIds() {
        return queue.failedWorkItemIds();
    }

    private SingleWorkerSchedulerQueue copyQueue() {
        return new SingleWorkerSchedulerQueue(
                queue.snapshot(queueId, revision));
    }

    private void adoptAfterPersistence(
            SingleWorkerSchedulerQueue candidate) throws IOException {
        SchedulerQueueState next =
                candidate.snapshot(queueId, revision + 1);
        store.update(next);
        queue = candidate;
        revision = next.revision();
    }
}
