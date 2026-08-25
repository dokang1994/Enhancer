package com.enhancer.runtime;

public enum CoordinatedDurableMigrationRefusalCode {
    UNMIGRATABLE_LEGACY_MODEL_WORK,
    INVALID_LEGACY_WORK,
    PARTIAL_CLOSURE,
    CROSS_STORE_MISMATCH,
    SOURCE_INVALID,
    STOPPED_OWNER_FENCE_INVALID
}
