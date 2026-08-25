package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.runtime.FileSystemSchedulerQueueStore;
import com.enhancer.runtime.SchedulerQueueState;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnhancerCliSchedulerQueueMigrationIntegrationTest {
    private static final int ENVELOPE_MAGIC = 0x45535131;
    private static final int HEADER_BYTES =
            Integer.BYTES + Long.BYTES + Integer.BYTES + 32;
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000001401";

    @TempDir
    Path temporaryRoot;

    @Test
    void explicitlyMigratesOneQueueAndReportsIdempotentCurrentState()
            throws Exception {
        Path root = temporaryRoot.resolve("queue");
        Files.createDirectories(root);
        Path artifact = artifact(root);
        Files.write(artifact, emptySchemaV2Envelope(1000L));

        Captured migrated = execute(new String[] {
                "scheduler-migrate-queue",
                "--queue-root", root.toString(),
                "--queue-id", QUEUE_ID
        });

        assertEquals(0, migrated.exitCode());
        assertTrue(migrated.stdout().contains("status=MIGRATED"));
        assertTrue(migrated.stdout().contains("queueId=" + QUEUE_ID));
        assertTrue(migrated.stdout().contains("sourceSchemaVersion=2"));
        assertTrue(migrated.stdout().contains(
                "targetSchemaVersion=" + SchedulerQueueState.CURRENT_SCHEMA_VERSION));
        assertEquals(SchedulerQueueState.CURRENT_SCHEMA_VERSION,
                new FileSystemSchedulerQueueStore(root)
                        .resolve(QUEUE_ID)
                        .schemaVersion());
        byte[] current = Files.readAllBytes(artifact);

        Captured replay = execute(new String[] {
                "scheduler-migrate-queue",
                "--queue-root", root.toString(),
                "--queue-id", QUEUE_ID
        });

        assertEquals(0, replay.exitCode());
        assertTrue(replay.stdout().contains("status=ALREADY_CURRENT"));
        assertArrayEquals(current, Files.readAllBytes(artifact));
    }

    @Test
    void absentQueueDoesNotCreateTheNamedRoot() {
        Path root = temporaryRoot.resolve("absent");

        Captured absent = execute(new String[] {
                "scheduler-migrate-queue",
                "--queue-root", root.toString(),
                "--queue-id", QUEUE_ID
        });

        assertEquals(0, absent.exitCode());
        assertTrue(absent.stdout().contains("status=ABSENT"));
        assertTrue(absent.stdout().contains("sourceSchemaVersion=NONE"));
        assertFalse(Files.exists(root));
    }

    private static byte[] emptySchemaV2Envelope(long storedAtMillis)
            throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(2);
            writeString(output, "scheduler-queue-state");
            writeString(output, QUEUE_ID);
            output.writeLong(0);
            output.writeInt(8);
            output.writeBoolean(false);
            output.writeInt(0);
            output.writeInt(0);
            output.writeInt(0);
            output.writeBoolean(false);
            output.writeInt(0);
            output.writeInt(0);
        }
        byte[] payload = bytes.toByteArray();
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
        return ByteBuffer.allocate(
                        HEADER_BYTES + payload.length)
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

    private Path artifact(Path root) {
        return root.resolve(QUEUE_ID + ".scheduler-queue");
    }

    private Captured execute(String[] arguments) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = new EnhancerCli().execute(
                arguments,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Captured(
                exitCode,
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private record Captured(
            int exitCode,
            String stdout,
            String stderr) {
    }
}
