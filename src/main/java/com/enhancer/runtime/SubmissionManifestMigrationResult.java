package com.enhancer.runtime;

/** Stable outcome of an explicit submission-manifest schema migration. */
public enum SubmissionManifestMigrationResult {
    ABSENT,
    ALREADY_CURRENT,
    MIGRATED
}
