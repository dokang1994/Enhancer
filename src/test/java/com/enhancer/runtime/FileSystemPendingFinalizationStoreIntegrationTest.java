package com.enhancer.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemPendingFinalizationStoreIntegrationTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-000000000801";
    private static final String AGENT_RUN_ID =
            "00000000-0000-0000-0000-000000000802";
    private static final String REFERENCE =
            "run-record/00000000-0000-0000-0000-000000000803";

    @TempDir
    Path tempDir;

    @Test
    void findPendingIsEmptyOnAFreshStore() throws Exception {
        assertTrue(store().findPending().isEmpty());
    }

    @Test
    void recordThenFindPendingRoundTripsWithoutReference() throws Exception {
        FileSystemPendingFinalizationStore store = store();
        PendingFinalization pending =
                new PendingFinalization(GOAL_ID, AGENT_RUN_ID, Optional.empty());
        store.record(pending);

        assertEquals(Optional.of(pending), store().findPending());
    }

    @Test
    void recordOverwritesTheSingleIntentWithReference() throws Exception {
        FileSystemPendingFinalizationStore store = store();
        store.record(new PendingFinalization(GOAL_ID, AGENT_RUN_ID, Optional.empty()));
        PendingFinalization updated =
                new PendingFinalization(GOAL_ID, AGENT_RUN_ID, Optional.of(REFERENCE));
        store.record(updated);

        assertEquals(Optional.of(updated), store().findPending());
    }

    @Test
    void clearRemovesTheIntentAndIsIdempotent() throws Exception {
        FileSystemPendingFinalizationStore store = store();
        store.record(new PendingFinalization(GOAL_ID, AGENT_RUN_ID, Optional.of(REFERENCE)));

        store.clear();
        store.clear();

        assertTrue(store().findPending().isEmpty());
    }

    @Test
    void clearOnAFreshStoreIsANoOp() throws Exception {
        store().clear();
        assertTrue(store().findPending().isEmpty());
    }

    @Test
    void corruptedArtifactFailsClosed() throws Exception {
        store().record(new PendingFinalization(GOAL_ID, AGENT_RUN_ID, Optional.of(REFERENCE)));
        Path artifact = soleArtifact();
        byte[] bytes = Files.readAllBytes(artifact);
        bytes[bytes.length - 1] ^= 0x01;
        Files.write(artifact, bytes);

        assertThrows(CorruptedPendingFinalizationException.class, () ->
                store().findPending());
    }

    @Test
    void truncatedArtifactFailsClosed() throws Exception {
        store().record(new PendingFinalization(GOAL_ID, AGENT_RUN_ID, Optional.of(REFERENCE)));
        Path artifact = soleArtifact();
        byte[] bytes = Files.readAllBytes(artifact);
        Files.write(artifact, Arrays.copyOf(bytes, bytes.length - 1));

        assertThrows(CorruptedPendingFinalizationException.class, () ->
                store().findPending());
    }

    @Test
    void oversizedArtifactFailsClosed() throws Exception {
        store().record(new PendingFinalization(GOAL_ID, AGENT_RUN_ID, Optional.empty()));
        Path artifact = soleArtifact();
        Files.write(artifact, new byte[
                FileSystemPendingFinalizationStore.HEADER_BYTES
                        + FileSystemPendingFinalizationStore.MAX_STATE_BYTES + 1]);

        assertThrows(CorruptedPendingFinalizationException.class, () ->
                store().findPending());
    }

    private FileSystemPendingFinalizationStore store() {
        return new FileSystemPendingFinalizationStore(tempDir.resolve("checkpoint"));
    }

    private Path soleArtifact() throws IOException {
        try (Stream<Path> files = Files.list(tempDir.resolve("checkpoint"))) {
            return files.findFirst().orElseThrow();
        }
    }
}
