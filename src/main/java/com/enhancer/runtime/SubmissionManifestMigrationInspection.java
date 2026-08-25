package com.enhancer.runtime;

import java.util.Objects;

record SubmissionManifestMigrationInspection(
        int sourceSchemaVersion,
        DurableSubmissionManifest manifest,
        byte[] sourceBytes,
        boolean alreadyCurrent) {

    SubmissionManifestMigrationInspection {
        if (sourceSchemaVersion
                        < FileSystemSubmissionManifestStore
                                .LEGACY_MIGRATION_SOURCE_SCHEMA_VERSION
                || sourceSchemaVersion
                        > FileSystemSubmissionManifestStore.CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "sourceSchemaVersion must be a supported migration schema");
        }
        manifest = Objects.requireNonNull(manifest, "manifest must not be null");
        sourceBytes = Objects.requireNonNull(
                sourceBytes, "sourceBytes must not be null").clone();
        if (alreadyCurrent
                != (sourceSchemaVersion
                        == FileSystemSubmissionManifestStore.CURRENT_SCHEMA_VERSION)) {
            throw new IllegalArgumentException(
                    "alreadyCurrent must match sourceSchemaVersion");
        }
    }

    @Override
    public byte[] sourceBytes() {
        return sourceBytes.clone();
    }
}
