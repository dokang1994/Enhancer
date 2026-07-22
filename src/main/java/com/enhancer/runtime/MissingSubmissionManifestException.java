package com.enhancer.runtime;

import java.io.IOException;

/**
 * Signals that no submission manifest is stored for a submission identity. It is distinct from a
 * corruption failure so a resolve-first caller can treat absence as first use while a corrupt
 * artifact still fails closed.
 */
public final class MissingSubmissionManifestException extends IOException {
    private static final long serialVersionUID = 1L;

    public MissingSubmissionManifestException(String submissionId) {
        super("submission manifest is missing: " + submissionId);
    }
}
