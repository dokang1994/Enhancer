package com.enhancer.tool;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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

class ReadFileToolIntegrationTest {
    @TempDir
    Path tempDirectory;

    @Test
    void readsARealUtf8FileThroughTheGovernedExecutionBoundary() throws IOException {
        Path projectRoot = Files.createDirectory(tempDirectory.resolve("project"));
        Path file = projectRoot.resolve("notes.txt");
        byte[] original = "Enhancer execution evidence".getBytes(StandardCharsets.UTF_8);
        Files.write(file, original);

        ToolResult result = execute(projectRoot, "notes.txt", 1024);

        assertEquals(ToolResultStatus.SUCCESS, result.status());
        assertEquals("Enhancer execution evidence", result.evidence().outputTail());
        assertFalse(result.evidence().truncated());
        assertTrue(result.exitCode().isEmpty());
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    void rejectsMalformedMissingAbsoluteAndEscapingPaths() throws IOException {
        Path projectRoot = Files.createDirectory(tempDirectory.resolve("project"));
        Path outside = tempDirectory.resolve("outside.txt");
        Files.writeString(outside, "outside", StandardCharsets.UTF_8);

        ToolResult missingArgument = execute(projectRoot, null, 1024);
        ToolResult blank = execute(projectRoot, " ", 1024);
        ToolResult absolute = execute(projectRoot, outside.toAbsolutePath().toString(), 1024);
        ToolResult traversal = execute(projectRoot, "../outside.txt", 1024);
        ToolResult missingFile = execute(projectRoot, "missing.txt", 1024);

        assertFailure(missingArgument, "path");
        assertFailure(blank, "path");
        assertFailure(absolute, "relative");
        assertFailure(traversal, "outside");
        assertFailure(missingFile, "not found");
    }

    @Test
    void rejectsDirectoriesOversizedFilesAndInvalidUtf8() throws IOException {
        Path projectRoot = Files.createDirectory(tempDirectory.resolve("project"));
        Files.createDirectory(projectRoot.resolve("directory"));
        Files.writeString(projectRoot.resolve("large.txt"), "123456789", StandardCharsets.UTF_8);
        Files.write(projectRoot.resolve("invalid.txt"), new byte[] {(byte) 0xC3, 0x28});

        ToolResult directory = execute(projectRoot, "directory", 8);
        ToolResult oversized = execute(projectRoot, "large.txt", 8);
        ToolResult invalidUtf8 = execute(projectRoot, "invalid.txt", 8);

        assertFailure(directory, "regular file");
        assertFailure(oversized, "size");
        assertFailure(invalidUtf8, "UTF-8");
    }

    @Test
    void reportsMissingEvidencePersistenceAsExecutionFailure() throws IOException {
        Path projectRoot = Files.createDirectory(tempDirectory.resolve("project"));
        Files.writeString(
                projectRoot.resolve("large-output.txt"),
                "x".repeat(VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS + 1),
                StandardCharsets.UTF_8);

        ToolResult result = execute(
                projectRoot,
                "large-output.txt",
                VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS + 1);

        assertEquals(ToolResultStatus.FAILURE, result.status());
        assertEquals(ToolFailureCode.EXECUTION_FAILED, result.failureCode().orElseThrow());
        assertTrue(result.evidence().outputTail().contains("persistence"));
    }

    @Test
    void rejectsASymbolicLinkThatEscapesTheRealProjectRoot() throws IOException {
        Path projectRoot = Files.createDirectory(tempDirectory.resolve("project"));
        Path outside = tempDirectory.resolve("outside.txt");
        Files.writeString(outside, "outside", StandardCharsets.UTF_8);
        Path link = projectRoot.resolve("outside-link.txt");

        try {
            Files.createSymbolicLink(link, outside);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(
                    false,
                    () -> "Symbolic-link creation is unavailable on this host: "
                            + exception.getClass().getSimpleName());
            return;
        }

        ToolResult result = execute(projectRoot, "outside-link.txt", 1024);

        assertFailure(result, "outside");
    }

    private ToolResult execute(Path projectRoot, String path, long maxReadBytes) {
        Map<String, String> arguments = path == null ? Map.of() : Map.of("path", path);
        ToolRequest request = new ToolRequest(
                ReadFileTool.NAME,
                "run-1",
                arguments);
        ExecutionPolicy policy = new ExecutionPolicy(
                projectRoot,
                Set.of(ReadFileTool.NAME),
                Set.of(),
                maxReadBytes,
                Duration.ofSeconds(1),
                CancellationToken.none());

        try (ToolExecutor executor = new ToolExecutor(List.of(new ReadFileTool()))) {
            return executor.execute(request, policy);
        }
    }

    private void assertFailure(ToolResult result, String expectedText) {
        assertEquals(ToolResultStatus.FAILURE, result.status());
        assertTrue(
                (result.evidence().summary() + " " + result.evidence().outputTail())
                        .contains(expectedText));
    }
}
