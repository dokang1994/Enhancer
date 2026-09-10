package com.enhancer.runtime;

import com.enhancer.context.ProjectContextReader;
import com.enhancer.loop.ApprovedTaskReader;
import com.enhancer.workspace.RepositoryMemorySnapshotCollector;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

/**
 * Internal manifest-first source for deterministic-fake typed model work.
 *
 * <p>The request retains caller intent but no capability authority. This boundary obtains its
 * sole capability from the closed repository-owned source, persists through the unchanged
 * durable submission service, and never consults current repository state during exact replay.
 */
final class DeterministicFakeModelSubmissionService {
    private final DeterministicFakeModelSubmissionManifestPreparer preparer;
    private final DurableWorkSubmissionService submissionService;

    DeterministicFakeModelSubmissionService(
            Path projectRoot,
            SubmissionManifestStore manifestStore,
            SchedulerQueueStore queueStore,
            Clock clock,
            ProjectContextReader contextReader,
            ApprovedTaskReader taskReader,
            RepositoryMemorySnapshotCollector snapshotCollector) {
        SubmissionManifestStore requiredManifestStore = Objects.requireNonNull(
                manifestStore, "manifestStore must not be null");
        this.preparer = new DeterministicFakeModelSubmissionManifestPreparer(
                projectRoot,
                requiredManifestStore,
                clock,
                contextReader,
                taskReader,
                snapshotCollector);
        this.submissionService = new DurableWorkSubmissionService(
                requiredManifestStore,
                Objects.requireNonNull(queueStore, "queueStore must not be null"));
    }

    DurableSubmissionResult submit(DeterministicFakeModelSubmissionRequest request)
            throws IOException {
        PreparedDeterministicFakeModelSubmission prepared = preparer.prepare(request);
        return submissionService.submitPersisted(
                prepared.manifest(), prepared.manifestCreated());
    }
}
