package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.runtime.FileSystemPendingFinalizationStore;
import com.enhancer.runtime.PendingFinalization;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliSchedulerMigrateCycleCheckpointIntegrationTest {
    private static final int ENVELOPE_MAGIC = 0x50464331;
    private static final int HEADER_BYTES =
            Integer.BYTES + Long.BYTES + Integer.BYTES + 32;
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000821";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000822";
    private static final String REFERENCE =
            "run-record/00000000-0000-0000-0000-000000000823";

    @TempDir
    Path temporaryRoot;

    @Test
    void reportsAbsentWithoutCreatingTheCheckpointRoot() {
        Path checkpointRoot = temporaryRoot.resolve("absent");

        Captured captured = execute(checkpointRoot);

        assertEquals(0, captured.exitCode());
        assertEquals("ABSENT", value(captured.stdout(), "status"));
        assertEquals("NONE", value(captured.stdout(), "sourceSchemaVersion"));
        assertEquals("2", value(captured.stdout(), "targetSchemaVersion"));
        assertEquals("", captured.stderr());
        assertFalse(Files.exists(checkpointRoot));
    }

    @Test
    void migratesSchemaV1ForNormalRecoveryAndDoesNotRewriteCurrentState()
            throws Exception {
        Path checkpointRoot = temporaryRoot.resolve("migration");
        byte[] old = schemaV1Envelope();
        Files.createDirectories(checkpointRoot);
        Files.write(artifact(checkpointRoot), old);

        Captured migrated = execute(checkpointRoot);

        assertEquals(0, migrated.exitCode());
        assertEquals("MIGRATED", value(migrated.stdout(), "status"));
        assertEquals("1", value(migrated.stdout(), "sourceSchemaVersion"));
        assertEquals("2", value(migrated.stdout(), "targetSchemaVersion"));
        assertEquals(
                Optional.of(new PendingFinalization(
                        GOAL_ID,
                        AGENT_RUN_ID,
                        Optional.of(REFERENCE),
                        Optional.empty())),
                new FileSystemPendingFinalizationStore(checkpointRoot)
                        .findPending());
        byte[] current = Files.readAllBytes(artifact(checkpointRoot));

        Captured replay = execute(checkpointRoot);

        assertEquals(0, replay.exitCode());
        assertEquals(
                "ALREADY_CURRENT",
                value(replay.stdout(), "status"));
        assertEquals("2", value(replay.stdout(), "sourceSchemaVersion"));
        assertArrayEquals(
                current, Files.readAllBytes(artifact(checkpointRoot)));
        assertTrue(migrated.stdout().length()
                <= EnhancerCli.MAX_DIAGNOSTIC_CHARACTERS);
    }

    @Test
    void corruptInputFailsClosedAndRemainsByteIdentical() throws Exception {
        Path checkpointRoot = temporaryRoot.resolve("corrupt");
        byte[] corrupt = schemaV1Envelope();
        corrupt[corrupt.length - 1] ^= 1;
        Files.createDirectories(checkpointRoot);
        Files.write(artifact(checkpointRoot), corrupt);

        Captured captured = execute(checkpointRoot);

        assertEquals(70, captured.exitCode());
        assertEquals("", captured.stdout());
        assertTrue(captured.stderr().contains("status=ERROR"));
        assertArrayEquals(
                corrupt, Files.readAllBytes(artifact(checkpointRoot)));
    }

    private static Captured execute(Path checkpointRoot) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(new String[] {
                "scheduler-migrate-cycle-checkpoint",
                "--cycle-checkpoint-root", checkpointRoot.toString()
        }, new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Captured(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private static String value(String output, String name) {
        String prefix = name + "=";
        return output.lines()
                .filter(line -> line.startsWith(prefix))
                .findFirst()
                .orElseThrow()
                .substring(prefix.length());
    }

    private static byte[] schemaV1Envelope() throws Exception {
        ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
        try (DataOutputStream output =
                new DataOutputStream(payloadBytes)) {
            output.writeInt(1);
            writeString(output, "pending-finalization");
            writeString(output, GOAL_ID);
            writeString(output, AGENT_RUN_ID);
            output.writeBoolean(true);
            writeString(output, REFERENCE);
        }
        byte[] payload = payloadBytes.toByteArray();
        long storedAtMillis = 2000L;
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                ByteBuffer.allocate(
                                Integer.BYTES
                                        + Long.BYTES
                                        + Integer.BYTES
                                        + payload.length)
                        .putInt(ENVELOPE_MAGIC)
                        .putLong(storedAtMillis)
                        .putInt(payload.length)
                        .put(payload)
                        .array());
        return ByteBuffer.allocate(HEADER_BYTES + payload.length)
                .putInt(ENVELOPE_MAGIC)
                .putLong(storedAtMillis)
                .putInt(payload.length)
                .put(digest)
                .put(payload)
                .array();
    }

    private static void writeString(
            DataOutputStream output,
            String value) throws Exception {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private static Path artifact(Path root) {
        return root.resolve("pending.finalization");
    }

    private record Captured(
            int exitCode,
            String stdout,
            String stderr) {
    }
}
