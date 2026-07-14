package com.enhancer.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EvidenceRecorderTest {
    @TempDir
    Path tempDirectory;

    @Test
    void leavesShortOutputUnpersisted() throws IOException {
        Path storageRoot = tempDirectory.resolve("evidence");
        FileSystemEvidenceStore store = new FileSystemEvidenceStore(
                storageRoot,
                new EvidenceRetentionPolicy(8192, Duration.ofDays(30)));
        String runId = store.createRun();
        EvidenceRecorder recorder = new EvidenceRecorder(store);

        VerificationEvidence evidence = recorder.capture(runId, "short result", "complete");

        assertFalse(evidence.truncated());
        assertTrue(evidence.fullOutputReference().isEmpty());
        try (var paths = Files.list(storageRoot.resolve(runId))) {
            assertEquals(0, paths.count());
        }
    }

    @Test
    void persistsTruncatedOutputAndReturnsAResolvableReference() throws IOException {
        FileSystemEvidenceStore store = new FileSystemEvidenceStore(
                tempDirectory.resolve("evidence"),
                new EvidenceRetentionPolicy(8192, Duration.ofDays(30)));
        String runId = store.createRun();
        String output = "prefix" + "x".repeat(VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS);

        VerificationEvidence evidence = new EvidenceRecorder(store)
                .capture(runId, "large result", output);
        ResolvedEvidence resolved = store.resolve(evidence.fullOutputReference().orElseThrow());

        assertTrue(evidence.truncated());
        assertEquals(VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS, evidence.outputTail().length());
        assertEquals(output, resolved.content());
    }
}
