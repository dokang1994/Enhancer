package com.enhancer.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.DurableMessageEnvelopeCodec;
import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.ModelWorkPayload;
import com.enhancer.bus.WorkPayload;
import com.enhancer.loop.ApprovedTask;
import com.enhancer.model.ModelInvokeTool;
import com.enhancer.model.ModelRequest;
import com.enhancer.tool.ToolRequest;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemModelRunRecordStoreIntegrationTest {
    private static final int ENVELOPE_MAGIC = 0x454E5234;
    private static final int DIGEST_BYTES = 32;
    private static final int HEADER_BYTES =
            Integer.BYTES + Long.BYTES + Integer.BYTES + DIGEST_BYTES;
    private static final int MAX_PAYLOAD_BYTES = 4 * 1024 * 1024;
    private static final String V1_ID = "11111111-1111-1111-1111-111111111111";
    private static final String V2_ID = "44444444-4444-4444-4444-444444444444";

    @TempDir
    Path storageRoot;

    @Test
    void roundTripsEveryModelComponentAndExactReplayDoesNotRewrite() throws Exception {
        ModelRunRecord record = richRecord(false);
        ModelRunRecordStore store = new FileSystemRunRecordStore(storageRoot);
        StoredRunRecord first = store.persistModel(V2_ID, record);
        Path artifact = artifact(storageRoot, V2_ID);
        byte[] firstBytes = Files.readAllBytes(artifact);
        FileTime fixedTime = FileTime.from(Instant.parse("2026-08-31T01:00:00Z"));
        Files.setLastModifiedTime(artifact, fixedTime);

        ResolvedModelRunRecord resolved = new FileSystemRunRecordStore(storageRoot)
                .resolveModel(first.reference());
        StoredRunRecord replay = new FileSystemRunRecordStore(storageRoot)
                .persistModel(V2_ID, record);

        assertEquals(record, resolved.record());
        assertEquals(first, resolved.metadata());
        assertEquals(first, replay);
        assertTrue(Arrays.equals(firstBytes, Files.readAllBytes(artifact)));
        assertEquals(fixedTime, Files.getLastModifiedTime(artifact));
    }

    @Test
    void typedResolversAndIdentityReuseFailClosedAcrossKnownKinds() throws Exception {
        Path v1FirstRoot = storageRoot.resolve("v1-first");
        FileSystemRunRecordStore v1First = new FileSystemRunRecordStore(v1FirstRoot);
        StoredRunRecord v1 = v1First.persist(V1_ID, richRecord(false).lifecycleRecord());
        byte[] v1Bytes = Files.readAllBytes(artifact(v1FirstRoot, V1_ID));

        UnsupportedRunRecordKindException modelResolveV1 = assertThrows(
                UnsupportedRunRecordKindException.class,
                () -> v1First.resolveModel(v1.reference()));
        assertEquals(RunRecordKind.MODEL_RUN_RECORD_V2, modelResolveV1.expectedKind());
        assertEquals(RunRecordKind.RUN_RECORD_V1, modelResolveV1.actualKind());
        assertThrows(
                UnsupportedRunRecordKindException.class,
                () -> v1First.persistModel(V1_ID, richRecord(false)));
        assertTrue(Arrays.equals(v1Bytes, Files.readAllBytes(artifact(v1FirstRoot, V1_ID))));

        Path v2FirstRoot = storageRoot.resolve("v2-first");
        FileSystemRunRecordStore v2First = new FileSystemRunRecordStore(v2FirstRoot);
        StoredRunRecord v2 = v2First.persistModel(V2_ID, richRecord(false));
        byte[] v2Bytes = Files.readAllBytes(artifact(v2FirstRoot, V2_ID));

        UnsupportedRunRecordKindException legacyResolveV2 = assertThrows(
                UnsupportedRunRecordKindException.class,
                () -> v2First.resolve(v2.reference()));
        assertEquals(RunRecordKind.RUN_RECORD_V1, legacyResolveV2.expectedKind());
        assertEquals(RunRecordKind.MODEL_RUN_RECORD_V2, legacyResolveV2.actualKind());
        assertThrows(
                UnsupportedRunRecordKindException.class,
                () -> v2First.persist(V2_ID, richRecord(false).lifecycleRecord()));
        assertTrue(Arrays.equals(v2Bytes, Files.readAllBytes(artifact(v2FirstRoot, V2_ID))));
    }

    @Test
    void opaqueListingObservesBothKindsWithoutResolvingEither() throws Exception {
        FileSystemRunRecordStore store = new FileSystemRunRecordStore(storageRoot);
        store.persist(V1_ID, richRecord(false).lifecycleRecord());
        store.persistModel(V2_ID, richRecord(false));
        Files.setLastModifiedTime(artifact(storageRoot, V1_ID), FileTime.fromMillis(1));
        Files.setLastModifiedTime(artifact(storageRoot, V2_ID), FileTime.fromMillis(2));

        assertEquals(
                List.of("run-record/" + V1_ID, "run-record/" + V2_ID),
                store.references());
        assertEquals(
                List.of("run-record/" + V2_ID, "run-record/" + V1_ID),
                store.recentReferences(2));
    }

    @Test
    void changedModelContentCannotReuseAnExistingIdentity() throws Exception {
        FileSystemRunRecordStore store = new FileSystemRunRecordStore(storageRoot);
        ModelRunRecord original = richRecord(false);
        store.persistModel(V2_ID, original);
        byte[] originalBytes = Files.readAllBytes(artifact(storageRoot, V2_ID));
        ModelRunRecord changed = new ModelRunRecord(
                original.workItemId(),
                "changed-independent-capability",
                original.workMessage(),
                original.modelRequest(),
                original.lifecycleRecord());

        assertThrows(IOException.class, () -> store.persistModel(V2_ID, changed));
        assertTrue(Arrays.equals(originalBytes, Files.readAllBytes(artifact(storageRoot, V2_ID))));
    }

    @Test
    void rejectsUnknownCorruptTruncatedTrailingAndOversizedArtifacts() throws Exception {
        assertCorruptedAfter("unknown", payload -> {
            ByteBuffer.wrap(payload).putInt(99);
            return payload;
        });
        assertCorruptedAfter("digest", payload -> payload, envelope -> {
            envelope[envelope.length - 1] ^= 1;
            return envelope;
        });
        assertCorruptedAfter("truncated", payload -> payload, envelope ->
                Arrays.copyOf(envelope, envelope.length - 1));
        assertCorruptedAfter("trailing", payload -> Arrays.copyOf(payload, payload.length + 1));

        Path oversizedRoot = storageRoot.resolve("oversized");
        Files.createDirectories(oversizedRoot);
        Path oversized = artifact(oversizedRoot, V2_ID);
        Files.write(oversized, new byte[HEADER_BYTES + MAX_PAYLOAD_BYTES + 1]);
        assertThrows(
                CorruptedRunRecordException.class,
                () -> new FileSystemRunRecordStore(oversizedRoot)
                        .resolveModel("run-record/" + V2_ID));
    }

    @Test
    void rejectsForeignNestedPayloadAndStructuralWorkIdentityTampering() throws Exception {
        Path foreignRoot = storageRoot.resolve("foreign");
        FileSystemRunRecordStore foreignStore = new FileSystemRunRecordStore(foreignRoot);
        ModelRunRecord original = richRecord(false);
        foreignStore.persistModel(V2_ID, original);
        Path foreignArtifact = artifact(foreignRoot, V2_ID);
        byte[] originalPayload = payload(foreignArtifact);
        int nestedLengthOffset = nestedLengthOffset(originalPayload);
        int nestedLength = ByteBuffer.wrap(originalPayload).getInt(nestedLengthOffset);
        int nestedStart = nestedLengthOffset + Integer.BYTES;
        int nestedEnd = nestedStart + nestedLength;

        ModelWorkPayload modelPayload = (ModelWorkPayload) original.workMessage().payload();
        WorkPayload legacyPayload = new WorkPayload(
                modelPayload.taskRevision(),
                modelPayload.snapshotId(),
                modelPayload.allowedTools());
        MessageEnvelope legacyEnvelope = new MessageEnvelope(
                original.workMessage().messageId(),
                original.workMessage().correlationId(),
                original.workMessage().causationId(),
                original.workMessage().logicalRunId(),
                original.workMessage().producer(),
                original.workMessage().occurredAt(),
                legacyPayload);
        byte[] foreignNested = new DurableMessageEnvelopeCodec().encode(legacyEnvelope);
        byte[] foreignPayload = concat(
                Arrays.copyOfRange(originalPayload, 0, nestedLengthOffset),
                ByteBuffer.allocate(Integer.BYTES).putInt(foreignNested.length).array(),
                foreignNested,
                Arrays.copyOfRange(originalPayload, nestedEnd, originalPayload.length));
        rewriteEnvelope(foreignArtifact, foreignPayload);
        assertThrows(
                CorruptedRunRecordException.class,
                () -> foreignStore.resolveModel("run-record/" + V2_ID));

        Path identityRoot = storageRoot.resolve("identity");
        FileSystemRunRecordStore identityStore = new FileSystemRunRecordStore(identityRoot);
        identityStore.persistModel(V2_ID, original);
        Path identityArtifact = artifact(identityRoot, V2_ID);
        byte[] identityPayload = payload(identityArtifact);
        int workIdentityLength = ByteBuffer.wrap(identityPayload).getInt(Integer.BYTES);
        assertEquals(36, workIdentityLength);
        byte[] messageIdentity = original.workMessage().messageId()
                .getBytes(StandardCharsets.UTF_8);
        System.arraycopy(
                messageIdentity,
                0,
                identityPayload,
                Integer.BYTES * 2,
                messageIdentity.length);
        rewriteEnvelope(identityArtifact, identityPayload);
        assertThrows(
                CorruptedRunRecordException.class,
                () -> identityStore.resolveModel("run-record/" + V2_ID));
    }

    @Test
    void rejectsNoncanonicalLifecycleCollectionOrderEvenWhenTheValueIsValid()
            throws Exception {
        Path root = storageRoot.resolve("noncanonical");
        FileSystemRunRecordStore store = new FileSystemRunRecordStore(root);
        store.persistModel(V2_ID, richRecord(true));
        Path artifact = artifact(root, V2_ID);
        byte[] canonical = payload(artifact);
        int entriesStart = lifecycleAllowedToolsEntriesOffset(canonical);
        int firstEnd = stringEnd(canonical, entriesStart);
        int secondEnd = stringEnd(canonical, firstEnd);
        byte[] reordered = concat(
                Arrays.copyOfRange(canonical, 0, entriesStart),
                Arrays.copyOfRange(canonical, firstEnd, secondEnd),
                Arrays.copyOfRange(canonical, entriesStart, firstEnd),
                Arrays.copyOfRange(canonical, secondEnd, canonical.length));
        rewriteEnvelope(artifact, reordered);

        assertThrows(
                CorruptedRunRecordException.class,
                () -> store.resolveModel("run-record/" + V2_ID));
    }

    private ModelRunRecord richRecord(boolean extraTool) {
        ModelRunRecord base = ModelRunRecordTestFixture.record();
        ModelWorkPayload basePayload = (ModelWorkPayload) base.workMessage().payload();
        Set<String> tools = extraTool
                ? Set.of(ModelInvokeTool.NAME, "read-file")
                : basePayload.allowedTools();
        ModelWorkPayload payload = new ModelWorkPayload(
                basePayload.taskRevision(),
                basePayload.snapshotId(),
                tools,
                basePayload.executionInput());
        MessageEnvelope message = new MessageEnvelope(
                base.workMessage().messageId(),
                base.workMessage().correlationId(),
                Optional.of("33333333-3333-3333-3333-333333333333"),
                base.workMessage().logicalRunId(),
                base.workMessage().producer(),
                Instant.parse("2026-08-31T00:00:00.123456789Z"),
                payload);
        ModelRequest request = new ModelRequest(
                base.modelRequest().correlationId(),
                "Analyze supplementary text \uD83D\uDE80 precisely.",
                base.modelRequest().modelClass(),
                base.modelRequest().timeout(),
                base.modelRequest().maxResponseLength());
        ToolRequest toolRequest = new ToolRequest(
                ModelInvokeTool.NAME,
                request.correlationId(),
                Map.of(
                        ModelInvokeTool.PROMPT_PATH_ARGUMENT,
                                payload.executionInput().targetPath(),
                        ModelInvokeTool.MODEL_CLASS_ARGUMENT, request.modelClass(),
                        ModelInvokeTool.TIMEOUT_MILLIS_ARGUMENT,
                                Long.toString(request.timeout().toMillis()),
                        ModelInvokeTool.MAX_RESPONSE_LENGTH_ARGUMENT,
                                Integer.toString(request.maxResponseLength())));
        RunRecord oldLifecycle = base.lifecycleRecord();
        RunRecord lifecycle = new RunRecord(
                oldLifecycle.logicalRunId(),
                oldLifecycle.recordedAt(),
                new ApprovedTask(
                        oldLifecycle.approvedTask().taskId(),
                        oldLifecycle.approvedTask().description(),
                        oldLifecycle.approvedTask().approvalEvidence(),
                        tools,
                        oldLifecycle.approvedTask().sourceDocument()),
                toolRequest,
                new PolicyDecision(
                        oldLifecycle.policyDecision().status(),
                        oldLifecycle.policyDecision().projectRoot(),
                        tools,
                        oldLifecycle.policyDecision().deniedTools(),
                        oldLifecycle.policyDecision().maxReadBytes(),
                        oldLifecycle.policyDecision().timeoutMillis()),
                oldLifecycle.toolResult(),
                oldLifecycle.expectedContentSha256(),
                oldLifecycle.verification(),
                oldLifecycle.iterations(),
                oldLifecycle.workerStopReason(),
                oldLifecycle.finalStopReason());
        return new ModelRunRecord(
                base.workItemId(),
                base.requiredCapability(),
                message,
                request,
                lifecycle);
    }

    private void assertCorruptedAfter(String name, PayloadMutation mutation) throws Exception {
        assertCorruptedAfter(name, mutation, envelope -> envelope);
    }

    private void assertCorruptedAfter(
            String name,
            PayloadMutation mutation,
            EnvelopeMutation envelopeMutation) throws Exception {
        Path root = storageRoot.resolve(name);
        FileSystemRunRecordStore store = new FileSystemRunRecordStore(root);
        store.persistModel(V2_ID, richRecord(false));
        Path artifact = artifact(root, V2_ID);
        byte[] envelope;
        if (name.equals("digest") || name.equals("truncated")) {
            envelope = Files.readAllBytes(artifact);
        } else {
            byte[] changedPayload = mutation.apply(payload(artifact));
            rewriteEnvelope(artifact, changedPayload);
            envelope = Files.readAllBytes(artifact);
        }
        Files.write(artifact, envelopeMutation.apply(envelope));
        assertThrows(
                CorruptedRunRecordException.class,
                () -> store.resolveModel("run-record/" + V2_ID));
    }

    private int nestedLengthOffset(byte[] payload) {
        int offset = Integer.BYTES;
        offset = stringEnd(payload, offset);
        return stringEnd(payload, offset);
    }

    private int lifecycleAllowedToolsEntriesOffset(byte[] payload) {
        int offset = nestedLengthOffset(payload);
        int nestedLength = ByteBuffer.wrap(payload).getInt(offset);
        offset += Integer.BYTES + nestedLength;
        offset = stringEnd(payload, offset);
        offset = stringEnd(payload, offset);
        offset = stringEnd(payload, offset);
        offset += Long.BYTES + Integer.BYTES + Integer.BYTES;
        offset = stringEnd(payload, offset);
        offset += Long.BYTES;
        offset = stringEnd(payload, offset);
        offset = stringEnd(payload, offset);
        offset = stringEnd(payload, offset);
        int count = ByteBuffer.wrap(payload).getInt(offset);
        assertEquals(2, count);
        return offset + Integer.BYTES;
    }

    private int stringEnd(byte[] bytes, int offset) {
        int length = ByteBuffer.wrap(bytes).getInt(offset);
        return offset + Integer.BYTES + length;
    }

    private byte[] payload(Path artifact) throws Exception {
        byte[] envelope = Files.readAllBytes(artifact);
        return Arrays.copyOfRange(envelope, HEADER_BYTES, envelope.length);
    }

    private void rewriteEnvelope(Path artifact, byte[] payload) throws Exception {
        byte[] oldEnvelope = Files.readAllBytes(artifact);
        long storedAtMillis = ByteBuffer.wrap(oldEnvelope).getLong(Integer.BYTES);
        ByteBuffer digestInput = ByteBuffer.allocate(
                        Integer.BYTES + Long.BYTES + Integer.BYTES + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAtMillis)
                .putInt(payload.length)
                .put(payload);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(digestInput.array());
        Files.write(
                artifact,
                ByteBuffer.allocate(HEADER_BYTES + payload.length)
                        .putInt(ENVELOPE_MAGIC)
                        .putLong(storedAtMillis)
                        .putInt(payload.length)
                        .put(digest)
                        .put(payload)
                        .array());
    }

    private static Path artifact(Path root, String recordId) {
        return root.resolve(recordId + ".run-record");
    }

    private static byte[] concat(byte[]... parts) {
        int length = Arrays.stream(parts).mapToInt(part -> part.length).sum();
        byte[] joined = new byte[length];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, joined, offset, part.length);
            offset += part.length;
        }
        return joined;
    }

    @FunctionalInterface
    private interface PayloadMutation {
        byte[] apply(byte[] payload) throws Exception;
    }

    @FunctionalInterface
    private interface EnvelopeMutation {
        byte[] apply(byte[] envelope) throws Exception;
    }
}
