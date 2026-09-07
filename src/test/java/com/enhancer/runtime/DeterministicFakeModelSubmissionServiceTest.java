package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.MessagePayload;
import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.bus.WorkPayload;
import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectContextReader;
import com.enhancer.context.RequiredProjectDocument;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.loop.ApprovedTaskReader;
import com.enhancer.model.ModelExecutionProfile;
import com.enhancer.workspace.ApprovedTaskRevision;
import com.enhancer.workspace.RepositoryMemorySnapshotCollector;
import com.enhancer.workspace.WorkspaceSnapshot;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeterministicFakeModelSubmissionServiceTest {
    private static final String SUBMISSION_ID =
            "00000000-0000-0000-0000-000000000e11";
    private static final String OTHER_MESSAGE_ID =
            "00000000-0000-0000-0000-000000000e12";
    private static final String OTHER_QUEUE_ID =
            "00000000-0000-0000-0000-000000000e13";
    private static final String CAUSATION_ID =
            "00000000-0000-0000-0000-000000000e14";
    private static final String TASK_ID = "model-submission-task";
    private static final String PRODUCER = "internal-deterministic-model-submission";
    private static final String TARGET = "docs/model-prompt.md";
    private static final String DIGEST = "a".repeat(64);
    private static final Instant FIRST_USE =
            Instant.parse("2026-09-07T06:00:00.123456789Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void firstUseLoadsGovernanceBeforeOneClockCaptureAndAdmitsExactTypedIntent()
            throws Exception {
        Path projectRoot = temporaryRoot.resolve("project");
        String originalTask = writeGovernedProject(
                projectRoot, TASK_ID, List.of("model-invoke", "read-file"));
        ProjectContext expectedContext = new ProjectContextReader().read(projectRoot);
        ApprovedTask expectedTask = new ApprovedTaskReader().read(expectedContext);
        WorkspaceSnapshot expectedSnapshot = new RepositoryMemorySnapshotCollector().collect(
                projectRoot, FIRST_USE, expectedTask, expectedContext);
        CountingClock clock = new CountingClock(FIRST_USE, () -> {
            try {
                Files.writeString(
                        projectRoot.resolve("CURRENT_TASK.md"),
                        "# Current Task\n\n## Status\n\nCompleted\n",
                        StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        });
        Path manifestRoot = temporaryRoot.resolve("manifests");
        Path queueRoot = temporaryRoot.resolve("queues");
        ModelExecutionProfile profile = ModelWorkFixtures.profile("profile-only-capability");

        DurableSubmissionResult result = service(
                projectRoot,
                new FileSystemSubmissionManifestStore(manifestRoot),
                new FileSystemSchedulerQueueStore(queueRoot),
                clock).submit(request(profile));

        assertTrue(result.manifestCreated());
        assertTrue(result.queueCreated());
        assertTrue(result.workAdmitted());
        assertEquals(1L, result.queueRevision());
        assertEquals(1, clock.calls());

        GeneratedSubmissionIdentities identities =
                GeneratedSubmissionIdentities.derive(SUBMISSION_ID);
        DurableSubmissionManifest manifest =
                new FileSystemSubmissionManifestStore(manifestRoot).resolve(SUBMISSION_ID);
        assertEquals(identities.queueId(), manifest.queueId());
        assertEquals(4, manifest.maxWorkItems());
        assertEquals("deterministic-echo", manifest.requiredCapability());
        assertEquals(SchedulerPriority.EXPEDITED, manifest.priority());

        MessageEnvelope envelope = manifest.workMessage();
        assertEquals(SUBMISSION_ID, envelope.messageId());
        assertEquals(identities.correlationId(), envelope.correlationId());
        assertEquals(Optional.empty(), envelope.causationId());
        assertEquals(identities.logicalRunId(), envelope.logicalRunId());
        assertEquals(PRODUCER, envelope.producer());
        assertEquals(FIRST_USE, envelope.occurredAt());

        ModelWorkPayload payload = assertInstanceOf(
                ModelWorkPayload.class, envelope.payload());
        assertEquals(expectedSnapshot.approvedTaskRevision(), payload.taskRevision());
        assertEquals(sha256(originalTask), payload.taskRevision().sourceSha256());
        assertEquals(expectedSnapshot.snapshotId(), payload.snapshotId());
        assertEquals(Set.of("model-invoke", "read-file"), payload.allowedTools());
        assertEquals(TARGET, payload.executionInput().targetPath());
        assertEquals(DIGEST, payload.executionInput().expectedResponseSha256());
        assertEquals(profile, payload.executionInput().executionProfile());
        assertEquals(
                "profile-only-capability",
                payload.executionInput().executionProfile().requiredCapability());

        SchedulerQueueState queue =
                new FileSystemSchedulerQueueStore(queueRoot).resolve(identities.queueId());
        assertEquals(1L, queue.revision());
        assertEquals(1, queue.pendingWork().size());
        assertEquals(SchedulerPriority.EXPEDITED, queue.pendingWork().get(0).priority());
        assertEquals("deterministic-echo",
                queue.pendingWork().get(0).workItem().requiredCapability());
        assertEquals(envelope, queue.pendingWork().get(0).workItem().workMessage());
    }

    @Test
    void exactReplayResolvesManifestBeforeRepositoryOrClockAndChangesNoBytesOrRevision()
            throws Exception {
        Path projectRoot = temporaryRoot.resolve("replay-project");
        writeGovernedProject(projectRoot, TASK_ID, List.of("model-invoke"));
        Path manifestRoot = temporaryRoot.resolve("replay-manifests");
        Path queueRoot = temporaryRoot.resolve("replay-queues");
        DeterministicFakeModelSubmissionRequest request =
                request(ModelWorkFixtures.profile("profile-only-capability"));
        service(
                projectRoot,
                new FileSystemSubmissionManifestStore(manifestRoot),
                new FileSystemSchedulerQueueStore(queueRoot),
                Clock.fixed(FIRST_USE, ZoneOffset.UTC)).submit(request);
        GeneratedSubmissionIdentities identities =
                GeneratedSubmissionIdentities.derive(SUBMISSION_ID);
        Path manifestArtifact = manifestRoot.resolve(
                SUBMISSION_ID + ".submission-manifest");
        Path queueArtifact = queueRoot.resolve(
                identities.queueId() + ".scheduler-queue");
        byte[] manifestBefore = Files.readAllBytes(manifestArtifact);
        byte[] queueBefore = Files.readAllBytes(queueArtifact);
        SchedulerQueueState stateBefore =
                new FileSystemSchedulerQueueStore(queueRoot).resolve(identities.queueId());

        DurableSubmissionResult replay = service(
                temporaryRoot.resolve("repository-no-longer-present"),
                new FileSystemSubmissionManifestStore(manifestRoot),
                new FileSystemSchedulerQueueStore(queueRoot),
                new ThrowingClock()).submit(request);

        assertFalse(replay.manifestCreated());
        assertFalse(replay.queueCreated());
        assertFalse(replay.workAdmitted());
        assertEquals(stateBefore.revision(), replay.queueRevision());
        assertArrayEquals(manifestBefore, Files.readAllBytes(manifestArtifact));
        assertArrayEquals(queueBefore, Files.readAllBytes(queueArtifact));
        SchedulerQueueState stateAfter =
                new FileSystemSchedulerQueueStore(queueRoot).resolve(identities.queueId());
        assertEquals(stateBefore.revision(), stateAfter.revision());
        assertEquals(stateBefore.pendingWork(), stateAfter.pendingWork());
    }

    @Test
    void replayRejectsEveryCallerOwnedDriftBeforeRecaptureOrMutation()
            throws Exception {
        Path projectRoot = temporaryRoot.resolve("caller-drift-project");
        writeGovernedProject(projectRoot, TASK_ID, List.of("model-invoke"));
        Path manifestRoot = temporaryRoot.resolve("caller-drift-manifests");
        Path queueRoot = temporaryRoot.resolve("caller-drift-queues");
        ModelExecutionProfile profile = ModelWorkFixtures.profile("profile-only-capability");
        DeterministicFakeModelSubmissionRequest baseline = request(profile);
        service(
                projectRoot,
                new FileSystemSubmissionManifestStore(manifestRoot),
                new FileSystemSchedulerQueueStore(queueRoot),
                Clock.fixed(FIRST_USE, ZoneOffset.UTC)).submit(baseline);
        GeneratedSubmissionIdentities identities =
                GeneratedSubmissionIdentities.derive(SUBMISSION_ID);
        Path manifestArtifact = manifestRoot.resolve(
                SUBMISSION_ID + ".submission-manifest");
        byte[] manifestBefore = Files.readAllBytes(manifestArtifact);
        SchedulerQueueState queueBefore =
                new FileSystemSchedulerQueueStore(queueRoot).resolve(identities.queueId());

        List<RequestDrift> drifts = List.of(
                new RequestDrift("task", request(
                        "other-task", PRODUCER, TARGET, DIGEST, profile, 4,
                        SchedulerPriority.EXPEDITED)),
                new RequestDrift("producer", request(
                        TASK_ID, "other-producer", TARGET, DIGEST, profile, 4,
                        SchedulerPriority.EXPEDITED)),
                new RequestDrift("target", request(
                        TASK_ID, PRODUCER, "docs/other.md", DIGEST, profile, 4,
                        SchedulerPriority.EXPEDITED)),
                new RequestDrift("digest", request(
                        TASK_ID, PRODUCER, TARGET, "b".repeat(64), profile, 4,
                        SchedulerPriority.EXPEDITED)),
                new RequestDrift("profile", request(
                        TASK_ID, PRODUCER, TARGET, DIGEST,
                        ModelWorkFixtures.profile("different-profile"), 4,
                        SchedulerPriority.EXPEDITED)),
                new RequestDrift("capacity", request(
                        TASK_ID, PRODUCER, TARGET, DIGEST, profile, 5,
                        SchedulerPriority.EXPEDITED)),
                new RequestDrift("priority", request(
                        TASK_ID, PRODUCER, TARGET, DIGEST, profile, 4,
                        SchedulerPriority.NORMAL)));

        for (RequestDrift drift : drifts) {
            IllegalArgumentException failure = assertThrows(
                    IllegalArgumentException.class,
                    () -> service(
                            temporaryRoot.resolve("missing-" + drift.name()),
                            new FileSystemSubmissionManifestStore(manifestRoot),
                            new FileSystemSchedulerQueueStore(queueRoot),
                            new ThrowingClock()).submit(drift.request()),
                    drift.name());
            assertFalse(failure.getMessage().isBlank(), drift.name());
            assertArrayEquals(manifestBefore, Files.readAllBytes(manifestArtifact), drift.name());
            SchedulerQueueState queueAfter =
                    new FileSystemSchedulerQueueStore(queueRoot).resolve(identities.queueId());
            assertEquals(queueBefore.revision(), queueAfter.revision(), drift.name());
            assertEquals(queueBefore.pendingWork(), queueAfter.pendingWork(), drift.name());
        }
    }

    @Test
    void replayRejectsDerivedCapabilityEnvelopeAndPayloadDriftBeforeDelegation()
            throws Exception {
        ModelExecutionProfile profile = ModelWorkFixtures.profile("profile-only-capability");
        DurableSubmissionManifest baseline = exactManifest(profile);
        GeneratedSubmissionIdentities identities =
                GeneratedSubmissionIdentities.derive(SUBMISSION_ID);
        MessageEnvelope envelope = baseline.workMessage();
        ModelWorkPayload payload = (ModelWorkPayload) envelope.payload();
        List<ManifestDrift> drifts = new ArrayList<>();
        drifts.add(new ManifestDrift("queue", manifest(
                OTHER_QUEUE_ID, baseline.maxWorkItems(), baseline.requiredCapability(),
                envelope, baseline.priority())));
        drifts.add(new ManifestDrift("message", manifest(
                identities.queueId(), baseline.maxWorkItems(), baseline.requiredCapability(),
                envelope(OTHER_MESSAGE_ID, identities.correlationId(), Optional.empty(),
                        identities.logicalRunId(), PRODUCER, payload), baseline.priority())));
        drifts.add(new ManifestDrift("correlation", manifest(
                identities.queueId(), baseline.maxWorkItems(), baseline.requiredCapability(),
                envelope(SUBMISSION_ID, "other-correlation", Optional.empty(),
                        identities.logicalRunId(), PRODUCER, payload), baseline.priority())));
        drifts.add(new ManifestDrift("logical-run", manifest(
                identities.queueId(), baseline.maxWorkItems(), baseline.requiredCapability(),
                envelope(SUBMISSION_ID, identities.correlationId(), Optional.empty(),
                        "other-logical-run", PRODUCER, payload), baseline.priority())));
        drifts.add(new ManifestDrift("causation", manifest(
                identities.queueId(), baseline.maxWorkItems(), baseline.requiredCapability(),
                envelope(SUBMISSION_ID, identities.correlationId(), Optional.of(CAUSATION_ID),
                        identities.logicalRunId(), PRODUCER, payload), baseline.priority())));
        drifts.add(new ManifestDrift("fixed-capability", manifest(
                identities.queueId(), baseline.maxWorkItems(), "other-capability",
                envelope, baseline.priority())));
        drifts.add(new ManifestDrift("cross-kind", manifest(
                identities.queueId(), baseline.maxWorkItems(), baseline.requiredCapability(),
                envelope(SUBMISSION_ID, identities.correlationId(), Optional.empty(),
                        identities.logicalRunId(), PRODUCER, legacyPayload()),
                baseline.priority())));
        drifts.add(new ManifestDrift("task", manifest(
                identities.queueId(), baseline.maxWorkItems(), baseline.requiredCapability(),
                envelopeWithPayload(envelope, modelPayload(
                        "other-task", TARGET, DIGEST, profile)), baseline.priority())));
        drifts.add(new ManifestDrift("target", manifest(
                identities.queueId(), baseline.maxWorkItems(), baseline.requiredCapability(),
                envelopeWithPayload(envelope, modelPayload(
                        TASK_ID, "docs/other.md", DIGEST, profile)), baseline.priority())));
        drifts.add(new ManifestDrift("digest", manifest(
                identities.queueId(), baseline.maxWorkItems(), baseline.requiredCapability(),
                envelopeWithPayload(envelope, modelPayload(
                        TASK_ID, TARGET, "b".repeat(64), profile)), baseline.priority())));
        drifts.add(new ManifestDrift("profile", manifest(
                identities.queueId(), baseline.maxWorkItems(), baseline.requiredCapability(),
                envelopeWithPayload(envelope, modelPayload(
                        TASK_ID, TARGET, DIGEST,
                        ModelWorkFixtures.profile("different-profile"))),
                baseline.priority())));

        for (ManifestDrift drift : drifts) {
            ReturningManifestStore manifests = new ReturningManifestStore(drift.manifest());
            NoCallQueueStore queues = new NoCallQueueStore();
            IllegalArgumentException failure = assertThrows(
                    IllegalArgumentException.class,
                    () -> service(
                            temporaryRoot.resolve("missing-stored-" + drift.name()),
                            manifests,
                            queues,
                            new ThrowingClock()).submit(request(profile)),
                    drift.name());
            assertFalse(failure.getMessage().isBlank(), drift.name());
            assertEquals(0, manifests.storeCalls(), drift.name());
            assertEquals(0, queues.calls(), drift.name());
        }
    }

    @Test
    void invalidFirstUseGovernanceAndTargetCreateNoDurableStateOrClockCapture()
            throws Exception {
        Path missingProject = temporaryRoot.resolve("missing-context-project");
        Files.createDirectories(missingProject);
        Path missingManifests = temporaryRoot.resolve("missing-context-manifests");
        Path missingQueues = temporaryRoot.resolve("missing-context-queues");
        assertThrows(IOException.class, () -> service(
                missingProject,
                new FileSystemSubmissionManifestStore(missingManifests),
                new FileSystemSchedulerQueueStore(missingQueues),
                new ThrowingClock()).submit(request(ModelWorkFixtures.profile())));
        assertNoRegularFiles(missingManifests);
        assertNoRegularFiles(missingQueues);

        Path mismatchProject = temporaryRoot.resolve("mismatch-project");
        writeGovernedProject(mismatchProject, TASK_ID, List.of("model-invoke"));
        Path mismatchManifests = temporaryRoot.resolve("mismatch-manifests");
        Path mismatchQueues = temporaryRoot.resolve("mismatch-queues");
        assertThrows(IllegalArgumentException.class, () -> service(
                mismatchProject,
                new FileSystemSubmissionManifestStore(mismatchManifests),
                new FileSystemSchedulerQueueStore(mismatchQueues),
                new ThrowingClock()).submit(request(
                        "other-task", PRODUCER, TARGET, DIGEST,
                        ModelWorkFixtures.profile(), 4, SchedulerPriority.EXPEDITED)));
        assertNoRegularFiles(mismatchManifests);
        assertNoRegularFiles(mismatchQueues);

        Path noToolProject = temporaryRoot.resolve("no-tool-project");
        writeGovernedProject(noToolProject, TASK_ID, List.of("read-file"));
        Path noToolManifests = temporaryRoot.resolve("no-tool-manifests");
        Path noToolQueues = temporaryRoot.resolve("no-tool-queues");
        assertThrows(IllegalArgumentException.class, () -> service(
                noToolProject,
                new FileSystemSubmissionManifestStore(noToolManifests),
                new FileSystemSchedulerQueueStore(noToolQueues),
                new ThrowingClock()).submit(request(ModelWorkFixtures.profile())));
        assertNoRegularFiles(noToolManifests);
        assertNoRegularFiles(noToolQueues);

        Path outsideProject = temporaryRoot.resolve("outside-target-project");
        writeGovernedProject(outsideProject, TASK_ID, List.of("model-invoke"));
        Path outsideManifests = temporaryRoot.resolve("outside-target-manifests");
        Path outsideQueues = temporaryRoot.resolve("outside-target-queues");
        assertThrows(IllegalArgumentException.class, () -> service(
                outsideProject,
                new FileSystemSubmissionManifestStore(outsideManifests),
                new FileSystemSchedulerQueueStore(outsideQueues),
                new ThrowingClock()).submit(request(
                        TASK_ID, PRODUCER, "../outside.md", DIGEST,
                        ModelWorkFixtures.profile(), 4, SchedulerPriority.EXPEDITED)));
        assertNoRegularFiles(outsideManifests);
        assertNoRegularFiles(outsideQueues);
    }

    @Test
    void onlyMissingManifestStartsFirstUseAndDurablePrefixesRecoverWithoutRecapture()
            throws Exception {
        NoCallQueueStore noCalls = new NoCallQueueStore();
        IOException ordinaryFailure = assertThrows(IOException.class, () -> service(
                temporaryRoot.resolve("ordinary-io-missing-project"),
                new SubmissionManifestStore() {
                    @Override
                    public boolean storeIdempotently(DurableSubmissionManifest manifest) {
                        throw new AssertionError("ordinary resolve failure must not become first use");
                    }

                    @Override
                    public DurableSubmissionManifest resolve(String submissionId)
                            throws IOException {
                        throw new IOException("ordinary manifest failure");
                    }
                },
                noCalls,
                new ThrowingClock()).submit(request(ModelWorkFixtures.profile())));
        assertEquals("ordinary manifest failure", ordinaryFailure.getMessage());
        assertEquals(0, noCalls.calls());

        verifyRecovery(Failure.CREATE, true);
        verifyRecovery(Failure.UPDATE, false);
    }

    private void verifyRecovery(Failure failure, boolean retryCreatesQueue)
            throws Exception {
        String suffix = failure.name().toLowerCase();
        Path projectRoot = temporaryRoot.resolve(suffix + "-project");
        writeGovernedProject(projectRoot, TASK_ID, List.of("model-invoke"));
        Path manifestRoot = temporaryRoot.resolve(suffix + "-manifests");
        Path queueRoot = temporaryRoot.resolve(suffix + "-queues");
        FileSystemSubmissionManifestStore manifests =
                new FileSystemSubmissionManifestStore(manifestRoot);
        FileSystemSchedulerQueueStore queues =
                new FileSystemSchedulerQueueStore(queueRoot);
        DeterministicFakeModelSubmissionRequest request =
                request(ModelWorkFixtures.profile("profile-only-capability"));

        assertThrows(IOException.class, () -> service(
                projectRoot,
                manifests,
                new FailingSchedulerQueueStore(queues, failure),
                Clock.fixed(FIRST_USE, ZoneOffset.UTC)).submit(request));
        DurableSubmissionManifest persisted = manifests.resolve(SUBMISSION_ID);
        assertEquals("deterministic-echo", persisted.requiredCapability());

        DurableSubmissionResult recovered = service(
                temporaryRoot.resolve(suffix + "-project-gone"),
                manifests,
                queues,
                new ThrowingClock()).submit(request);
        assertFalse(recovered.manifestCreated());
        assertEquals(retryCreatesQueue, recovered.queueCreated());
        assertTrue(recovered.workAdmitted());
        assertEquals(1L, recovered.queueRevision());
        SchedulerQueueState state = queues.resolve(
                GeneratedSubmissionIdentities.derive(SUBMISSION_ID).queueId());
        assertEquals(1, state.pendingWork().size());
    }

    private DeterministicFakeModelSubmissionService service(
            Path projectRoot,
            SubmissionManifestStore manifests,
            SchedulerQueueStore queues,
            Clock clock) {
        return new DeterministicFakeModelSubmissionService(
                projectRoot,
                manifests,
                queues,
                clock,
                new ProjectContextReader(),
                new ApprovedTaskReader(),
                new RepositoryMemorySnapshotCollector());
    }

    private DeterministicFakeModelSubmissionRequest request(ModelExecutionProfile profile) {
        return request(
                TASK_ID,
                PRODUCER,
                TARGET,
                DIGEST,
                profile,
                4,
                SchedulerPriority.EXPEDITED);
    }

    private DeterministicFakeModelSubmissionRequest request(
            String taskId,
            String producer,
            String target,
            String digest,
            ModelExecutionProfile profile,
            int capacity,
            SchedulerPriority priority) {
        return new DeterministicFakeModelSubmissionRequest(
                SUBMISSION_ID,
                taskId,
                producer,
                target,
                digest,
                profile,
                capacity,
                priority);
    }

    private DurableSubmissionManifest exactManifest(ModelExecutionProfile profile) {
        GeneratedSubmissionIdentities identities =
                GeneratedSubmissionIdentities.derive(SUBMISSION_ID);
        return manifest(
                identities.queueId(),
                4,
                "deterministic-echo",
                envelope(
                        SUBMISSION_ID,
                        identities.correlationId(),
                        Optional.empty(),
                        identities.logicalRunId(),
                        PRODUCER,
                        modelPayload(TASK_ID, TARGET, DIGEST, profile)),
                SchedulerPriority.EXPEDITED);
    }

    private DurableSubmissionManifest manifest(
            String queueId,
            int capacity,
            String capability,
            MessageEnvelope envelope,
            SchedulerPriority priority) {
        return new DurableSubmissionManifest(
                queueId, capacity, capability, envelope, priority);
    }

    private MessageEnvelope envelopeWithPayload(
            MessageEnvelope envelope,
            MessagePayload payload) {
        return envelope(
                envelope.messageId(),
                envelope.correlationId(),
                envelope.causationId(),
                envelope.logicalRunId(),
                envelope.producer(),
                payload);
    }

    private MessageEnvelope envelope(
            String messageId,
            String correlationId,
            Optional<String> causationId,
            String logicalRunId,
            String producer,
            MessagePayload payload) {
        return new MessageEnvelope(
                messageId,
                correlationId,
                causationId,
                logicalRunId,
                producer,
                FIRST_USE,
                payload);
    }

    private ModelWorkPayload modelPayload(
            String taskId,
            String target,
            String digest,
            ModelExecutionProfile profile) {
        return new ModelWorkPayload(
                new ApprovedTaskRevision(taskId, "CURRENT_TASK.md", "b".repeat(64)),
                "c".repeat(64),
                Set.of("model-invoke"),
                new ModelWorkPayload.ModelInvocationExecutionInput(
                        target, digest, profile));
    }

    private WorkPayload legacyPayload() {
        return new WorkPayload(
                new ApprovedTaskRevision(TASK_ID, "CURRENT_TASK.md", "b".repeat(64)),
                "c".repeat(64),
                Set.of("model-invoke"),
                Optional.of(new WorkPayload.ExecutionInput(TARGET, DIGEST)));
    }

    private String writeGovernedProject(
            Path projectRoot,
            String taskId,
            List<String> allowedTools) throws IOException {
        StringBuilder tools = new StringBuilder();
        for (String tool : allowedTools) {
            tools.append("- ").append(tool).append('\n');
        }
        String currentTask = "# Current Task\n\n"
                + "## Status\n\nIn Progress\n\n"
                + "## Task\n\nSubmit one deterministic model work item.\n\n"
                + "## Task ID\n\n" + taskId + "\n\n"
                + "## Approval\n\nApproved by the RFC-0024 test owner.\n\n"
                + "## Allowed Tools\n\n" + tools;
        for (RequiredProjectDocument document : RequiredProjectDocument.values()) {
            Path path = projectRoot.resolve(document.path());
            Files.createDirectories(path.getParent());
            Files.writeString(
                    path,
                    document == RequiredProjectDocument.CURRENT_TASK
                            ? currentTask
                            : "# " + document.name() + "\n",
                    StandardCharsets.UTF_8);
        }
        return currentTask;
    }

    private void assertNoRegularFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var files = Files.walk(root)) {
            assertTrue(files.noneMatch(Files::isRegularFile));
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                    value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record RequestDrift(
            String name,
            DeterministicFakeModelSubmissionRequest request) {}

    private record ManifestDrift(
            String name,
            DurableSubmissionManifest manifest) {}

    private static final class CountingClock extends Clock {
        private final Instant instant;
        private final Runnable onInstant;
        private int calls;

        private CountingClock(Instant instant, Runnable onInstant) {
            this.instant = instant;
            this.onInstant = onInstant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            calls++;
            onInstant.run();
            return instant;
        }

        private int calls() {
            return calls;
        }
    }

    private static final class ThrowingClock extends Clock {
        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            throw new AssertionError("clock must not be consulted");
        }
    }

    private static final class ReturningManifestStore
            implements SubmissionManifestStore {
        private final DurableSubmissionManifest manifest;
        private int storeCalls;

        private ReturningManifestStore(DurableSubmissionManifest manifest) {
            this.manifest = manifest;
        }

        @Override
        public boolean storeIdempotently(DurableSubmissionManifest ignored) {
            storeCalls++;
            throw new AssertionError("drift must fail before manifest submission");
        }

        @Override
        public DurableSubmissionManifest resolve(String submissionId) {
            return manifest;
        }

        private int storeCalls() {
            return storeCalls;
        }
    }

    private static final class NoCallQueueStore implements SchedulerQueueStore {
        private int calls;

        @Override
        public void create(SchedulerQueueState initialState) {
            calls++;
            throw new AssertionError("queue must not be consulted");
        }

        @Override
        public void update(SchedulerQueueState nextState) {
            calls++;
            throw new AssertionError("queue must not be consulted");
        }

        @Override
        public SchedulerQueueState resolve(String queueId) {
            calls++;
            throw new AssertionError("queue must not be consulted");
        }

        private int calls() {
            return calls;
        }
    }

    private enum Failure {
        CREATE,
        UPDATE
    }

    private static final class FailingSchedulerQueueStore
            implements SchedulerQueueStore {
        private final SchedulerQueueStore delegate;
        private final Failure failure;

        private FailingSchedulerQueueStore(
                SchedulerQueueStore delegate,
                Failure failure) {
            this.delegate = delegate;
            this.failure = failure;
        }

        @Override
        public void create(SchedulerQueueState initialState) throws IOException {
            if (failure == Failure.CREATE) {
                throw new IOException("simulated queue-create interruption");
            }
            delegate.create(initialState);
        }

        @Override
        public void update(SchedulerQueueState nextState) throws IOException {
            if (failure == Failure.UPDATE) {
                throw new IOException("simulated queue-update interruption");
            }
            delegate.update(nextState);
        }

        @Override
        public SchedulerQueueState resolve(String queueId) throws IOException {
            return delegate.resolve(queueId);
        }
    }
}
