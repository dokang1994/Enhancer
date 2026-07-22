package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enhancer.bus.MessageEnvelope;
import com.enhancer.bus.WorkPayload;
import com.enhancer.workspace.ApprovedTaskRevision;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemSubmissionManifestStoreTest {
    private static final String QUEUE_ID =
            "00000000-0000-0000-0000-000000000b01";
    private static final String MESSAGE_ID =
            "00000000-0000-0000-0000-000000000b02";
    private static final String CAUSATION_ID =
            "00000000-0000-0000-0000-000000000b03";

    @TempDir
    Path temporaryRoot;

    @Test
    void exactReplayDoesNotRewriteAndChangedIdentityReuseFails() throws Exception {
        FileSystemSubmissionManifestStore store =
                new FileSystemSubmissionManifestStore(temporaryRoot);
        DurableSubmissionManifest manifest = manifest("manifest-store-test");
        assertTrue(store.storeIdempotently(manifest));
        Path artifact = store.artifactPath(MESSAGE_ID);
        byte[] first = Files.readAllBytes(artifact);

        assertFalse(store.storeIdempotently(manifest));
        assertArrayEquals(first, Files.readAllBytes(artifact));
        assertEquals(manifest, store.resolve(MESSAGE_ID));
        assertThrows(
                IllegalArgumentException.class,
                () -> store.storeIdempotently(manifest("changed-producer")));
    }

    @Test
    void corruptedArtifactFailsExplicitly() throws Exception {
        FileSystemSubmissionManifestStore store =
                new FileSystemSubmissionManifestStore(temporaryRoot);
        store.storeIdempotently(manifest("manifest-store-test"));
        Path artifact = store.artifactPath(MESSAGE_ID);
        byte[] corrupted = Files.readAllBytes(artifact);
        corrupted[corrupted.length - 1] ^= 1;
        Files.write(artifact, corrupted);

        IOException failure = assertThrows(
                IOException.class,
                () -> store.resolve(MESSAGE_ID));
        assertTrue(failure.getMessage().contains("corrupted submission manifest"));
    }

    private DurableSubmissionManifest manifest(String producer) {
        return new DurableSubmissionManifest(
                QUEUE_ID,
                4,
                "read-file-worker",
                new MessageEnvelope(
                        MESSAGE_ID,
                        "manifest-store-correlation",
                        Optional.of(CAUSATION_ID),
                        "manifest-store-logical-run",
                        producer,
                        Instant.parse("2026-07-22T15:30:00Z"),
                        new WorkPayload(
                                new ApprovedTaskRevision(
                                        "manifest-store-task",
                                        "CURRENT_TASK.md",
                                        "c".repeat(64)),
                                "d".repeat(64),
                                Set.of("read-file"),
                                Optional.of(new WorkPayload.ExecutionInput(
                                        "CURRENT_TASK.md",
                                        "e".repeat(64))))));
    }
}
