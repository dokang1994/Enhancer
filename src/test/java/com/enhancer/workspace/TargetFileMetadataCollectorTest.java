package com.enhancer.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TargetFileMetadataCollectorTest {
    private static final Instant OBSERVED_AT = Instant.parse("2026-07-15T12:00:00Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void observesAReadableTargetWithItsStreamedDigest() throws Exception {
        Path projectRoot = Files.createDirectories(temporaryRoot.resolve("project"));
        String content = "target-content\n";
        Files.createDirectories(projectRoot.resolve("docs"));
        Files.writeString(projectRoot.resolve("docs/target.txt"), content, StandardCharsets.UTF_8);

        WorkspaceSourceObservation observation = new TargetFileMetadataCollector().observe(
                projectRoot,
                "docs/target.txt",
                OBSERVED_AT);

        assertEquals(WorkspaceSourceKind.REPOSITORY_FILE, observation.kind());
        assertEquals("docs/target.txt", observation.sourceId());
        assertEquals("target-file-reader", observation.provenance());
        assertEquals(OBSERVED_AT, observation.observedAt());
        assertEquals(WorkspaceSourceState.AVAILABLE, observation.state());
        assertEquals(
                Optional.of(sha256(content)),
                observation.contentSha256());
    }

    @Test
    void observesAMissingTargetAsUnavailableWithAReason() throws Exception {
        Path projectRoot = Files.createDirectories(temporaryRoot.resolve("missing-project"));

        WorkspaceSourceObservation observation = new TargetFileMetadataCollector().observe(
                projectRoot,
                "absent.txt",
                OBSERVED_AT);

        assertEquals(WorkspaceSourceState.UNAVAILABLE, observation.state());
        assertEquals(Optional.empty(), observation.contentSha256());
        assertTrue(observation.reason().isPresent());
    }

    @Test
    void rejectsAbsoluteTraversalAndNonRegularTargets() throws Exception {
        Path projectRoot = Files.createDirectories(temporaryRoot.resolve("contained-project"));
        Files.createDirectories(projectRoot.resolve("nested"));
        Files.writeString(temporaryRoot.resolve("outside.txt"), "outside");
        TargetFileMetadataCollector collector = new TargetFileMetadataCollector();

        assertThrows(IllegalArgumentException.class, () -> collector.observe(
                projectRoot,
                temporaryRoot.resolve("outside.txt").toAbsolutePath().toString(),
                OBSERVED_AT));
        assertThrows(IllegalArgumentException.class, () -> collector.observe(
                projectRoot,
                "../outside.txt",
                OBSERVED_AT));
        assertThrows(IllegalArgumentException.class, () -> collector.observe(
                projectRoot,
                "nested",
                OBSERVED_AT));
        assertThrows(IllegalArgumentException.class, () -> collector.observe(
                projectRoot,
                " ",
                OBSERVED_AT));
        assertThrows(NullPointerException.class, () -> collector.observe(
                null,
                "target.txt",
                OBSERVED_AT));
        assertThrows(NullPointerException.class, () -> collector.observe(
                projectRoot,
                null,
                OBSERVED_AT));
        assertThrows(NullPointerException.class, () -> collector.observe(
                projectRoot,
                "target.txt",
                null));
    }

    private static String sha256(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }
}
