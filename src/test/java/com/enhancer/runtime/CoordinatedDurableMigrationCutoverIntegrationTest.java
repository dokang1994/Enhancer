package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        Path manifestRoot = temporaryRoot.resolve("manifests");
        Path queueRoot = temporaryRoot.resolve("queue");
        Path runtimeRoot = temporaryRoot.resolve("runtime");
        Path fence = temporaryRoot.resolve("stopped-owner.fence");
        Path binding = temporaryRoot.resolve("effect-ledger.binding");
        byte[] fenceBytes = "held-fence".getBytes(StandardCharsets.UTF_8);
        byte[] bindingBytes = "immutable-effect-ledger-binding"
                .getBytes(StandardCharsets.UTF_8);
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
                temporaryRoot.resolve("work-spool"),
                new TransportMessage(
                        DeliveryDestination.queue("work"), workMessage));
        Path ingressPoint = spool(
                temporaryRoot.resolve("ingress-spool"),
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
                temporaryRoot.resolve("result-spool"),
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
        List<String> events = new ArrayList<>();
        CoordinatedDurableMigrationCutover.Hook hook =
                new CoordinatedDurableMigrationCutover.Hook() {
                    @Override
                    public void afterCandidatesPrepared(List<Path> candidates)
                            throws IOException {
                        assertEquals(3, candidates.size());
                        assertTrue(candidates.stream().allMatch(Files::isRegularFile));
                        assertSourceBytes(sourceArtifacts, sourceBytes);
                        events.add("CANDIDATES_READY");
                    }

                    @Override
                    public void beforePublication(
                            CoordinatedDurableMigrationCutover.PublicationPoint point,
                            Path source) {
                        events.add(point.name());
                    }
                };
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

        CoordinatedDurableMigrationCutover.Result result =
                new CoordinatedDurableMigrationCutover(hook).execute(plan);

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
        assertEquals(manifest,
                new FileSystemSubmissionManifestStore(manifestRoot)
                        .resolve(SUBMISSION_ID));
        SchedulerQueueState migratedQueue =
                new FileSystemSchedulerQueueStore(queueRoot).resolve(QUEUE_ID);
        assertEquals(queueState.admittedWork(), migratedQueue.admittedWork());
        assertEquals(queueState.pendingWork(), migratedQueue.pendingWork());
        assertEquals(queueState.admissionOrder(), migratedQueue.admissionOrder());
        assertEquals(queueState.maximumExpeditedBurst(),
                migratedQueue.maximumExpeditedBurst());
        assertEquals(queueState.consecutiveExpeditedClaims(),
                migratedQueue.consecutiveExpeditedClaims());
        AgentRuntimeState migratedRuntime =
                new FileSystemAgentRuntimeStateStore(runtimeRoot).resolve(GOAL_ID);
        assertEquals(runtimeState.goal(), migratedRuntime.goal());
        assertEquals(runtimeState.agentRuns(), migratedRuntime.agentRuns());
        assertEquals(runtimeState.retryDecisions(), migratedRuntime.retryDecisions());
        assertArrayEquals(sourceBytes.get(3), Files.readAllBytes(workPoint));
        assertArrayEquals(sourceBytes.get(4), Files.readAllBytes(resultPoint));
        assertArrayEquals(sourceBytes.get(5), Files.readAllBytes(ingressPoint));
        assertArrayEquals(bindingBytes, Files.readAllBytes(binding));
        assertArrayEquals(fenceBytes, Files.readAllBytes(fence));
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
}
