package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectContextReader;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.loop.ApprovedTaskReader;
import com.enhancer.workspace.RepositoryMemorySnapshotCollector;
import com.enhancer.workspace.WorkspaceSnapshot;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal manifest-first source for deterministic-fake typed model work.
 *
 * <p>The request retains caller intent but no capability authority. This boundary obtains its
 * sole capability from the closed repository-owned source, persists through the unchanged
 * durable submission service, and never consults current repository state during exact replay.
 */
final class DeterministicFakeModelSubmissionService {
    private final Path projectRoot;
    private final SubmissionManifestStore manifestStore;
    private final DurableWorkSubmissionService submissionService;
    private final Clock clock;
    private final ProjectContextReader contextReader;
    private final ApprovedTaskReader taskReader;
    private final RepositoryMemorySnapshotCollector snapshotCollector;

    DeterministicFakeModelSubmissionService(
            Path projectRoot,
            SubmissionManifestStore manifestStore,
            SchedulerQueueStore queueStore,
            Clock clock,
            ProjectContextReader contextReader,
            ApprovedTaskReader taskReader,
            RepositoryMemorySnapshotCollector snapshotCollector) {
        this.projectRoot = Objects.requireNonNull(
                projectRoot, "projectRoot must not be null");
        this.manifestStore = Objects.requireNonNull(
                manifestStore, "manifestStore must not be null");
        this.submissionService = new DurableWorkSubmissionService(
                manifestStore,
                Objects.requireNonNull(queueStore, "queueStore must not be null"));
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.contextReader = Objects.requireNonNull(
                contextReader, "contextReader must not be null");
        this.taskReader = Objects.requireNonNull(
                taskReader, "taskReader must not be null");
        this.snapshotCollector = Objects.requireNonNull(
                snapshotCollector, "snapshotCollector must not be null");
    }

    DurableSubmissionResult submit(DeterministicFakeModelSubmissionRequest request)
            throws IOException {
        Objects.requireNonNull(request, "request must not be null");
        GeneratedSubmissionIdentities identities =
                GeneratedSubmissionIdentities.derive(request.submissionId());

        DurableSubmissionManifest manifest;
        try {
            manifest = manifestStore.resolve(request.submissionId());
        } catch (MissingSubmissionManifestException firstUse) {
            manifest = buildFirstUseManifest(request, identities);
        }
        requireConsistent(request, identities, manifest);
        return submissionService.submit(manifest);
    }

    private DurableSubmissionManifest buildFirstUseManifest(
            DeterministicFakeModelSubmissionRequest request,
            GeneratedSubmissionIdentities identities) throws IOException {
        ProjectContext context = contextReader.read(projectRoot);
        ApprovedTask task = taskReader.read(context);
        require(task.taskId().equals(request.taskId()),
                "active task identity does not match the submission request");
        require(task.allows(ModelWorkPayload.MODEL_INVOKE_TOOL_NAME),
                "active task does not allow model-invoke");
        requireTargetContained(request.targetPath());

        Instant occurredAt = clock.instant();
        WorkspaceSnapshot snapshot = snapshotCollector.collect(
                projectRoot,
                occurredAt,
                task,
                context);
        ModelWorkPayload payload = new ModelWorkPayload(
                snapshot.approvedTaskRevision(),
                snapshot.snapshotId(),
                task.allowedTools(),
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        request.targetPath(),
                        request.expectedResponseSha256(),
                        request.executionProfile()));
        MessageEnvelope envelope = new MessageEnvelope(
                identities.submissionId(),
                identities.correlationId(),
                Optional.empty(),
                identities.logicalRunId(),
                request.producer(),
                occurredAt,
                payload);
        return new DurableSubmissionManifest(
                identities.queueId(),
                request.maxWorkItems(),
                DeterministicFakeModelSubmissionCapabilitySource.requiredCapability(),
                envelope,
                request.priority());
    }

    private void requireTargetContained(String targetPath) {
        Path normalizedRoot = projectRoot.toAbsolutePath().normalize();
        Path candidate = normalizedRoot.resolve(Path.of(targetPath)).normalize();
        require(candidate.startsWith(normalizedRoot),
                "target path resolves outside the project root");
    }

    private void requireConsistent(
            DeterministicFakeModelSubmissionRequest request,
            GeneratedSubmissionIdentities identities,
            DurableSubmissionManifest manifest) {
        require(manifest.queueId().equals(identities.queueId()),
                "derived queue identity does not match the submission manifest");
        require(manifest.maxWorkItems() == request.maxWorkItems(),
                "queue capacity does not match the submission manifest");
        require(manifest.requiredCapability().equals(
                        DeterministicFakeModelSubmissionCapabilitySource.requiredCapability()),
                "fixed capability does not match the submission manifest");
        require(manifest.priority() == request.priority(),
                "priority does not match the submission manifest");

        MessageEnvelope envelope = manifest.workMessage();
        require(envelope.messageId().equals(request.submissionId()),
                "message identity does not match the submission request");
        require(envelope.correlationId().equals(identities.correlationId()),
                "derived correlation identity does not match the submission manifest");
        require(envelope.logicalRunId().equals(identities.logicalRunId()),
                "derived logical-run identity does not match the submission manifest");
        require(envelope.causationId().isEmpty(),
                "submission manifest causation must remain absent");
        require(envelope.producer().equals(request.producer()),
                "producer does not match the submission manifest");
        require(envelope.payload() instanceof ModelWorkPayload,
                "submission manifest payload must be ModelWork");

        ModelWorkPayload payload = (ModelWorkPayload) envelope.payload();
        require(payload.taskRevision().taskId().equals(request.taskId()),
                "task identity does not match the submission manifest");
        ModelWorkPayload.ModelInvocationExecutionInput input = payload.executionInput();
        require(input.targetPath().equals(request.targetPath()),
                "target path does not match the submission manifest");
        require(input.expectedResponseSha256().equals(request.expectedResponseSha256()),
                "expected response digest does not match the submission manifest");
        require(input.executionProfile().equals(request.executionProfile()),
                "execution profile does not match the submission manifest");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
