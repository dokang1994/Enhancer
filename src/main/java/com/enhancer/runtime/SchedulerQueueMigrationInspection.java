package com.enhancer.runtime;

import java.util.Objects;

record SchedulerQueueMigrationInspection(
        int sourceSchemaVersion,
        SchedulerQueueState state,
        byte[] sourceBytes,
        boolean alreadyCurrent) {

    SchedulerQueueMigrationInspection {
        if (sourceSchemaVersion
                        < SchedulerQueueState.LEGACY_MIGRATION_SOURCE_SCHEMA_VERSION
                || sourceSchemaVersion > SchedulerQueueState.CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "sourceSchemaVersion must be a supported migration schema");
        }
        state = Objects.requireNonNull(state, "state must not be null");
        if (state.schemaVersion() != SchedulerQueueState.CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "migration inspection state must use the current schema");
        }
        sourceBytes = Objects.requireNonNull(
                sourceBytes, "sourceBytes must not be null").clone();
        if (alreadyCurrent
                != (sourceSchemaVersion
                        == SchedulerQueueState.CURRENT_SCHEMA_VERSION)) {
            throw new IllegalArgumentException(
                    "alreadyCurrent must match sourceSchemaVersion");
        }
    }

    @Override
    public byte[] sourceBytes() {
        return sourceBytes.clone();
    }
}
