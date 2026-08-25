package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemAgentRuntimeStateMigrationInspectionTest {
    private static final int ENVELOPE_MAGIC = 0x45415231;
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000002501";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000002502";
    private static final String MESSAGE_ID =
            "00000000-0000-0000-0000-000000002503";
    private static final long STORED_AT_MILLIS = 1_777_777_777L;

    @TempDir
    Path temporaryRoot;

    @Test
    void inspectsExactSchemaV4ReadFileStateAsV5WithoutWriting()
            throws Exception {
        Path root = temporaryRoot.resolve("runtime");
        Files.createDirectories(root);
        Path artifact = root.resolve(GOAL_ID + ".agent-runtime");
        byte[] source = schemaV4Envelope();
        Files.write(artifact, source);
        FileTime sourceTime = Files.getLastModifiedTime(artifact);
        FileTime rootTime = Files.getLastModifiedTime(root);
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(root);

        assertThrows(
                CorruptedAgentRuntimeStateException.class,
                () -> store.resolve(GOAL_ID));
        AgentRuntimeMigrationInspection inspection =
                store.inspectForMigration(GOAL_ID).orElseThrow();

        assertEquals(4, inspection.sourceSchemaVersion());
        assertFalse(inspection.alreadyCurrent());
        assertArrayEquals(source, inspection.sourceBytes());
        assertEquals(AgentRuntimeState.CURRENT_SCHEMA_VERSION,
                inspection.state().schemaVersion());
        assertEquals(expectedWorkItem(), inspection.state().goal().workItem());
        assertEquals(RuntimeGoalStatus.ACCEPTED,
                inspection.state().goal().status());
        assertArrayEquals(source, Files.readAllBytes(artifact));
        assertEquals(sourceTime, Files.getLastModifiedTime(artifact));
        assertEquals(rootTime, Files.getLastModifiedTime(root));
        try (Stream<Path> entries = Files.list(root)) {
            assertEquals(Set.of(artifact), Set.copyOf(entries.toList()));
        }
    }

    @Test
    void inspectsCurrentStateWithoutRewriteAndDefensivelyRetainsSourceBytes()
            throws Exception {
        Path root = temporaryRoot.resolve("current-runtime");
        FileSystemAgentRuntimeStateStore store =
                new FileSystemAgentRuntimeStateStore(root);
        store.create(AgentRuntimeState.initial(GOAL_ID, expectedWorkItem()));
        Path artifact = root.resolve(GOAL_ID + ".agent-runtime");
        byte[] source = Files.readAllBytes(artifact);
        FileTime sourceTime = Files.getLastModifiedTime(artifact);
        FileTime rootTime = Files.getLastModifiedTime(root);

        AgentRuntimeMigrationInspection inspection =
                store.inspectForMigration(GOAL_ID).orElseThrow();
        byte[] callerCopy = inspection.sourceBytes();
        callerCopy[0] ^= 0x7f;

        assertEquals(AgentRuntimeState.CURRENT_SCHEMA_VERSION,
                inspection.sourceSchemaVersion());
        assertTrue(inspection.alreadyCurrent());
        assertEquals(expectedWorkItem(), inspection.state().goal().workItem());
        assertArrayEquals(source, inspection.sourceBytes());
        assertArrayEquals(source, Files.readAllBytes(artifact));
        assertEquals(sourceTime, Files.getLastModifiedTime(artifact));
        assertEquals(rootTime, Files.getLastModifiedTime(root));
    }

    @Test
    void absentInspectionDoesNotCreateItsRoot() throws Exception {
        Path root = temporaryRoot.resolve("absent-runtime");

        assertTrue(new FileSystemAgentRuntimeStateStore(root)
                .inspectForMigration(GOAL_ID)
                .isEmpty());
        assertFalse(Files.exists(root));
    }

    @Test
    void unsupportedSchemaIsRejectedWithoutMutation() throws Exception {
        Path root = temporaryRoot.resolve("future-runtime");
        Files.createDirectories(root);
        Path artifact = root.resolve(GOAL_ID + ".agent-runtime");
        byte[] payload = schemaV4Payload();
        ByteBuffer.wrap(payload).putInt(
                AgentRuntimeState.CURRENT_SCHEMA_VERSION + 1);
        byte[] source = envelope(payload);
        Files.write(artifact, source);
        FileTime sourceTime = Files.getLastModifiedTime(artifact);
        FileTime rootTime = Files.getLastModifiedTime(root);

        assertThrows(
                CorruptedAgentRuntimeStateException.class,
                () -> new FileSystemAgentRuntimeStateStore(root)
                        .inspectForMigration(GOAL_ID));

        assertArrayEquals(source, Files.readAllBytes(artifact));
        assertEquals(sourceTime, Files.getLastModifiedTime(artifact));
        assertEquals(rootTime, Files.getLastModifiedTime(root));
    }

    private static byte[] schemaV4Envelope() throws Exception {
        return envelope(schemaV4Payload());
    }

    private static byte[] envelope(byte[] payload) throws Exception {
        ByteBuffer digestInput = ByteBuffer.allocate(
                        Integer.BYTES + Long.BYTES + Integer.BYTES + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(STORED_AT_MILLIS)
                .putInt(payload.length)
                .put(payload);
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(digestInput.array());
        return ByteBuffer.allocate(
                        Integer.BYTES + Long.BYTES + Integer.BYTES + digest.length
                                + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(STORED_AT_MILLIS)
                .putInt(payload.length)
                .put(digest)
                .put(payload)
                .array();
    }

    private static byte[] schemaV4Payload() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(4);
            writeString(output, "agent-runtime-state");
            writeString(output, GOAL_ID);
            output.writeLong(0L);
            output.writeLong(0L);
            writeString(output, RuntimeGoalStatus.ACCEPTED.name());
            writeString(output, WORK_ITEM_ID);
            writeString(output, "independent-capability");
            writeLegacyWorkEnvelope(output);
            output.writeInt(0);
            output.writeInt(0);
            output.writeInt(0);
            output.writeInt(0);
            output.writeBoolean(false);
        }
        return bytes.toByteArray();
    }

    private static void writeLegacyWorkEnvelope(DataOutputStream output)
            throws Exception {
        writeString(output, MessageEnvelope.ENVELOPE_VERSION);
        writeString(output, MESSAGE_ID);
        writeString(output, "runtime-v4-correlation");
        output.writeBoolean(false);
        writeString(output, "runtime-v4-logical-run");
        writeString(output, "runtime-v4-producer");
        Instant occurredAt = Instant.parse("2026-08-25T05:06:07.008000009Z");
        output.writeLong(occurredAt.getEpochSecond());
        output.writeInt(occurredAt.getNano());
        writeString(output, "work");
        writeString(output, "runtime-v4-migration");
        writeString(output, "CURRENT_TASK.md");
        writeString(output, "a".repeat(64));
        writeString(output, "b".repeat(64));
        output.writeInt(2);
        writeString(output, "model-invoke");
        writeString(output, "read-file");
        output.writeBoolean(false);
    }

    private static WorkItem expectedWorkItem() {
        WorkPayload payload = new WorkPayload(
                new ApprovedTaskRevision(
                        "runtime-v4-migration",
                        "CURRENT_TASK.md",
                        "a".repeat(64)),
                "b".repeat(64),
                Set.of("model-invoke", "read-file"),
                Optional.empty());
        return new WorkItem(
                WORK_ITEM_ID,
                "independent-capability",
                new MessageEnvelope(
                        MESSAGE_ID,
                        "runtime-v4-correlation",
                        Optional.empty(),
                        "runtime-v4-logical-run",
                        "runtime-v4-producer",
                        Instant.parse("2026-08-25T05:06:07.008000009Z"),
                        payload));
    }

    private static void writeString(DataOutputStream output, String value)
            throws Exception {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(encoded.length);
        output.write(encoded);
    }
}
