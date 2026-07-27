package com.enhancer.runtime;

import java.io.IOException;

/** The immutable source changed while an explicit migration was being prepared. */
public final class ConcurrentSubmissionManifestMigrationException
        extends IOException {
    private static final long serialVersionUID = 1L;

    public ConcurrentSubmissionManifestMigrationException(String submissionId) {
        super("submission manifest changed during migration: " + submissionId);
    }
}
