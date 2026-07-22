package com.enhancer.runtime;

import java.io.IOException;

/** Durable immutable boundary for exact end-user submission intent. */
public interface SubmissionManifestStore {
    boolean storeIdempotently(DurableSubmissionManifest manifest) throws IOException;

    DurableSubmissionManifest resolve(String submissionId) throws IOException;
}
