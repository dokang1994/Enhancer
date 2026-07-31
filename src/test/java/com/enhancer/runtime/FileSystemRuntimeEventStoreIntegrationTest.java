package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.kernel.VerificationStatus;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemRuntimeEventStoreIntegrationTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000003001";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000003002";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000003003";
    private static final int DIGEST_OFFSET =
            Integer.BYTES + Long.BYTES + Integer.BYTES;
    private static final int PAYLOAD_OFFSET = DIGEST_OFFSET + 32;

    @TempDir
    Path temporaryRoot;

    @Test
    void appendsRecoversAndReplaysWithoutRewritingTheArtifact()
            throws Exception {
        Path root = temporaryRoot.resolve("runtime-events");
        FileSystemRuntimeEventStore store =
                new FileSystemRuntimeEventStore(root);
        RuntimeEvent first = event("verification/1");

        assertEquals(RuntimeEventAppendResult.APPENDED, store.append(first));
        RuntimeEventStream recovered =
                new FileSystemRuntimeEventStore(root).resolve(GOAL_ID);
        assertEquals(1, recovered.revision());
        assertEquals(List.of(first), recovered.events());

        Path artifact = root.resolve(GOAL_ID + ".runtime-events");
        byte[] beforeReplay = Files.readAllBytes(artifact);
        assertEquals(RuntimeEventAppendResult.REPLAYED, store.append(first));
        assertArrayEquals(beforeReplay, Files.readAllBytes(artifact));

        RuntimeEvent second = RuntimeEvent.create(
                Instant.parse("2026-07-31T07:01:00Z"),
                binding(),
                AGENT_RUN_ID,
                Optional.of(first.eventId()),
                "runtime-event-store-test",
                new RuntimeEventDetail.WorkItemTerminated(
                        WorkItemDisposition.VERIFIED_COMPLETED),
                List.of(reference(
                        RuntimeEventReferenceKind.RUN_RECORD,
                        "termination/1")));
        assertEquals(RuntimeEventAppendResult.APPENDED, store.append(second));
        RuntimeEventStream advanced =
                new FileSystemRuntimeEventStore(root).resolve(GOAL_ID);
        assertEquals(2, advanced.revision());
        assertEquals(List.of(first, second), advanced.events());
        assertTrue(advanced.isValidSuccessorOf(recovered));
    }

    @Test
    void rejectsMissingCorruptTrailingUnsupportedAndOversizedArtifacts()
            throws Exception {
        Path root = temporaryRoot.resolve("invalid-runtime-events");
        FileSystemRuntimeEventStore store =
                new FileSystemRuntimeEventStore(root);
        assertThrows(MissingRuntimeEventStreamException.class, () ->
                store.resolve(GOAL_ID));

        store.append(event("corrupt/1"));
        Path artifact = root.resolve(GOAL_ID + ".runtime-events");
        byte[] corrupt = Files.readAllBytes(artifact);
        corrupt[corrupt.length - 1] ^= 1;
        Files.write(artifact, corrupt);
        assertThrows(CorruptedRuntimeEventStreamException.class, () ->
                store.resolve(GOAL_ID));

        Files.delete(artifact);
        store.append(event("trailing/1"));
        byte[] trailing = Files.readAllBytes(artifact);
        Files.write(
                artifact,
                ByteBuffer.allocate(trailing.length + 1)
                        .put(trailing)
                        .put((byte) 1)
                        .array());
        assertThrows(CorruptedRuntimeEventStreamException.class, () ->
                store.resolve(GOAL_ID));

        Files.delete(artifact);
        store.append(event("unsupported/1"));
        byte[] unsupported = Files.readAllBytes(artifact);
        ByteBuffer.wrap(unsupported).putInt(
                PAYLOAD_OFFSET,
                RuntimeEventStream.CURRENT_SCHEMA_VERSION + 1);
        replaceDigest(unsupported);
        Files.write(artifact, unsupported);
        CorruptedRuntimeEventStreamException versionFailure = assertThrows(
                CorruptedRuntimeEventStreamException.class,
                () -> store.resolve(GOAL_ID));
        assertTrue(versionFailure.getMessage().contains("version"));

        Files.delete(artifact);
        try (FileChannel channel = FileChannel.open(
                artifact,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            channel.position(
                    FileSystemRuntimeEventStore.HEADER_BYTES
                            + FileSystemRuntimeEventStore.MAX_STATE_BYTES);
            channel.write(ByteBuffer.wrap(new byte[] {1}));
        }
        assertThrows(CorruptedRuntimeEventStreamException.class, () ->
                store.resolve(GOAL_ID));
    }

    @Test
    void changedIdentityReuseAndForeignBindingFailWithoutChangingTheStream()
            throws Exception {
        Path root = temporaryRoot.resolve("foreign-runtime-events");
        FileSystemRuntimeEventStore store =
                new FileSystemRuntimeEventStore(root);
        RuntimeEvent original = event("foreign/1");
        store.append(original);
        Path artifact = root.resolve(GOAL_ID + ".runtime-events");
        byte[] before = Files.readAllBytes(artifact);
        RuntimeEvent changed = new RuntimeEvent(
                RuntimeEvent.SCHEMA_VERSION,
                original.eventId(),
                original.kind(),
                original.occurredAt().plusSeconds(1),
                original.binding(),
                original.agentRunId(),
                original.causationId(),
                original.producerId(),
                original.detail(),
                original.authoritativeReferences());

        assertThrows(IOException.class, () -> store.append(changed));
        assertArrayEquals(before, Files.readAllBytes(artifact));

        RuntimeEvent foreign = RuntimeEvent.create(
                Instant.parse("2026-07-31T07:02:00Z"),
                new RuntimeEventBinding(
                        GOAL_ID,
                        "00000000-0000-0000-0000-000000003099",
                        taskRevision(),
                        "b".repeat(64),
                        "logical-run-runtime-events",
                        "correlation-runtime-events"),
                AGENT_RUN_ID,
                Optional.empty(),
                "runtime-event-store-test",
                new RuntimeEventDetail.VerificationRecorded(
                        VerificationStatus.REJECTED),
                List.of(reference(
                        RuntimeEventReferenceKind.EVIDENCE,
                        "foreign/2")));

        assertThrows(IOException.class, () -> store.append(foreign));
        assertArrayEquals(before, Files.readAllBytes(artifact));
    }

    @Test
    void symbolicRuntimeRootIsRejectedWhenThePlatformSupportsLinks()
            throws Exception {
        Path target = temporaryRoot.resolve("runtime-event-target");
        Path link = temporaryRoot.resolve("runtime-event-link");
        Files.createDirectory(target);
        new FileSystemRuntimeEventStore(target).append(event("link/1"));
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException exception) {
            Assumptions.assumeTrue(
                    false,
                    "symbolic links are unavailable: " + exception.getMessage());
        }

        FileSystemRuntimeEventStore store =
                new FileSystemRuntimeEventStore(link);
        assertThrows(IOException.class, () -> store.append(event("link/2")));
        assertThrows(
                CorruptedRuntimeEventStreamException.class,
                () -> store.resolve(GOAL_ID));
    }

    private static RuntimeEvent event(String reference) {
        return RuntimeEvent.create(
                Instant.parse("2026-07-31T07:00:00Z"),
                binding(),
                AGENT_RUN_ID,
                Optional.empty(),
                "runtime-event-store-test",
                new RuntimeEventDetail.VerificationRecorded(
                        VerificationStatus.VERIFIED),
                List.of(reference(
                        RuntimeEventReferenceKind.EVIDENCE,
                        reference)));
    }

    private static RuntimeEventBinding binding() {
        return new RuntimeEventBinding(
                GOAL_ID,
                WORK_ITEM_ID,
                taskRevision(),
                "b".repeat(64),
                "logical-run-runtime-events",
                "correlation-runtime-events");
    }

    private static ApprovedTaskRevision taskRevision() {
        return new ApprovedTaskRevision(
                "implement-runtime-event-store-contract",
                "CURRENT_TASK.md",
                "a".repeat(64));
    }

    private static RuntimeEventReference reference(
            RuntimeEventReferenceKind kind,
            String value) {
        return new RuntimeEventReference(
                kind, value, Optional.of("c".repeat(64)));
    }

    private static void replaceDigest(byte[] envelope) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        int magic = buffer.getInt(0);
        long storedAt = buffer.getLong(Integer.BYTES);
        int payloadLength = buffer.getInt(Integer.BYTES + Long.BYTES);
        byte[] payload = new byte[payloadLength];
        System.arraycopy(envelope, PAYLOAD_OFFSET, payload, 0, payloadLength);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                ByteBuffer.allocate(
                                Integer.BYTES
                                        + Long.BYTES
                                        + Integer.BYTES
                                        + payload.length)
                        .putInt(magic)
                        .putLong(storedAt)
                        .putInt(payloadLength)
                        .put(payload)
                        .array());
        System.arraycopy(digest, 0, envelope, DIGEST_OFFSET, digest.length);
    }
}
