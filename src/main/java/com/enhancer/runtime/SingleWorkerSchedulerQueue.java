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
    private final Set<String> admittedWorkItemIds = new LinkedHashSet<>();
    private final Set<String> completedWorkItemIds = new LinkedHashSet<>();
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
        this.admittedWorkItemIds.addAll(state.admissionOrder());
        for (QueuedWork queuedWork : state.pendingWork()) {
            pending.put(
                    queuedWork.workItem().workItemId(),
                    queuedWork);
        }
        this.completedWorkItemIds.addAll(
                state.completedWorkItemIds());
        this.active = state.activeWork().orElse(null);
    }

    public void enqueue(QueuedWork queuedWork) {
        Objects.requireNonNull(queuedWork, "queuedWork must not be null");
        String workItemId = queuedWork.workItem().workItemId();
        if (admittedWorkItemIds.contains(workItemId)) {
            throw new IllegalArgumentException(
                    "work item identity has already been admitted");
        }
        for (String dependency : queuedWork.dependencyWorkItemIds()) {
            if (!admittedWorkItemIds.contains(dependency)) {
                throw new IllegalArgumentException(
                        "dependency must already be admitted: " + dependency);
            }
        }
        if (admittedWorkItemIds.size() >= maxWorkItems) {
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
        admittedWorkItemIds.add(workItemId);
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

    public void completeActive(String workItemId) {
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
                List.copyOf(admittedWorkItemIds),
                List.copyOf(pending.values()),
                Optional.ofNullable(active),
                completedWorkItemIds);
    }

    boolean requeueActiveForRecovery() {
        if (active == null) {
            return false;
        }
        Map<String, QueuedWork> recoverable = new LinkedHashMap<>(pending);
        recoverable.put(active.workItem().workItemId(), active);
        pending.clear();
        for (String workItemId : admittedWorkItemIds) {
            QueuedWork queuedWork = recoverable.get(workItemId);
            if (queuedWork != null) {
                pending.put(workItemId, queuedWork);
            }
        }
        active = null;
        return true;
    }
}
