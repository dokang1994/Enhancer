package com.enhancer.runtime;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Deterministic run-scoped in-memory queue with one active slot. This type does not persist,
 * lease, execute, retry, cancel, or recover work.
 */
public final class SingleWorkerSchedulerQueue {
    public static final int MAX_WORK_ITEMS = 4096;

    private final int maxWorkItems;
    private final Map<String, QueuedWork> pending = new LinkedHashMap<>();
    private final Map<String, QueuedWork> admittedWork = new LinkedHashMap<>();
    private final Set<String> completedWorkItemIds = new LinkedHashSet<>();
    private final Set<String> failedWorkItemIds = new LinkedHashSet<>();
    private String logicalRunId;
    private QueuedWork active;

    public SingleWorkerSchedulerQueue() {
        this(MAX_WORK_ITEMS);
    }

    public SingleWorkerSchedulerQueue(int maxWorkItems) {
        if (maxWorkItems < 1 || maxWorkItems > MAX_WORK_ITEMS) {
            throw new IllegalArgumentException(
                    "maxWorkItems must be between 1 and " + MAX_WORK_ITEMS);
        }
        this.maxWorkItems = maxWorkItems;
    }

    SingleWorkerSchedulerQueue(SchedulerQueueState state) {
        Objects.requireNonNull(state, "state must not be null");
        this.maxWorkItems = state.maxWorkItems();
        this.logicalRunId = state.logicalRunId().orElse(null);
        for (QueuedWork queuedWork : state.admittedWork()) {
            admittedWork.put(
                    queuedWork.workItem().workItemId(),
                    queuedWork);
        }
        for (QueuedWork queuedWork : state.pendingWork()) {
            pending.put(
                    queuedWork.workItem().workItemId(),
                    queuedWork);
        }
        this.completedWorkItemIds.addAll(
                state.completedWorkItemIds());
        this.failedWorkItemIds.addAll(
                state.failedWorkItemIds());
        this.active = state.activeWork().orElse(null);
    }

    public void enqueue(QueuedWork queuedWork) {
        Objects.requireNonNull(queuedWork, "queuedWork must not be null");
        String workItemId = queuedWork.workItem().workItemId();
        if (admittedWork.containsKey(workItemId)) {
            throw new IllegalArgumentException(
                    "work item identity has already been admitted");
        }
        for (String dependency : queuedWork.dependencyWorkItemIds()) {
            if (!admittedWork.containsKey(dependency)) {
                throw new IllegalArgumentException(
                        "dependency must already be admitted: " + dependency);
            }
        }
        if (admittedWork.size() >= maxWorkItems) {
            throw new IllegalStateException(
                    "Scheduler queue work-item capacity is exhausted");
        }
        String workLogicalRunId = queuedWork.workItem().logicalRunId();
        if (logicalRunId != null
                && !logicalRunId.equals(workLogicalRunId)) {
            throw new IllegalArgumentException(
                    "work item logical run does not match queue");
        }
        if (logicalRunId == null) {
            logicalRunId = workLogicalRunId;
        }
        admittedWork.put(workItemId, queuedWork);
        pending.put(workItemId, queuedWork);
    }

    public Optional<WorkItem> claimNext() {
        if (active != null) {
            return Optional.empty();
        }
        Iterator<Map.Entry<String, QueuedWork>> iterator =
                pending.entrySet().iterator();
        while (iterator.hasNext()) {
            QueuedWork candidate = iterator.next().getValue();
            if (completedWorkItemIds.containsAll(
                    candidate.dependencyWorkItemIds())) {
                iterator.remove();
                active = candidate;
                return Optional.of(candidate.workItem());
            }
        }
        return Optional.empty();
    }

    public void completeActiveVerified(String workItemId) {
        Objects.requireNonNull(workItemId, "workItemId must not be null");
        if (active == null) {
            throw new IllegalStateException("no active work item exists");
        }
        if (!active.workItem().workItemId().equals(workItemId)) {
            throw new IllegalStateException(
                    "only the active work item may be completed");
        }
        completedWorkItemIds.add(workItemId);
        active = null;
    }

    public void failActive(String workItemId) {
        Objects.requireNonNull(workItemId, "workItemId must not be null");
        if (active == null) {
            throw new IllegalStateException("no active work item exists");
        }
        if (!active.workItem().workItemId().equals(workItemId)) {
            throw new IllegalStateException(
                    "only the active work item may be failed");
        }
        failedWorkItemIds.add(workItemId);
        active = null;
    }

    public Set<String> failedWorkItemIds() {
        return Set.copyOf(failedWorkItemIds);
    }

    public Optional<WorkItemDisposition> dispositionOf(String workItemId) {
        Objects.requireNonNull(workItemId, "workItemId must not be null");
        if (completedWorkItemIds.contains(workItemId)) {
            return Optional.of(WorkItemDisposition.VERIFIED_COMPLETED);
        }
        if (failedWorkItemIds.contains(workItemId)) {
            return Optional.of(WorkItemDisposition.FAILED);
        }
        return Optional.empty();
    }

    public Optional<WorkItem> activeWork() {
        return active == null
                ? Optional.empty()
                : Optional.of(active.workItem());
    }

    public int pendingCount() {
        return pending.size();
    }

    public Optional<String> logicalRunId() {
        return Optional.ofNullable(logicalRunId);
    }

    public Set<String> completedWorkItemIds() {
        return Set.copyOf(completedWorkItemIds);
    }

    SchedulerQueueState snapshot(String queueId, long revision) {
        return new SchedulerQueueState(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                queueId,
                revision,
                maxWorkItems,
                logicalRunId(),
                List.copyOf(admittedWork.keySet()),
                List.copyOf(admittedWork.values()),
                List.copyOf(pending.values()),
                Optional.ofNullable(active),
                completedWorkItemIds,
                failedWorkItemIds);
    }

    boolean requeueActiveForRecovery() {
        if (active == null) {
            return false;
        }
        Map<String, QueuedWork> recoverable = new LinkedHashMap<>(pending);
        recoverable.put(active.workItem().workItemId(), active);
        pending.clear();
        for (String workItemId : admittedWork.keySet()) {
            QueuedWork queuedWork = recoverable.get(workItemId);
            if (queuedWork != null) {
                pending.put(workItemId, queuedWork);
            }
        }
        active = null;
        return true;
    }

    Optional<QueuedWork> admittedWork(String workItemId) {
        Objects.requireNonNull(workItemId, "workItemId must not be null");
        return Optional.ofNullable(admittedWork.get(workItemId));
    }
}
