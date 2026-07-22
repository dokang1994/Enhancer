package com.enhancer.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Restart-safe composition of immutable intent persistence, queue creation or recovery, and
 * exact durable work admission. Scheduler execution remains a separate boundary.
 */
public final class DurableWorkSubmissionService {
    private final SubmissionManifestStore manifestStore;
    private final SchedulerQueueStore queueStore;

    public DurableWorkSubmissionService(
            SubmissionManifestStore manifestStore,
            SchedulerQueueStore queueStore) {
        this.manifestStore = Objects.requireNonNull(
                manifestStore, "manifestStore must not be null");
        this.queueStore = Objects.requireNonNull(
                queueStore, "queueStore must not be null");
    }

    public DurableSubmissionResult submit(DurableSubmissionManifest manifest)
            throws IOException {
        Objects.requireNonNull(manifest, "manifest must not be null");
        boolean manifestCreated = manifestStore.storeIdempotently(manifest);
        QueueResolution resolution = resolveOrCreateQueue(manifest);
        DurableSingleWorkerSchedulerQueue queue = resolution.queue();

        long revisionBeforeAdmission = queue.revision();
        try {
            new DurableWorkItemAdmissionHandler(
                    manifest.requiredCapability(), queue)
                    .handle(manifest.workMessage());
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
        return new DurableSubmissionResult(
                manifest.submissionId(),
                queue.queueId(),
                manifestCreated,
                resolution.created(),
                queue.revision() != revisionBeforeAdmission,
                queue.revision());
    }

    private QueueResolution resolveOrCreateQueue(
            DurableSubmissionManifest manifest) throws IOException {
        try {
            SchedulerQueueState existing = queueStore.resolve(manifest.queueId());
            if (existing.maxWorkItems() != manifest.maxWorkItems()) {
                throw new IllegalArgumentException(
                        "existing Scheduler queue capacity does not match submission manifest");
            }
            return new QueueResolution(
                    DurableSingleWorkerSchedulerQueue.recover(
                            manifest.queueId(), queueStore),
                    false);
        } catch (MissingSchedulerQueueStateException exception) {
            return new QueueResolution(
                    DurableSingleWorkerSchedulerQueue.create(
                            manifest.queueId(),
                            manifest.maxWorkItems(),
                            queueStore),
                    true);
        }
    }

    private record QueueResolution(
            DurableSingleWorkerSchedulerQueue queue,
            boolean created) {
    }
}
