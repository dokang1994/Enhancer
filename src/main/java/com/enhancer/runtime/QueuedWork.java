package com.enhancer.runtime;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable admission metadata for one {@link WorkItem}. Dependencies are work identities from
 * the same run-scoped queue and create no execution or Tool authority.
 */
public final class QueuedWork {
    public static final int MAX_DEPENDENCIES = 256;

    private final WorkItem workItem;
    private final Set<String> dependencyWorkItemIds;

    public QueuedWork(
            WorkItem workItem,
            Collection<String> dependencyWorkItemIds) {
        this.workItem = Objects.requireNonNull(
                workItem, "workItem must not be null");
        Objects.requireNonNull(
                dependencyWorkItemIds,
                "dependencyWorkItemIds must not be null");
        if (dependencyWorkItemIds.size() > MAX_DEPENDENCIES) {
            throw new IllegalArgumentException(
                    "dependencyWorkItemIds must not exceed "
                            + MAX_DEPENDENCIES + " entries");
        }

        LinkedHashSet<String> dependencies = new LinkedHashSet<>();
        for (String dependency : dependencyWorkItemIds) {
            String canonicalDependency = canonicalUuid(dependency);
            if (canonicalDependency.equals(workItem.workItemId())) {
                throw new IllegalArgumentException(
                        "workItem must not depend on itself");
            }
            if (!dependencies.add(canonicalDependency)) {
                throw new IllegalArgumentException(
                        "dependencyWorkItemIds must not contain duplicates");
            }
        }
        this.dependencyWorkItemIds =
                Collections.unmodifiableSet(dependencies);
    }

    public WorkItem workItem() {
        return workItem;
    }

    public Set<String> dependencyWorkItemIds() {
        return dependencyWorkItemIds;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof QueuedWork that)) {
            return false;
        }
        return workItem.equals(that.workItem)
                && dependencyWorkItemIds.equals(that.dependencyWorkItemIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workItem, dependencyWorkItemIds);
    }

    private static String canonicalUuid(String value) {
        Objects.requireNonNull(
                value, "dependencyWorkItemIds must not contain null");
        try {
            if (!UUID.fromString(value).toString().equals(value)) {
                throw new IllegalArgumentException(
                        "dependency work identity must be a canonical UUID");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "dependency work identity must be a canonical UUID",
                    exception);
        }
        return value;
    }
}
