package com.enhancer.runtime;

/**
 * Terminal disposition of a Scheduler queue work item. Only a verified completion satisfies a
 * dependent's dependency; a failure is terminal and never satisfies dependencies.
 */
public enum WorkItemDisposition {
    VERIFIED_COMPLETED,
    FAILED;

    public boolean satisfiesDependencies() {
        return this == VERIFIED_COMPLETED;
    }
}
