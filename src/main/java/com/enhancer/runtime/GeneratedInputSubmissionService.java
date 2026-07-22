package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Replay-safe application boundary for generated-input Scheduler submission.
 *
 * <p>The caller retains only one canonical submission UUID plus the facts it owns. The queue,
 * correlation, and logical-run identities are derived from the submission UUID through fixed
 * versioned domain transforms ({@link GeneratedSubmissionIdentities}), so the same key always
 * names the same generated work.
 *
 * <p>On every call the existing submission manifest is resolved <em>before</em> the clock or the
 * repository snapshot is consulted. If it exists, its exact occurrence time and envelope are
 * reused and the caller-owned intent is checked against the stored value, failing closed on any
 * conflict. Only when it is absent is the occurrence time captured from the clock, the first-use
 * envelope built, and the exact existing {@link DurableWorkSubmissionService} used to persist the
 * manifest before creating the queue and admitting the work. This boundary adds no second store
 * and never runs a Scheduler cycle.
 */
public final class GeneratedInputSubmissionService {
    private final SubmissionManifestStore manifestStore;
    private final DurableWorkSubmissionService submissionService;
    private final Clock clock;

    public GeneratedInputSubmissionService(
            SubmissionManifestStore manifestStore,
            SchedulerQueueStore queueStore,
            Clock clock) {
        this.manifestStore = Objects.requireNonNull(
                manifestStore, "manifestStore must not be null");
        this.submissionService = new DurableWorkSubmissionService(
                manifestStore,
                Objects.requireNonNull(queueStore, "queueStore must not be null"));
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public DurableSubmissionResult submit(
            GeneratedSubmissionRequest request,
            SubmissionEnvelopeFactory firstUseEnvelopeFactory) throws IOException {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(
                firstUseEnvelopeFactory, "firstUseEnvelopeFactory must not be null");
        GeneratedSubmissionIdentities identities =
                GeneratedSubmissionIdentities.derive(request.submissionId());

        DurableSubmissionManifest manifest;
        try {
            manifest = manifestStore.resolve(request.submissionId());
        } catch (MissingSubmissionManifestException firstUse) {
            manifest = buildFirstUseManifest(request, identities, firstUseEnvelopeFactory);
        }
        requireConsistent(request, identities, manifest);
        return submissionService.submit(manifest);
    }

    private DurableSubmissionManifest buildFirstUseManifest(
            GeneratedSubmissionRequest request,
            GeneratedSubmissionIdentities identities,
            SubmissionEnvelopeFactory firstUseEnvelopeFactory) throws IOException {
        Instant occurredAt = clock.instant();
        MessageEnvelope envelope = Objects.requireNonNull(
                firstUseEnvelopeFactory.create(identities, occurredAt),
                "firstUseEnvelopeFactory must not return null");
        return new DurableSubmissionManifest(
                identities.queueId(),
                request.maxWorkItems(),
                request.requiredCapability(),
                envelope);
    }

    private void requireConsistent(
            GeneratedSubmissionRequest request,
            GeneratedSubmissionIdentities identities,
            DurableSubmissionManifest manifest) {
        require(manifest.queueId().equals(identities.queueId()),
                "generated queue identity does not match the submission manifest");
        require(manifest.maxWorkItems() == request.maxWorkItems(),
                "queue capacity does not match the submission manifest");
        require(manifest.requiredCapability().equals(request.requiredCapability()),
                "required capability does not match the submission manifest");

        MessageEnvelope envelope = manifest.workMessage();
        require(envelope.messageId().equals(request.submissionId()),
                "message identity does not match the submission");
        require(envelope.correlationId().equals(identities.correlationId()),
                "generated correlation identity does not match the submission manifest");
        require(envelope.logicalRunId().equals(identities.logicalRunId()),
                "generated logical-run identity does not match the submission manifest");
        require(envelope.producer().equals(request.producer()),
                "producer does not match the submission manifest");

        WorkPayload work = (WorkPayload) envelope.payload();
        ApprovedTaskRevision revision = work.taskRevision();
        require(revision.taskId().equals(request.taskId()),
                "task identity does not match the submission manifest");

        Optional<WorkPayload.ExecutionInput> executionInput = work.executionInput();
        require(executionInput.isPresent(),
                "submission manifest is missing the execution input");
        WorkPayload.ExecutionInput input = executionInput.orElseThrow();
        require(input.targetPath().equals(request.targetPath()),
                "target path does not match the submission manifest");
        require(input.expectedContentSha256().equals(request.expectedSha256()),
                "expected digest does not match the submission manifest");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
