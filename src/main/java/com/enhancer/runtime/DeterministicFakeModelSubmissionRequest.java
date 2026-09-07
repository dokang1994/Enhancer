package com.enhancer.runtime;

import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Exact caller-retained intent for one internal deterministic-fake model submission.
 * Derived identities, occurrence time, task scope, and snapshot provenance are deliberately absent.
 */
record DeterministicFakeModelSubmissionRequest(
        String submissionId,
        String taskId,
        String producer,
        String targetPath,
        String expectedResponseSha256,
        ModelExecutionProfile executionProfile,
        int maxWorkItems,
        SchedulerPriority priority) {

    private static final int MAX_PRODUCER_CHARACTERS = 256;

    DeterministicFakeModelSubmissionRequest {
        submissionId = GeneratedSubmissionIdentities.canonicalSubmissionId(submissionId);
        taskId = RuntimeEventContractSupport.bounded(
                taskId,
                "taskId",
                ApprovedTaskRevision.MAX_TASK_ID_CHARACTERS);
        producer = RuntimeEventContractSupport.bounded(
                producer,
                "producer",
                MAX_PRODUCER_CHARACTERS);

        ModelWorkPayload.ModelInvocationExecutionInput validatedInput =
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        targetPath,
                        expectedResponseSha256,
                        executionProfile);
        targetPath = validatedInput.targetPath();
        expectedResponseSha256 = validatedInput.expectedResponseSha256();
        executionProfile = validatedInput.executionProfile();
        Path target = Path.of(targetPath);
        if (target.getRoot() != null || target.isAbsolute()) {
            throw new IllegalArgumentException("targetPath must be relative");
        }
        if (maxWorkItems < 1
                || maxWorkItems > SingleWorkerSchedulerQueue.MAX_WORK_ITEMS) {
            throw new IllegalArgumentException(
                    "maxWorkItems must be between 1 and "
                            + SingleWorkerSchedulerQueue.MAX_WORK_ITEMS);
        }
        Objects.requireNonNull(priority, "priority must not be null");
    }
}
