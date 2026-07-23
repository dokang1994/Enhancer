package com.enhancer.runtime;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Read-only admission-ordered projection of one persisted Scheduler queue snapshot.
 */
public final class SchedulerQueueStatus {
    private final String queueId;
    private final long revision;
    private final int maxWorkItems;
    private final List<WorkStatus> workItems;
    private final Map<WorkState, Integer> counts;

    private SchedulerQueueStatus(
            String queueId,
            long revision,
            int maxWorkItems,
            List<WorkStatus> workItems) {
        this.queueId = queueId;
        this.revision = revision;
        this.maxWorkItems = maxWorkItems;
        this.workItems = List.copyOf(workItems);
        EnumMap<WorkState, Integer> projectedCounts =
                new EnumMap<>(WorkState.class);
        for (WorkState state : WorkState.values()) {
            projectedCounts.put(state, 0);
        }
        for (WorkStatus workStatus : workItems) {
            projectedCounts.compute(
                    workStatus.state(),
                    (ignored, count) -> count + 1);
        }
        this.counts = Map.copyOf(projectedCounts);
    }

    public static SchedulerQueueStatus project(SchedulerQueueState state) {
        Objects.requireNonNull(state, "state must not be null");
        Set<String> pending = new HashSet<>();
        for (QueuedWork work : state.pendingWork()) {
            pending.add(work.workItem().workItemId());
        }
        String active = state.activeWork()
                .map(work -> work.workItem().workItemId())
                .orElse(null);
        List<WorkStatus> projected = new ArrayList<>();
        for (QueuedWork admitted : state.admittedWork()) {
            String workItemId = admitted.workItem().workItemId();
            WorkState workState;
            if (state.completedWorkItemIds().contains(workItemId)) {
                workState = WorkState.VERIFIED;
            } else if (state.failedWorkItemIds().contains(workItemId)) {
                workState = WorkState.FAILED;
            } else if (workItemId.equals(active)) {
                workState = WorkState.ACTIVE;
            } else if (pending.contains(workItemId)) {
                workState = state.completedWorkItemIds().containsAll(
                        admitted.dependencyWorkItemIds())
                        ? WorkState.READY
                        : WorkState.BLOCKED;
            } else {
                throw new IllegalArgumentException(
                        "Scheduler queue state contains an unclassified admission");
            }
            projected.add(new WorkStatus(workItemId, workState));
        }
        return new SchedulerQueueStatus(
                state.queueId(),
                state.revision(),
                state.maxWorkItems(),
                projected);
    }

    public String queueId() {
        return queueId;
    }

    public long revision() {
        return revision;
    }

    public int maxWorkItems() {
        return maxWorkItems;
    }

    public List<WorkStatus> workItems() {
        return workItems;
    }

    public int count(WorkState state) {
        Objects.requireNonNull(state, "state must not be null");
        return counts.get(state);
    }

    public enum WorkState {
        READY,
        BLOCKED,
        ACTIVE,
        VERIFIED,
        FAILED
    }

    public record WorkStatus(String workItemId, WorkState state) {
        public WorkStatus {
            Objects.requireNonNull(workItemId, "workItemId must not be null");
            try {
                if (!UUID.fromString(workItemId).toString()
                        .equals(workItemId)) {
                    throw new IllegalArgumentException(
                            "workItemId must be a canonical UUID");
                }
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "workItemId must be a canonical UUID",
                        exception);
            }
            Objects.requireNonNull(state, "state must not be null");
        }
    }
}
