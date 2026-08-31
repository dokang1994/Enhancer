package com.enhancer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.enhancer.tool.CancellationToken;
import com.enhancer.tool.ExecutionPolicy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GovernedModelPromptReaderTest {
    @TempDir
    Path temporaryRoot;

    private final GovernedModelPromptReader reader = new GovernedModelPromptReader();

    @Test
    void readsOneExactContainedStrictUtf8PromptAtTheByteCeiling() throws IOException {
        Path projectRoot = Files.createDirectory(temporaryRoot.resolve("project"));
        String prompt = "한글 prompt";
        byte[] bytes = prompt.getBytes(StandardCharsets.UTF_8);
        Files.write(projectRoot.resolve("prompt.txt"), bytes);

        assertEquals(prompt, reader.readFile("prompt.txt", policy(projectRoot, bytes.length)));
    }

    @Test
    void rejectsAbsoluteEscapingMissingNonRegularOversizedAndMalformedInput()
            throws IOException {
        Path projectRoot = Files.createDirectory(temporaryRoot.resolve("project"));
        Files.createDirectory(projectRoot.resolve("directory"));
        Files.writeString(projectRoot.resolve("oversized.txt"), "12345", StandardCharsets.UTF_8);
        Files.write(projectRoot.resolve("malformed.txt"), new byte[] {(byte) 0xC3, 0x28});
        ExecutionPolicy policy = policy(projectRoot, 4);

        assertThrows(
                IllegalArgumentException.class,
                () -> reader.readFile(projectRoot.resolve("oversized.txt").toString(), policy));
        assertThrows(SecurityException.class, () -> reader.readFile("../outside.txt", policy));
        assertTrue(assertThrows(
                IOException.class,
                () -> reader.readFile("missing.txt", policy)).getMessage().contains("not found"));
        assertTrue(assertThrows(
                IOException.class,
                () -> reader.readFile("directory", policy)).getMessage().contains("regular"));
        assertTrue(assertThrows(
                IOException.class,
                () -> reader.readFile("oversized.txt", policy)).getMessage().contains("limit"));
        assertTrue(assertThrows(
                IOException.class,
                () -> reader.readFile("malformed.txt", policy)).getMessage().contains("UTF-8"));
    }

    @Test
    void rejectsASymbolicLinkThatResolvesOutsideTheRealProjectRoot() throws IOException {
        Path projectRoot = Files.createDirectory(temporaryRoot.resolve("project"));
        Path outside = temporaryRoot.resolve("outside.txt");
        Files.writeString(outside, "outside", StandardCharsets.UTF_8);
        Path link = projectRoot.resolve("prompt.txt");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, () -> "Symbolic-link creation unavailable: "
                    + exception.getClass().getSimpleName());
        }

        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> reader.readFile("prompt.txt", policy(projectRoot, 64)));

        assertTrue(exception.getMessage().contains("real project root"));
    }

    @Test
    void preservesTheExistingInternalSymbolicLinkBehavior() throws IOException {
        Path projectRoot = Files.createDirectory(temporaryRoot.resolve("project"));
        Path target = projectRoot.resolve("target.txt");
        Files.writeString(target, "inside", StandardCharsets.UTF_8);
        Path link = projectRoot.resolve("prompt.txt");
        try {
            Files.createSymbolicLink(link, target.getFileName());
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            assumeTrue(false, () -> "Symbolic-link creation unavailable: "
                    + exception.getClass().getSimpleName());
        }

        assertEquals("inside", reader.readFile("prompt.txt", policy(projectRoot, 64)));
    }

    private ExecutionPolicy policy(Path projectRoot, long maximumReadBytes) {
        return new ExecutionPolicy(
                projectRoot,
                Set.of(ModelInvokeTool.NAME),
                Set.of(),
                maximumReadBytes,
                Duration.ofSeconds(5),
                CancellationToken.none());
    }
}
