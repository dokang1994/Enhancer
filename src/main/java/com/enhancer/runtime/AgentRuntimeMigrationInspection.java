package com.enhancer.runtime;

import java.util.Objects;

record AgentRuntimeMigrationInspection(
        int sourceSchemaVersion,
        AgentRuntimeState state,
        byte[] sourceBytes,
        boolean alreadyCurrent) {

    AgentRuntimeMigrationInspection {
        if (sourceSchemaVersion != AgentRuntimeState.PREVIOUS_SCHEMA_VERSION
                && sourceSchemaVersion != AgentRuntimeState.CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "sourceSchemaVersion must be a supported migration schema");
        }
        state = Objects.requireNonNull(state, "state must not be null");
        if (state.schemaVersion() != AgentRuntimeState.CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "migration inspection state must use the current schema");
        }
        sourceBytes = Objects.requireNonNull(
                sourceBytes, "sourceBytes must not be null").clone();
        if (alreadyCurrent
                != (sourceSchemaVersion
                        == AgentRuntimeState.CURRENT_SCHEMA_VERSION)) {
            throw new IllegalArgumentException(
                    "alreadyCurrent must match sourceSchemaVersion");
        }
    }

    @Override
    public byte[] sourceBytes() {
        return sourceBytes.clone();
    }
}
