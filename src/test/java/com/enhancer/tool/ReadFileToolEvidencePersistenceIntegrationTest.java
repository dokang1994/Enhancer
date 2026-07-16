package com.enhancer.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReadFileToolEvidencePersistenceIntegrationTest {
    @TempDir
    Path tempDirectory;

    @Test
    void readsALargeFileAndReturnsResolvableCompleteEvidence() throws IOException {
        Path projectRoot = Files.createDirectory(tempDirectory.resolve("project"));
        String content = "prefix-" + "x".repeat(VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS);
        Files.writeString(projectRoot.resolve("large.txt"), content, StandardCharsets.UTF_8);

        FileSystemEvidenceStore store = new FileSystemEvidenceStore(
                tempDirectory.resolve("evidence"),
                new EvidenceStoragePolicy(16 * 1024));
        String runId = store.createRun();
        ToolRequest request = new ToolRequest(
                ReadFileTool.NAME,
                runId,
                Map.of(ReadFileTool.PATH_ARGUMENT, "large.txt"));
        ExecutionPolicy policy = new ExecutionPolicy(
                projectRoot,
                Set.of(ReadFileTool.NAME),
                Set.of(),
                8 * 1024,
                Duration.ofSeconds(1),
                CancellationToken.none());

        ToolResult result;
        try (ToolExecutor executor = new ToolExecutor(List.of(
                new ReadFileTool(new EvidenceRecorder(store))))) {
            result = executor.execute(request, policy);
        }

        assertEquals(ToolResultStatus.SUCCESS, result.status());
        assertTrue(result.evidence().truncated());
        assertEquals(
                VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS,
                result.evidence().outputTail().length());
        ResolvedEvidence resolved = store.resolve(
                result.evidence().fullOutputReference().orElseThrow());
        assertEquals(content, resolved.content());
        assertEquals(content.getBytes(StandardCharsets.UTF_8).length, resolved.metadata().contentLength());
    }
}
