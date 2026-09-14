package com.enhancer.runtime;

import com.enhancer.bus.BackpressurePolicy;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.FileSpoolPublicationOutcome;
import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.bus.TransportMessage;
import com.enhancer.context.ProjectContextReader;
import com.enhancer.loop.ApprovedTaskReader;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.workspace.RepositoryMemorySnapshotCollector;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

/**
 * Closed filesystem composition for manifest-first deterministic-fake ModelWork publication.
 *
 * <p>The publisher persists or exact-replays immutable submission intent and then sends only
 * the manifest-derived queue route and envelope through the existing bounded file spool. It has
 * no Scheduler queue dependency and performs no admission or execution.
 */
public final class FileSystemDeterministicFakeModelWorkPublisher {
    private final Publication publication;

    public FileSystemDeterministicFakeModelWorkPublisher(
            Path projectRoot,
            Path submissionRoot,
            Path transportSpoolRoot,
            int maxPendingPublications) {
        this(productionPublication(
                projectRoot,
                submissionRoot,
                transportSpoolRoot,
                BackpressurePolicy.of(maxPendingPublications)));
    }

    FileSystemDeterministicFakeModelWorkPublisher(Publication publication) {
        this.publication = Objects.requireNonNull(
                publication, "publication must not be null");
    }

    public DeterministicFakeModelWorkPublicationResult publish(
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
            return publication.publish(request);
        } catch (InvalidDeterministicFakeModelSubmissionConfigurationException exception) {
            throw new IllegalArgumentException(
                    "deterministic-fake model publication project configuration is invalid",
                    exception);
        }
    }

    private static Publication productionPublication(
            Path projectRoot,
            Path submissionRoot,
            Path transportSpoolRoot,
            BackpressurePolicy backpressurePolicy) {
        SubmissionManifestStore manifestStore =
                new FileSystemSubmissionManifestStore(submissionRoot);
        DeterministicFakeModelSubmissionManifestPreparer preparer =
                new DeterministicFakeModelSubmissionManifestPreparer(
                        projectRoot,
                        manifestStore,
                        Clock.systemUTC(),
                        new ProjectContextReader(),
                        new ApprovedTaskReader(),
                        new RepositoryMemorySnapshotCollector());
        FileSpoolMessageTransport transport = new FileSpoolMessageTransport(
                transportSpoolRoot, backpressurePolicy);
        return request -> {
            PreparedDeterministicFakeModelSubmission prepared = preparer.prepare(request);
            DurableSubmissionManifest manifest = prepared.manifest();
            FileSpoolPublicationOutcome outcome = transport.sendWithReference(
                    new TransportMessage(
                            DeliveryDestination.queue(manifest.queueId()),
                            manifest.workMessage()));
            ModelWorkPayload payload = (ModelWorkPayload) manifest.workMessage().payload();
            return new DeterministicFakeModelWorkPublicationResult(
                    manifest.submissionId(),
                    manifest.queueId(),
                    prepared.manifestCreated(),
                    payload.snapshotId(),
                    outcome);
        };
    }

    @FunctionalInterface
    interface Publication {
        DeterministicFakeModelWorkPublicationResult publish(
                DeterministicFakeModelSubmissionRequest request) throws IOException;
    }
}
