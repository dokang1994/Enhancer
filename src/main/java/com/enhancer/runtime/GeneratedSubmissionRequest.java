package com.enhancer.runtime;

import java.util.Objects;

/**
 * Caller-retained intent for one generated-input Scheduler submission. The caller keeps only the
 * canonical submission UUID and the facts it owns; queue, correlation, and logical-run identities
 * plus the occurrence time are generated from the submission UUID and the clock, so the operator
 * no longer preserves the entire replay tuple.
 *
 * <p>Every field is caller-owned intent that must remain stable across replays. A stored manifest
 * whose recorded value disagrees with any of these fails closed rather than admitting new work.
 */
public record GeneratedSubmissionRequest(
        String submissionId,
        int maxWorkItems,
        String requiredCapability,
        String producer,
        String taskId,
        String targetPath,
        String expectedSha256) {

    public GeneratedSubmissionRequest {
        submissionId = GeneratedSubmissionIdentities.canonicalSubmissionId(submissionId);
        Objects.requireNonNull(requiredCapability, "requiredCapability must not be null");
        Objects.requireNonNull(producer, "producer must not be null");
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(targetPath, "targetPath must not be null");
        Objects.requireNonNull(expectedSha256, "expectedSha256 must not be null");
    }
}
