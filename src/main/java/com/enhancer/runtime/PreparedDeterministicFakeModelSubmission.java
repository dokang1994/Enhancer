package com.enhancer.runtime;

import java.util.Objects;

record PreparedDeterministicFakeModelSubmission(
        DurableSubmissionManifest manifest,
        boolean manifestCreated) {
    PreparedDeterministicFakeModelSubmission {
        Objects.requireNonNull(manifest, "manifest must not be null");
    }
}
