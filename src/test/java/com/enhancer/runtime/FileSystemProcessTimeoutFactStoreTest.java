package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemProcessTimeoutFactStoreTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000008101";
    private static final String WORK_ITEM_ID =
            "00000000-0000-0000-0000-000000008102";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000008103";

    @TempDir
    Path temporaryRoot;

    @Test
    void pointPersistsResolvesAndExactReplaysWithoutRewrite() throws Exception {
        Path root = temporaryRoot.resolve("process-timeouts");
        FileSystemProcessTimeoutFactStore store =
                new FileSystemProcessTimeoutFactStore(root);
        ProcessTimeoutFact fact = fact("watchdog destroyed the child");

        ResolvedProcessTimeoutFact persisted = store.persist(fact);
        Path artifact = root.resolve(GOAL_ID).resolve(
                AGENT_RUN_ID + ".process-timeout");
        byte[] firstBytes = Files.readAllBytes(artifact);

        assertEquals(fact, persisted.fact());
        assertEquals(fact.reference(), persisted.reference());
        assertEquals(64, persisted.sha256().length());
        assertEquals(Optional.of(persisted), store.find(GOAL_ID, AGENT_RUN_ID));
        assertEquals(persisted, store.resolve(fact.reference()));

        assertEquals(persisted, store.persist(fact));
        assertArrayEquals(firstBytes, Files.readAllBytes(artifact));
    }

    @Test
    void rejectsChangedReplayForeignReferenceAndInvalidArtifacts() throws Exception {
        Path root = temporaryRoot.resolve("invalid-process-timeouts");
        FileSystemProcessTimeoutFactStore store =
                new FileSystemProcessTimeoutFactStore(root);
        ProcessTimeoutFact original = fact("watchdog destroyed the child");
        store.persist(original);
        Path artifact = root.resolve(GOAL_ID).resolve(
                AGENT_RUN_ID + ".process-timeout");
        byte[] firstBytes = Files.readAllBytes(artifact);

        ProcessTimeoutFact changed = new ProcessTimeoutFact(
                original.schemaVersion(),
                original.occurredAt().plusSeconds(1),
                original.binding(),
                original.agentRunId(),
                original.timeout(),
                original.reason());
        assertThrows(IOException.class, () -> store.persist(changed));
        assertArrayEquals(firstBytes, Files.readAllBytes(artifact));
        assertThrows(IOException.class, () -> store.resolve(
                "process-timeout/" + GOAL_ID
                        + "/00000000-0000-0000-0000-000000008199"));

        byte[] corrupt = firstBytes.clone();
        corrupt[corrupt.length - 1] ^= 1;
        Files.write(artifact, corrupt);
        assertThrows(IOException.class, () -> store.resolve(original.reference()));

        byte[] unsupported = firstBytes.clone();
        ByteBuffer.wrap(unsupported).putInt(
                FileSystemProcessTimeoutFactStore.HEADER_BYTES,
                2);
        replaceDigest(unsupported);
        Files.write(artifact, unsupported);
        IOException versionFailure = assertThrows(
                IOException.class, () -> store.resolve(original.reference()));
        assertTrue(versionFailure.getMessage().contains("version"));

        Files.write(
                artifact,
                ByteBuffer.allocate(firstBytes.length + 1)
                        .put(firstBytes)
                        .put((byte) 1)
                        .array());
        assertThrows(IOException.class, () -> store.resolve(original.reference()));

        try (FileChannel channel = FileChannel.open(
                artifact,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            channel.position(
                    FileSystemProcessTimeoutFactStore.HEADER_BYTES
                            + FileSystemProcessTimeoutFactStore.MAX_STATE_BYTES);
            channel.write(ByteBuffer.wrap(new byte[] {1}));
        }
        assertThrows(IOException.class, () -> store.resolve(original.reference()));
    }

    @Test
    void symbolicStorageRootIsRejectedWhenLinksAreAvailable() throws Exception {
        Path target = temporaryRoot.resolve("timeout-target");
        Path link = temporaryRoot.resolve("timeout-link");
        Files.createDirectory(target);
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException exception) {
            Assumptions.assumeTrue(
                    false,
                    "symbolic links are unavailable: " + exception.getMessage());
        }

        FileSystemProcessTimeoutFactStore store =
                new FileSystemProcessTimeoutFactStore(link);
        IOException failure = assertThrows(IOException.class, () ->
                store.persist(fact("watchdog destroyed the child")));
        assertTrue(failure.getMessage().contains("symbolic"), failure.getMessage());
    }

    private static ProcessTimeoutFact fact(String reason) {
        return ProcessTimeoutFact.create(
                Instant.parse("2026-08-04T10:00:00Z"),
                new RuntimeEventBinding(
                        GOAL_ID,
                        WORK_ITEM_ID,
                        new ApprovedTaskRevision(
                                "persist-process-timeout",
                                "CURRENT_TASK.md",
                                "a".repeat(64)),
                        "b".repeat(64),
                        "logical-run-process-timeout",
                        "correlation-process-timeout"),
                AGENT_RUN_ID,
                Duration.ofSeconds(30),
                reason);
    }

    private static void replaceDigest(byte[] envelope) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        int payloadLength = buffer.getInt(Integer.BYTES);
        byte[] payload = new byte[payloadLength];
        System.arraycopy(
                envelope,
                FileSystemProcessTimeoutFactStore.HEADER_BYTES,
                payload,
                0,
                payloadLength);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload);
        System.arraycopy(
                digest,
                0,
                envelope,
                Integer.BYTES + Integer.BYTES,
                digest.length);
    }
}
