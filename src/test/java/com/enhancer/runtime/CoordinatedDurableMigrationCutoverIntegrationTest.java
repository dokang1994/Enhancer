package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.BackpressurePolicy;
import com.enhancer.bus.DeliveryDestination;
import com.enhancer.bus.FileSpoolMessageTransport;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ResultPayload;
import com.enhancer.bus.TransportMessage;
import com.enhancer.bus.TransportStatus;
import com.enhancer.bus.WorkPayload;
import com.enhancer.kernel.VerificationStatus;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CoordinatedDurableMigrationCutoverIntegrationTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000002601";
    private static final String SUBMISSION_ID =
            "00000000-0000-0000-0000-000000002602";
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000002604";
    private static final long STORED_AT_MILLIS = 1_777_777_779L;

    @TempDir
    Path temporaryRoot;

    @Test
    void preparesEveryCandidateThenPublishesTheCompleteClosureConsumerFirst()
            throws Exception {
        Fixture fixture = fixture("success");
        List<String> events = new ArrayList<>();
        CoordinatedDurableMigrationCutover.Hook hook =
                new CoordinatedDurableMigrationCutover.Hook() {
                    @Override
                    public void afterCandidatesPrepared(List<Path> candidates)
                            throws IOException {
                        assertEquals(3, candidates.size());
                        assertTrue(candidates.stream().allMatch(Files::isRegularFile));
                        assertSourceBytes(
                                fixture.sourceArtifacts(), fixture.sourceBytes());
                        events.add("CANDIDATES_READY");
                    }

                    @Override
                    public void beforePublication(
                            CoordinatedDurableMigrationCutover.PublicationPoint point,
                            Path source) {
                        events.add(point.name());
                    }
                };
        CoordinatedDurableMigrationCutover.Result result =
                new CoordinatedDurableMigrationCutover(hook)
                        .execute(fixture.plan());

        assertEquals(CoordinatedDurableMigrationCutover.Status.MIGRATED,
                result.status());
        assertEquals(List.of(
                        "CANDIDATES_READY",
                        "RESULT_SPOOL",
                        "WORK_SPOOL",
                        "AGENT_RUNTIME",
                        "SCHEDULER_QUEUE",
                        "SUBMISSION_MANIFEST",
                        "INGRESS_SPOOL"),
                events);
        assertCurrent(fixture);
        assertArrayEquals(fixture.sourceBytes().get(3),
                Files.readAllBytes(fixture.workPoint()));
        assertArrayEquals(fixture.sourceBytes().get(4),
                Files.readAllBytes(fixture.resultPoint()));
        assertArrayEquals(fixture.sourceBytes().get(5),
                Files.readAllBytes(fixture.ingressPoint()));
        assertArrayEquals(fixture.sourceBytes().get(6),
                Files.readAllBytes(fixture.binding()));
        assertArrayEquals(fixture.sourceBytes().get(7),
                Files.readAllBytes(fixture.fence()));
    }

    @ParameterizedTest
    @EnumSource(CoordinatedDurableMigrationCutover.PublicationPoint.class)
    void crashAfterEveryPublicationBoundaryResumesAtTheFirstOldPoint(
            CoordinatedDurableMigrationCutover.PublicationPoint crashPoint)
            throws Exception {
        Fixture fixture = fixture("crash-" + crashPoint.name().toLowerCase());
        List<Path> candidates = new ArrayList<>();
        CoordinatedDurableMigrationCutover.Hook crashHook =
                new CoordinatedDurableMigrationCutover.Hook() {
                    @Override
                    public void afterCandidatesPrepared(List<Path> prepared) {
                        candidates.addAll(prepared);
                    }

                    @Override
                    public void afterPublication(
                            CoordinatedDurableMigrationCutover.PublicationPoint point,
                            Path source) throws IOException {
                        if (point == crashPoint) {
                            throw new IOException("simulated crash after " + point);
                        }
                    }
                };

        CoordinatedDurableMigrationCutover.Result interrupted =
                new CoordinatedDurableMigrationCutover(crashHook)
                        .execute(fixture.plan());

        assertEquals(CoordinatedDurableMigrationCutover.Status.REFUSED,
                interrupted.status());
        assertTrue(candidates.stream().noneMatch(Files::exists));
        List<Path> currentPrefix = currentStoreArtifacts(fixture);
        assertEquals(expectedCurrentPrefixSize(crashPoint), currentPrefix.size());
        assertOldClosureIsOrdinarilyUnreadable(fixture, currentPrefix.size());
        List<byte[]> prefixBytes = currentPrefix.stream()
                .map(CoordinatedDurableMigrationCutoverIntegrationTest::read)
                .toList();
        List<FileTime> prefixTimes = modificationTimes(currentPrefix);

        CoordinatedDurableMigrationCutover.Result resumed =
                new CoordinatedDurableMigrationCutover().execute(fixture.plan());

        assertTrue(resumed.status()
                == CoordinatedDurableMigrationCutover.Status.MIGRATED
                || resumed.status()
                == CoordinatedDurableMigrationCutover.Status.ALREADY_CURRENT);
        assertSourceBytes(currentPrefix, prefixBytes);
        assertEquals(prefixTimes, modificationTimes(currentPrefix));
        assertCurrent(fixture);
    }

    @Test
    void exactCurrentClosureIsNonWritingAlreadyCurrent() throws Exception {
        Fixture fixture = fixture("already-current");
        assertEquals(
                CoordinatedDurableMigrationCutover.Status.MIGRATED,
                new CoordinatedDurableMigrationCutover()
                        .execute(fixture.plan()).status());
        List<byte[]> bytes = fixture.sourceArtifacts().stream()
                .map(CoordinatedDurableMigrationCutoverIntegrationTest::read)
                .toList();
        List<FileTime> times = modificationTimes(fixture.sourceArtifacts());
        List<String> events = new ArrayList<>();
        CoordinatedDurableMigrationCutover.Hook hook =
                new CoordinatedDurableMigrationCutover.Hook() {
                    @Override
                    public void beforePublication(
                            CoordinatedDurableMigrationCutover.PublicationPoint point,
                            Path source) {
                        events.add(point.name());
                    }
                };

        CoordinatedDurableMigrationCutover.Result repeated =
                new CoordinatedDurableMigrationCutover(hook)
                        .execute(fixture.plan());

        assertEquals(CoordinatedDurableMigrationCutover.Status.ALREADY_CURRENT,
                repeated.status());
        assertTrue(events.isEmpty());
        assertSourceBytes(fixture.sourceArtifacts(), bytes);
        assertEquals(times, modificationTimes(fixture.sourceArtifacts()));
        assertFalse(hasCandidates(fixture.root()));
    }

    @ParameterizedTest
    @EnumSource(DriftPoint.class)
    void sourceDriftPreservesTheChangedSourceAndEveryLaterTarget(
            DriftPoint driftPoint) throws Exception {
        Fixture fixture = fixture("drift-" + driftPoint.name().toLowerCase());
        Path driftedSource = driftPoint.path(fixture);
        byte[][] changedBytes = new byte[1][];
        CoordinatedDurableMigrationCutover.Hook hook =
                new CoordinatedDurableMigrationCutover.Hook() {
                    @Override
                    public void afterCandidatesPrepared(List<Path> candidates)
                            throws IOException {
                        if (driftPoint == DriftPoint.FENCE
                                || driftPoint == DriftPoint.BINDING) {
                            changedBytes[0] = corrupt(driftedSource);
                        }
                    }

                    @Override
                    public void beforePublication(
                            CoordinatedDurableMigrationCutover.PublicationPoint point,
                            Path source) throws IOException {
                        if (driftPoint.publicationPoint().equals(Optional.of(point))) {
                            changedBytes[0] = corrupt(source);
                        }
                    }
                };

        CoordinatedDurableMigrationCutover.Result result =
                new CoordinatedDurableMigrationCutover(hook)
                        .execute(fixture.plan());

        assertEquals(CoordinatedDurableMigrationCutover.Status.REFUSED,
                result.status());
        assertEquals(Optional.of(CoordinatedDurableMigrationRefusalCode.SOURCE_INVALID),
                result.refusalCode());
        assertArrayEquals(changedBytes[0], Files.readAllBytes(driftedSource));
        for (Path later : driftPoint.laterTargets(fixture)) {
            assertArrayEquals(originalBytes(fixture, later),
                    Files.readAllBytes(later));
        }
        assertFalse(hasCandidates(fixture.root()));
    }

    private Fixture fixture(String name) throws Exception {
        Path root = temporaryRoot.resolve(name);
        Path manifestRoot = root.resolve("manifests");
        Path queueRoot = root.resolve("queue");
        Path runtimeRoot = root.resolve("runtime");
        Path fence = root.resolve("stopped-owner.fence");
        Path binding = root.resolve("effect-ledger.binding");
        byte[] fenceBytes = "held-fence".getBytes(StandardCharsets.UTF_8);
        byte[] bindingBytes = "immutable-effect-ledger-binding"
                .getBytes(StandardCharsets.UTF_8);
        Files.createDirectories(root);
        Files.write(fence, fenceBytes);
        Files.write(binding, bindingBytes);

        MessageEnvelope workMessage = workMessage();
        String workItemId = DurableWorkItemAdmissionHandler.workItemIdFor(
                SUBMISSION_ID);
        WorkItem workItem = new WorkItem(
                workItemId, "independent-capability", workMessage);
        DurableSubmissionManifest manifest = new DurableSubmissionManifest(
                QUEUE_ID,
                8,
                workItem.requiredCapability(),
                workMessage,
                SchedulerPriority.EXPEDITED);
        QueuedWork queuedWork = new QueuedWork(
                workItem, List.of(), SchedulerPriority.EXPEDITED);
        SchedulerQueueState queueState = new SchedulerQueueState(
                SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                QUEUE_ID,
                7L,
                8,
                3,
                1,
                Optional.empty(),
                Optional.of("coordinated-cutover-run"),
                List.of(workItemId),
                List.of(queuedWork),
                List.of(queuedWork),
                Optional.empty(),
                Set.of(),
                Set.of());
        AgentRuntimeState runtimeState = AgentRuntimeState.initial(GOAL_ID, workItem);

        Path manifestArtifact = manifestRoot.resolve(
                SUBMISSION_ID + ".submission-manifest");
        Path queueArtifact = queueRoot.resolve(QUEUE_ID + ".scheduler-queue");
        Path runtimeArtifact = runtimeRoot.resolve(GOAL_ID + ".agent-runtime");
        write(manifestArtifact, manifestV2Envelope(manifest));
        write(queueArtifact, queueV3Envelope(queueState));
        write(runtimeArtifact, runtimeV4Envelope(runtimeState));

        Path workPoint = spool(
                root.resolve("work-spool"),
                new TransportMessage(
                        DeliveryDestination.queue("work"), workMessage));
        Path ingressPoint = spool(
                root.resolve("ingress-spool"),
                new TransportMessage(
                        DeliveryDestination.queue("ingress"), workMessage));
        MessageEnvelope resultEnvelope = new MessageEnvelope(
                "00000000-0000-0000-0000-000000002605",
                workMessage.correlationId(),
                Optional.of(workMessage.messageId()),
                workMessage.logicalRunId(),
                "coordinated-cutover-result",
                Instant.parse("2026-08-26T02:03:05Z"),
                new ResultPayload(
                        "coordinated-cutover",
                        "run-record/00000000-0000-0000-0000-000000002606",
                        VerificationStatus.VERIFIED));
        Path resultPoint = spool(
                root.resolve("result-spool"),
                new TransportMessage(
                        DeliveryDestination.topic("results"), resultEnvelope));
        List<Path> sourceArtifacts = List.of(
                manifestArtifact,
                queueArtifact,
                runtimeArtifact,
                workPoint,
                resultPoint,
                ingressPoint,
                binding,
                fence);
        List<byte[]> sourceBytes = sourceArtifacts.stream()
                .map(CoordinatedDurableMigrationCutoverIntegrationTest::read)
                .toList();
        CoordinatedDurableMigrationPlan plan =
                new CoordinatedDurableMigrationPlan(
                        fence,
                        fenceBytes,
                        manifestRoot,
                        List.of(SUBMISSION_ID),
                        queueRoot,
                        QUEUE_ID,
                        runtimeRoot,
                        List.of(GOAL_ID),
                        List.of(workPoint),
                        List.of(resultPoint),
                        List.of(ingressPoint),
                        List.of(binding));
        return new Fixture(
                root,
                plan,
                manifestRoot,
                queueRoot,
                runtimeRoot,
                manifestArtifact,
                queueArtifact,
                runtimeArtifact,
                workPoint,
                resultPoint,
                ingressPoint,
                binding,
                fence,
                manifest,
                queueState,
                runtimeState,
                sourceArtifacts,
                sourceBytes);
    }

    private static void assertCurrent(Fixture fixture) throws Exception {
        assertEquals(fixture.manifest(),
                new FileSystemSubmissionManifestStore(fixture.manifestRoot())
                        .resolve(SUBMISSION_ID));
        SchedulerQueueState migratedQueue =
                new FileSystemSchedulerQueueStore(fixture.queueRoot())
                        .resolve(QUEUE_ID);
        assertEquals(fixture.queueState().admittedWork(),
                migratedQueue.admittedWork());
        assertEquals(fixture.queueState().pendingWork(),
                migratedQueue.pendingWork());
        assertEquals(fixture.queueState().admissionOrder(),
                migratedQueue.admissionOrder());
        assertEquals(fixture.queueState().maximumExpeditedBurst(),
                migratedQueue.maximumExpeditedBurst());
        assertEquals(fixture.queueState().consecutiveExpeditedClaims(),
                migratedQueue.consecutiveExpeditedClaims());
        AgentRuntimeState migratedRuntime =
                new FileSystemAgentRuntimeStateStore(fixture.runtimeRoot())
                        .resolve(GOAL_ID);
        assertEquals(fixture.runtimeState().goal(), migratedRuntime.goal());
        assertEquals(fixture.runtimeState().agentRuns(),
                migratedRuntime.agentRuns());
        assertEquals(fixture.runtimeState().retryDecisions(),
                migratedRuntime.retryDecisions());
    }

    private static List<Path> currentStoreArtifacts(Fixture fixture)
            throws Exception {
        List<Path> current = new ArrayList<>();
        if (new FileSystemAgentRuntimeStateStore(fixture.runtimeRoot())
                .inspectForMigration(GOAL_ID).orElseThrow().alreadyCurrent()) {
            current.add(fixture.runtimeArtifact());
        }
        if (new FileSystemSchedulerQueueStore(fixture.queueRoot())
                .inspectForMigration(QUEUE_ID).orElseThrow().alreadyCurrent()) {
            current.add(fixture.queueArtifact());
        }
        if (new FileSystemSubmissionManifestStore(fixture.manifestRoot())
                .inspectForMigration(SUBMISSION_ID).orElseThrow().alreadyCurrent()) {
            current.add(fixture.manifestArtifact());
        }
        return List.copyOf(current);
    }

    private static int expectedCurrentPrefixSize(
            CoordinatedDurableMigrationCutover.PublicationPoint crashPoint) {
        return switch (crashPoint) {
            case RESULT_SPOOL, WORK_SPOOL -> 0;
            case AGENT_RUNTIME -> 1;
            case SCHEDULER_QUEUE -> 2;
            case SUBMISSION_MANIFEST, INGRESS_SPOOL -> 3;
        };
    }

    private static void assertOldClosureIsOrdinarilyUnreadable(
            Fixture fixture,
            int currentPrefixSize) throws Exception {
        if (currentPrefixSize == 3) {
            return;
        }
        assertEquals(
                CoordinatedDurableMigrationPreflightStatus.READY,
                new CoordinatedDurableMigrationPreflight()
                        .inspect(fixture.plan()).status());
        if (currentPrefixSize == 0) {
            assertThrows(CorruptedAgentRuntimeStateException.class,
                    () -> new FileSystemAgentRuntimeStateStore(
                            fixture.runtimeRoot()).resolve(GOAL_ID));
        } else if (currentPrefixSize == 1) {
            assertThrows(CorruptedSchedulerQueueStateException.class,
                    () -> new FileSystemSchedulerQueueStore(
                            fixture.queueRoot()).resolve(QUEUE_ID));
        } else {
            assertThrows(IOException.class,
                    () -> new FileSystemSubmissionManifestStore(
                            fixture.manifestRoot()).resolve(SUBMISSION_ID));
        }
    }

    private static List<FileTime> modificationTimes(List<Path> paths)
            throws IOException {
        List<FileTime> times = new ArrayList<>();
        for (Path path : paths) {
            times.add(Files.getLastModifiedTime(path));
        }
        return List.copyOf(times);
    }

    private static boolean hasCandidates(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.anyMatch(path -> path.getFileName().toString()
                    .startsWith(".coordinated-"));
        }
    }

    private static byte[] corrupt(Path source) throws IOException {
        byte[] changed = Files.readAllBytes(source);
        changed[changed.length - 1] ^= 0x01;
        Files.write(source, changed);
        return changed;
    }

    private static byte[] originalBytes(Fixture fixture, Path path) {
        int index = fixture.sourceArtifacts().indexOf(path);
        if (index < 0) {
            throw new IllegalArgumentException("path is not a fixture source");
        }
        return fixture.sourceBytes().get(index);
    }

    private static MessageEnvelope workMessage() {
        return new MessageEnvelope(
                SUBMISSION_ID,
                "coordinated-cutover-correlation",
                Optional.empty(),
                "coordinated-cutover-run",
                "coordinated-cutover-test",
                Instant.parse("2026-08-26T02:03:04.005000006Z"),
                new WorkPayload(
                        new ApprovedTaskRevision(
                                "coordinated-cutover",
                                "CURRENT_TASK.md",
                                "a".repeat(64)),
                        "b".repeat(64),
                        Set.of("read-file", "model-invoke"),
                        Optional.empty()));
    }

    private static byte[] manifestV2Envelope(DurableSubmissionManifest manifest)
            throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(2);
            writeString(output, "durable-submission-manifest");
            writeString(output, manifest.queueId());
            output.writeInt(manifest.maxWorkItems());
            writeString(output, manifest.requiredCapability());
            writeString(output, manifest.priority().name());
            writeLegacyEnvelope(output, manifest.workMessage(), true);
        }
        return envelope(0x45534d31, bytes.toByteArray());
    }

    private static byte[] queueV3Envelope(SchedulerQueueState state)
            throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(3);
            writeString(output, "scheduler-queue-state");
            writeString(output, state.queueId());
            output.writeLong(state.revision());
            output.writeInt(state.maxWorkItems());
            output.writeInt(state.maximumExpeditedBurst());
            output.writeInt(state.consecutiveExpeditedClaims());
            output.writeBoolean(false);
            output.writeBoolean(state.logicalRunId().isPresent());
            writeString(output, state.logicalRunId().orElseThrow());
            writeStringList(output, state.admissionOrder());
            writeQueuedWorkList(output, state.admittedWork());
            writeQueuedWorkList(output, state.pendingWork());
            output.writeBoolean(false);
            writeStringSet(output, state.completedWorkItemIds());
            writeStringSet(output, state.failedWorkItemIds());
        }
        return envelope(0x45535131, bytes.toByteArray());
    }

    private static byte[] runtimeV4Envelope(AgentRuntimeState state)
            throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(4);
            writeString(output, "agent-runtime-state");
            writeString(output, state.goal().goalId());
            output.writeLong(state.revision());
            output.writeLong(state.lastIssuedFenceToken());
            writeString(output, state.goal().status().name());
            WorkItem workItem = state.goal().workItem();
            writeString(output, workItem.workItemId());
            writeString(output, workItem.requiredCapability());
            writeLegacyEnvelope(output, workItem.workMessage(), false);
            output.writeInt(0);
            output.writeInt(0);
            output.writeInt(0);
            output.writeInt(0);
            output.writeBoolean(false);
        }
        return envelope(0x45415231, bytes.toByteArray());
    }

    private static void writeQueuedWorkList(
            DataOutputStream output,
            List<QueuedWork> work) throws Exception {
        output.writeInt(work.size());
        for (QueuedWork queuedWork : work) {
            WorkItem item = queuedWork.workItem();
            writeString(output, item.workItemId());
            writeString(output, item.requiredCapability());
            writeLegacyEnvelope(output, item.workMessage(), true);
            writeStringSet(output, queuedWork.dependencyWorkItemIds());
            writeString(output, queuedWork.priority().name());
        }
    }

    private static void writeLegacyEnvelope(
            DataOutputStream output,
            MessageEnvelope envelope,
            boolean manifestOrQueue) throws Exception {
        writeString(output, MessageEnvelope.ENVELOPE_VERSION);
        writeString(output, envelope.messageId());
        writeString(output, envelope.correlationId());
        output.writeBoolean(envelope.causationId().isPresent());
        writeString(output, envelope.logicalRunId());
        writeString(output, envelope.producer());
        output.writeLong(envelope.occurredAt().getEpochSecond());
        output.writeInt(envelope.occurredAt().getNano());
        if (!manifestOrQueue) {
            writeString(output, "work");
        }
        WorkPayload payload = (WorkPayload) envelope.payload();
        writeString(output, payload.taskRevision().taskId());
        writeString(output, payload.taskRevision().sourceDocument());
        writeString(output, payload.taskRevision().sourceSha256());
        writeString(output, payload.snapshotId());
        writeStringSet(output, payload.allowedTools());
        output.writeBoolean(false);
    }

    private static byte[] envelope(int magic, byte[] payload) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                ByteBuffer.allocate(Integer.BYTES + Long.BYTES
                                + Integer.BYTES + payload.length)
                        .putInt(magic)
                        .putLong(STORED_AT_MILLIS)
                        .putInt(payload.length)
                        .put(payload)
                        .array());
        return ByteBuffer.allocate(Integer.BYTES + Long.BYTES
                        + Integer.BYTES + digest.length + payload.length)
                .putInt(magic)
                .putLong(STORED_AT_MILLIS)
                .putInt(payload.length)
                .put(digest)
                .put(payload)
                .array();
    }

    private static Path spool(Path root, TransportMessage message) {
        var publication = new FileSpoolMessageTransport(
                root, BackpressurePolicy.standard()).sendWithReference(message);
        assertEquals(TransportStatus.ACCEPTED, publication.outcome().status());
        return root.resolve(publication.messageFile().orElseThrow());
    }

    private static void write(Path path, byte[] bytes) throws Exception {
        Files.createDirectories(path.getParent());
        Files.write(path, bytes);
    }

    private static byte[] read(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void assertSourceBytes(
            List<Path> artifacts,
            List<byte[]> expected) throws IOException {
        for (int index = 0; index < artifacts.size(); index++) {
            assertArrayEquals(expected.get(index),
                    Files.readAllBytes(artifacts.get(index)));
        }
    }

    private static void writeStringList(
            DataOutputStream output,
            List<String> values) throws Exception {
        output.writeInt(values.size());
        for (String value : values) {
            writeString(output, value);
        }
    }

    private static void writeStringSet(
            DataOutputStream output,
            Set<String> values) throws Exception {
        output.writeInt(values.size());
        for (String value : values.stream().sorted().toList()) {
            writeString(output, value);
        }
    }

    private static void writeString(DataOutputStream output, String value)
            throws Exception {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private record Fixture(
            Path root,
            CoordinatedDurableMigrationPlan plan,
            Path manifestRoot,
            Path queueRoot,
            Path runtimeRoot,
            Path manifestArtifact,
            Path queueArtifact,
            Path runtimeArtifact,
            Path workPoint,
            Path resultPoint,
            Path ingressPoint,
            Path binding,
            Path fence,
            DurableSubmissionManifest manifest,
            SchedulerQueueState queueState,
            AgentRuntimeState runtimeState,
            List<Path> sourceArtifacts,
            List<byte[]> sourceBytes) {
    }

    private enum DriftPoint {
        FENCE(Optional.empty()),
        BINDING(Optional.empty()),
        RESULT_SPOOL(Optional.of(
                CoordinatedDurableMigrationCutover.PublicationPoint.RESULT_SPOOL)),
        WORK_SPOOL(Optional.of(
                CoordinatedDurableMigrationCutover.PublicationPoint.WORK_SPOOL)),
        AGENT_RUNTIME(Optional.of(
                CoordinatedDurableMigrationCutover.PublicationPoint.AGENT_RUNTIME)),
        SCHEDULER_QUEUE(Optional.of(
                CoordinatedDurableMigrationCutover.PublicationPoint.SCHEDULER_QUEUE)),
        SUBMISSION_MANIFEST(Optional.of(
                CoordinatedDurableMigrationCutover.PublicationPoint
                        .SUBMISSION_MANIFEST)),
        INGRESS_SPOOL(Optional.of(
                CoordinatedDurableMigrationCutover.PublicationPoint.INGRESS_SPOOL));

        private final Optional<CoordinatedDurableMigrationCutover.PublicationPoint>
                publicationPoint;

        DriftPoint(Optional<CoordinatedDurableMigrationCutover.PublicationPoint>
                publicationPoint) {
            this.publicationPoint = publicationPoint;
        }

        Optional<CoordinatedDurableMigrationCutover.PublicationPoint>
                publicationPoint() {
            return publicationPoint;
        }

        Path path(Fixture fixture) {
            return switch (this) {
                case FENCE -> fixture.fence();
                case BINDING -> fixture.binding();
                case RESULT_SPOOL -> fixture.resultPoint();
                case WORK_SPOOL -> fixture.workPoint();
                case AGENT_RUNTIME -> fixture.runtimeArtifact();
                case SCHEDULER_QUEUE -> fixture.queueArtifact();
                case SUBMISSION_MANIFEST -> fixture.manifestArtifact();
                case INGRESS_SPOOL -> fixture.ingressPoint();
            };
        }

        List<Path> laterTargets(Fixture fixture) {
            return switch (this) {
                case FENCE, BINDING, RESULT_SPOOL, WORK_SPOOL -> List.of(
                        fixture.runtimeArtifact(),
                        fixture.queueArtifact(),
                        fixture.manifestArtifact());
                case AGENT_RUNTIME -> List.of(
                        fixture.queueArtifact(), fixture.manifestArtifact());
                case SCHEDULER_QUEUE -> List.of(fixture.manifestArtifact());
                case SUBMISSION_MANIFEST, INGRESS_SPOOL -> List.of();
            };
        }
    }
}
