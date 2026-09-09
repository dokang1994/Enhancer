package com.enhancer.runtime;

import com.enhancer.context.ProjectContextReader;
import com.enhancer.loop.ApprovedTaskReader;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.workspace.RepositoryMemorySnapshotCollector;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

/**
 * Closed filesystem composition for one deterministic-fake typed ModelWork submission.
 *
 * <p>The caller supplies only the three storage roots and the eight semantic request values.
 * Capability, derived identities, time, governed context, and snapshot capture remain internal.
 */
public final class FileSystemDeterministicFakeModelSubmission {
    private final Submission submission;

    public FileSystemDeterministicFakeModelSubmission(
            Path projectRoot,
            Path submissionRoot,
            Path queueRoot) {
        this(productionSubmission(projectRoot, submissionRoot, queueRoot));
    }

    FileSystemDeterministicFakeModelSubmission(Submission submission) {
        this.submission = Objects.requireNonNull(
                submission, "submission must not be null");
    }

    public DurableSubmissionResult submit(
            String submissionId,
            String taskId,
            String producer,
            String targetPath,
            String expectedResponseSha256,
            ModelExecutionProfile executionProfile,
            int maxWorkItems,
            SchedulerPriority priority) throws IOException {
        DeterministicFakeModelSubmissionRequest request =
                new DeterministicFakeModelSubmissionRequest(
                        submissionId,
                        taskId,
                        producer,
                        targetPath,
                        expectedResponseSha256,
                        executionProfile,
                        maxWorkItems,
                        priority);
        try {
            return submission.submit(request);
        } catch (InvalidDeterministicFakeModelSubmissionConfigurationException exception) {
            throw new IllegalArgumentException(
                    "deterministic-fake model submission project configuration is invalid",
                    exception);
        }
    }

    private static Submission productionSubmission(
            Path projectRoot,
            Path submissionRoot,
            Path queueRoot) {
        DeterministicFakeModelSubmissionService service =
                new DeterministicFakeModelSubmissionService(
                        projectRoot,
                        new FileSystemSubmissionManifestStore(submissionRoot),
                        new FileSystemSchedulerQueueStore(queueRoot),
                        Clock.systemUTC(),
                        new ProjectContextReader(),
                        new ApprovedTaskReader(),
                        new RepositoryMemorySnapshotCollector());
        return service::submit;
    }

    @FunctionalInterface
    interface Submission {
        DurableSubmissionResult submit(DeterministicFakeModelSubmissionRequest request)
                throws IOException;
    }
}
