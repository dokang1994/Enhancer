package com.enhancer.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable schema-versioned state needed to reconstruct one run-scoped Scheduler queue.
 */
public final class SchedulerQueueState {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private final int schemaVersion;
    private final String queueId;
    private final long revision;
    private final int maxWorkItems;
    private final Optional<String> logicalRunId;
    private final List<String> admissionOrder;
    private final List<QueuedWork> pendingWork;
    private final Optional<QueuedWork> activeWork;
    private final Set<String> completedWorkItemIds;
    private final Set<String> failedWorkItemIds;

    SchedulerQueueState(
            int schemaVersion,
            String queueId,
            long revision,
            int maxWorkItems,
            Optional<String> logicalRunId,
            List<String> admissionOrder,
            List<QueuedWork> pendingWork,
            Optional<QueuedWork> activeWork,
            Set<String> completedWorkItemIds,
            Set<String> failedWorkItemIds) {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Scheduler queue schema version is unsupported");
        }
        this.schemaVersion = schemaVersion;
        this.queueId = requireCanonicalQueueId(queueId);
        if (revision < 0) {
            throw new IllegalArgumentException(
                    "revision must not be negative");
        }
        this.revision = revision;
        if (maxWorkItems < 1
                || maxWorkItems > SingleWorkerSchedulerQueue.MAX_WORK_ITEMS) {
            throw new IllegalArgumentException(
                    "maxWorkItems must be between 1 and "
                            + SingleWorkerSchedulerQueue.MAX_WORK_ITEMS);
        }
        this.maxWorkItems = maxWorkItems;
        Objects.requireNonNull(logicalRunId, "logicalRunId must not be null");
        this.logicalRunId = logicalRunId.map(value -> {
            if (value.isBlank()) {
                throw new IllegalArgumentException(
                        "logicalRunId must not be blank");
            }
            return value;
        });
        this.admissionOrder = canonicalIdentityList(
                admissionOrder,
                "admissionOrder",
                maxWorkItems);
        this.pendingWork = immutableWorkList(
                pendingWork,
                "pendingWork",
                maxWorkItems);
        Objects.requireNonNull(activeWork, "activeWork must not be null");
        this.activeWork = activeWork;
        this.completedWorkItemIds = canonicalIdentitySet(
                completedWorkItemIds,
                "completedWorkItemIds",
                maxWorkItems);
        this.failedWorkItemIds = canonicalIdentitySet(
                failedWorkItemIds,
                "failedWorkItemIds",
                maxWorkItems);
        validateStructure();
    }

    public static SchedulerQueueState initial(
            String queueId,
            int maxWorkItems) {
        return new SchedulerQueueState(
                CURRENT_SCHEMA_VERSION,
                queueId,
                0,
                maxWorkItems,
                Optional.empty(),
                List.of(),
                List.of(),
                Optional.empty(),
                Set.of(),
                Set.of());
    }

    public int schemaVersion() {
        return schemaVersion;
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

    public Optional<String> logicalRunId() {
        return logicalRunId;
    }

    public List<String> admissionOrder() {
        return admissionOrder;
    }

    public List<QueuedWork> pendingWork() {
        return pendingWork;
    }

    public Optional<QueuedWork> activeWork() {
        return activeWork;
    }

    public Set<String> completedWorkItemIds() {
        return completedWorkItemIds;
    }

    public Set<String> failedWorkItemIds() {
        return failedWorkItemIds;
    }

    static String requireCanonicalQueueId(String value) {
        Objects.requireNonNull(value, "queueId must not be null");
        try {
            if (!UUID.fromString(value).toString().equals(value)) {
                throw new IllegalArgumentException(
                        "queueId must be a canonical UUID");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "queueId must be a canonical UUID",
                    exception);
        }
        return value;
    }

    private void validateStructure() {
        Map<String, Integer> positions = new HashMap<>();
        for (int index = 0; index < admissionOrder.size(); index++) {
            positions.put(admissionOrder.get(index), index);
        }

        Set<String> statusIds = new LinkedHashSet<>();
        for (String completed : completedWorkItemIds) {
            requireAdmitted(positions, completed);
            statusIds.add(completed);
        }
        for (String failed : failedWorkItemIds) {
            requireAdmitted(positions, failed);
            if (!statusIds.add(failed)) {
                throw new IllegalArgumentException(
                        "work item must not be both completed and failed");
            }
        }
        int previousPendingPosition = -1;
        for (QueuedWork pending : pendingWork) {
            validateWork(pending, positions, statusIds);
            int pendingPosition =
                    positions.get(pending.workItem().workItemId());
            if (pendingPosition <= previousPendingPosition) {
                throw new IllegalArgumentException(
                        "pendingWork must preserve admission order");
            }
            previousPendingPosition = pendingPosition;
        }
        activeWork.ifPresent(active -> {
            validateWork(active, positions, statusIds);
            if (!completedWorkItemIds.containsAll(
                    active.dependencyWorkItemIds())) {
                throw new IllegalArgumentException(
                        "active work dependencies must be completed");
            }
        });
        if (!statusIds.equals(new LinkedHashSet<>(admissionOrder))) {
            throw new IllegalArgumentException(
                    "queue statuses must partition admissionOrder");
        }
        if (admissionOrder.isEmpty() != logicalRunId.isEmpty()) {
            throw new IllegalArgumentException(
                    "logicalRunId presence must match admitted work");
        }
    }

    private void validateWork(
            QueuedWork queuedWork,
            Map<String, Integer> positions,
            Set<String> statusIds) {
        String workItemId = queuedWork.workItem().workItemId();
        requireAdmitted(positions, workItemId);
        if (!statusIds.add(workItemId)) {
            throw new IllegalArgumentException(
                    "work item has more than one queue status");
        }
        String expectedRun = logicalRunId.orElseThrow(() ->
                new IllegalArgumentException(
                        "admitted work requires logicalRunId"));
        if (!expectedRun.equals(queuedWork.workItem().logicalRunId())) {
            throw new IllegalArgumentException(
                    "work item logical run does not match queue");
        }
        int workPosition = positions.get(workItemId);
        for (String dependency : queuedWork.dependencyWorkItemIds()) {
            Integer dependencyPosition = positions.get(dependency);
            if (dependencyPosition == null
                    || dependencyPosition >= workPosition) {
                throw new IllegalArgumentException(
                        "dependency must precede dependent admission");
            }
        }
    }

    private static void requireAdmitted(
            Map<String, Integer> positions,
            String workItemId) {
        if (!positions.containsKey(workItemId)) {
            throw new IllegalArgumentException(
                    "queue status references unadmitted work");
        }
    }

    private static List<String> canonicalIdentityList(
            List<String> values,
            String field,
            int maximum) {
        Objects.requireNonNull(values, field + " must not be null");
        if (values.size() > maximum) {
            throw new IllegalArgumentException(
                    field + " exceeds queue capacity");
        }
        List<String> copy = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            String canonical = canonicalUuid(value, field);
            if (!unique.add(canonical)) {
                throw new IllegalArgumentException(
                        field + " must not contain duplicates");
            }
            copy.add(canonical);
        }
        return List.copyOf(copy);
    }

    private static Set<String> canonicalIdentitySet(
            Set<String> values,
            String field,
            int maximum) {
        Objects.requireNonNull(values, field + " must not be null");
        if (values.size() > maximum) {
            throw new IllegalArgumentException(
                    field + " exceeds queue capacity");
        }
        LinkedHashSet<String> copy = new LinkedHashSet<>();
        for (String value : values) {
            if (!copy.add(canonicalUuid(value, field))) {
                throw new IllegalArgumentException(
                        field + " must not contain duplicates");
            }
        }
        return Collections.unmodifiableSet(copy);
    }

    private static List<QueuedWork> immutableWorkList(
            List<QueuedWork> values,
            String field,
            int maximum) {
        Objects.requireNonNull(values, field + " must not be null");
        if (values.size() > maximum) {
            throw new IllegalArgumentException(
                    field + " exceeds queue capacity");
        }
        List<QueuedWork> copy = new ArrayList<>();
        for (QueuedWork value : values) {
            copy.add(Objects.requireNonNull(
                    value, field + " must not contain null"));
        }
        return List.copyOf(copy);
    }

    private static String canonicalUuid(String value, String field) {
        Objects.requireNonNull(value, field + " must not contain null");
        try {
            if (!UUID.fromString(value).toString().equals(value)) {
                throw new IllegalArgumentException(
                        field + " must contain canonical UUIDs");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    field + " must contain canonical UUIDs",
                    exception);
        }
        return value;
    }
}
