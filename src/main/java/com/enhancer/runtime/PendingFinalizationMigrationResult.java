package com.enhancer.runtime;

/**
 * Stable outcome of the explicit pending-finalization schema migration.
 */
public enum PendingFinalizationMigrationResult {
    ABSENT,
    ALREADY_CURRENT,
    MIGRATED
}
