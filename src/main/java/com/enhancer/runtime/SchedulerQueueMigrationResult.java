package com.enhancer.runtime;

/**
 * Stable outcome of the explicit Scheduler queue schema migration.
 */
public enum SchedulerQueueMigrationResult {
    ABSENT,
    ALREADY_CURRENT,
    MIGRATED
}
