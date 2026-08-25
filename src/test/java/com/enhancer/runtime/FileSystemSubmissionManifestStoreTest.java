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
    void persistsExactPriorityAndRejectsChangedPriorityReplay() throws Exception {
        FileSystemSubmissionManifestStore store =
                new FileSystemSubmissionManifestStore(temporaryRoot);
        DurableSubmissionManifest expedited = manifest(
                "manifest-store-test", SchedulerPriority.EXPEDITED);

        assertTrue(store.storeIdempotently(expedited));
        assertEquals(
                SchedulerPriority.EXPEDITED,
                store.resolve(MESSAGE_ID).priority());
        assertThrows(
                IllegalArgumentException.class,
                () -> store.storeIdempotently(manifest(
                        "manifest-store-test", SchedulerPriority.NORMAL)));
    }

    @Test
    void currentSchemaRetainsExactTypedModelWorkAndIndependentCapability()
            throws Exception {
        FileSystemSubmissionManifestStore store =
                new FileSystemSubmissionManifestStore(temporaryRoot);
        DurableSubmissionManifest manifest = new DurableSubmissionManifest(
                QUEUE_ID,
                4,
                ModelWorkFixtures.INDEPENDENT_CAPABILITY,
                ModelWorkFixtures.envelope(),
                SchedulerPriority.EXPEDITED);

        assertTrue(store.storeIdempotently(manifest));
        DurableSubmissionManifest restored = store.resolve(ModelWorkFixtures.MESSAGE_ID);

        assertEquals(3, FileSystemSubmissionManifestStore.CURRENT_SCHEMA_VERSION);
        assertEquals(manifest, restored);
        assertEquals(ModelWorkFixtures.envelope(), restored.workMessage());
        assertEquals(
                ModelWorkFixtures.INDEPENDENT_CAPABILITY,
                restored.requiredCapability());
        assertEquals(
                ModelWorkFixtures.profile(),
                ((com.enhancer.bus.ModelWorkPayload) restored.workMessage().payload())
                        .executionInput()
                        .executionProfile());
    }

    @Test
    void changedModelProfileUnderOneSubmissionIdentityFailsWithoutRewrite()
            throws Exception {
        FileSystemSubmissionManifestStore store =
                new FileSystemSubmissionManifestStore(temporaryRoot);
        DurableSubmissionManifest original = new DurableSubmissionManifest(
                QUEUE_ID,
                4,
                ModelWorkFixtures.INDEPENDENT_CAPABILITY,
                ModelWorkFixtures.envelope());
        assertTrue(store.storeIdempotently(original));
        Path artifact = store.artifactPath(ModelWorkFixtures.MESSAGE_ID);
        byte[] before = Files.readAllBytes(artifact);
        DurableSubmissionManifest changedProfile = new DurableSubmissionManifest(
                QUEUE_ID,
                4,
                ModelWorkFixtures.INDEPENDENT_CAPABILITY,
                ModelWorkFixtures.envelope(
                        ModelWorkFixtures.profile("changed-profile-capability")));

        assertThrows(
                IllegalArgumentException.class,
                () -> store.storeIdempotently(changedProfile));
        assertArrayEquals(before, Files.readAllBytes(artifact));
        assertEquals(original, store.resolve(ModelWorkFixtures.MESSAGE_ID));
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
        return manifest(producer, SchedulerPriority.NORMAL);
    }

    private DurableSubmissionManifest manifest(
            String producer,
            SchedulerPriority priority) {
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
                                        "e".repeat(64))))),
                priority);
    }
}
