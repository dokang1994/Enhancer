package com.enhancer.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DevelopmentSessionCheckpointManagerIntegrationTest {
    private static final String RUN_ID =
            "00000000-0000-0000-0000-000000000901";

    @TempDir
    Path temporaryRoot;

    @Test
    void forcedStopRecoversTheLastSuccessfulStepAndPendingActionFromAFreshManager()
            throws Exception {
        writeTask("In Progress", "Pending", "Run regression tests.");
        writeArtifact("src/example.txt", "before");
        DevelopmentSessionCheckpointManager first = manager();

        DevelopmentSessionCheckpoint started = first.start(
                "edit checkpoint code",
                "record the edit outcome",
                List.of("src/example.txt"));
        writeArtifact("src/example.txt", "after");
        DevelopmentSessionCheckpoint succeeded = first.record(
                RUN_ID,
                started.revision(),
                DevelopmentSessionCheckpointState.STEP_SUCCEEDED,
                "edit checkpoint code",
                "run focused tests",
                List.of("test-log/red-green"),
                List.of("src/example.txt"));
        DevelopmentSessionCheckpoint pending = first.record(
                RUN_ID,
                succeeded.revision(),
                DevelopmentSessionCheckpointState.STEP_PENDING,
                "run focused tests",
                "record focused test evidence",
                List.of("test-log/red-green"),
                List.of("src/example.txt"));

        DevelopmentSessionCheckpointInspection recovered = manager().inspect()
                .orElseThrow();

        assertEquals(pending, recovered.checkpoint());
        assertEquals("edit checkpoint code",
                recovered.checkpoint().lastSuccessfulStep().orElseThrow());
        assertEquals("run focused tests", recovered.checkpoint().currentStep());
        assertEquals("record focused test evidence", recovered.checkpoint().nextAction());
        assertTrue(recovered.taskContractMatches());
        assertTrue(recovered.artifactMismatches().isEmpty());
    }

    @Test
    void lifecycleDocumentEditsDoNotChangeContractButScopeEditsFailClosed()
            throws Exception {
        writeTask("In Progress", "Pending", "Run regression tests.");
        DevelopmentSessionCheckpoint started = manager().start(
                "implement",
                "record outcome",
                List.of());

        writeTask("Implemented", "Focused tests passed.", "Run regression tests.");
        DevelopmentSessionCheckpoint updated = manager().record(
                RUN_ID,
                started.revision(),
                DevelopmentSessionCheckpointState.STEP_SUCCEEDED,
                "implement",
                "synchronize documents",
                List.of("focused-tests"),
                List.of());
        assertEquals(started.taskContractSha256(), updated.taskContractSha256());

        writeTask("Implemented", "Focused tests passed.", "Changed scope.");
        assertThrows(DevelopmentSessionCheckpointConflictException.class, () ->
                manager().record(
                        RUN_ID,
                        updated.revision(),
                        DevelopmentSessionCheckpointState.STEP_PENDING,
                        "synchronize documents",
                        "review diff",
                        List.of(),
                        List.of()));
        DevelopmentSessionCheckpointInspection inspection = manager().inspect()
                .orElseThrow();
        assertFalse(inspection.taskContractMatches());
        assertEquals(updated, inspection.checkpoint());
    }

    @Test
    void staleRevisionAndDifferentRunCannotOverwriteTheCheckpoint()
            throws Exception {
        writeTask("In Progress", "Pending", "Run regression tests.");
        DevelopmentSessionCheckpoint started = manager().start(
                "implement",
                "record outcome",
                List.of());

        assertThrows(DevelopmentSessionCheckpointConflictException.class, () ->
                manager().record(
                        "00000000-0000-0000-0000-000000000999",
                        started.revision(),
                        DevelopmentSessionCheckpointState.STEP_SUCCEEDED,
                        "implement",
                        "test",
                        List.of(),
                        List.of()));
        assertThrows(DevelopmentSessionCheckpointConflictException.class, () ->
                manager().record(
                        RUN_ID,
                        started.revision() - 1,
                        DevelopmentSessionCheckpointState.STEP_SUCCEEDED,
                        "implement",
                        "test",
                        List.of(),
                        List.of()));
        assertEquals(started, manager().inspect().orElseThrow().checkpoint());
    }

    @Test
    void clearRequiresStableStateAndAnUnchangedArtifactManifest()
            throws Exception {
        writeTask("In Progress", "Pending", "Run regression tests.");
        writeArtifact("src/example.txt", "verified");
        DevelopmentSessionCheckpoint started = manager().start(
                "verify",
                "mark stable",
                List.of("src/example.txt"));

        assertThrows(DevelopmentSessionCheckpointConflictException.class, () ->
                manager().clear(RUN_ID, started.revision()));
        DevelopmentSessionCheckpoint stable = manager().record(
                RUN_ID,
                started.revision(),
                DevelopmentSessionCheckpointState.STABLE,
                "session close synchronized",
                "clear checkpoint",
                List.of("full-build"),
                List.of("src/example.txt"));

        writeArtifact("src/example.txt", "changed-after-stable");
        assertThrows(DevelopmentSessionCheckpointConflictException.class, () ->
                manager().clear(RUN_ID, stable.revision()));
        assertTrue(manager().inspect().isPresent());

        writeArtifact("src/example.txt", "verified");
        manager().clear(RUN_ID, stable.revision());
        assertTrue(manager().inspect().isEmpty());
    }

    @Test
    void startNeverOverwritesAnExistingCheckpointAndCorruptionFailsClosed()
            throws Exception {
        writeTask("In Progress", "Pending", "Run regression tests.");
        manager().start("implement", "record", List.of());

        assertThrows(DevelopmentSessionCheckpointConflictException.class, () ->
                manager().start("another", "record", List.of()));

        Path artifact = temporaryRoot.resolve(
                ".enhancer/session-checkpoint/session.checkpoint");
        byte[] bytes = Files.readAllBytes(artifact);
        bytes[bytes.length - 1] ^= 0x01;
        Files.write(artifact, bytes);
        assertThrows(CorruptedDevelopmentSessionCheckpointException.class, () ->
                manager().inspect());
    }

    @Test
    void symbolicLinkRuntimeBoundaryFailsClosedWithoutReadingOutsideTheProject()
            throws Exception {
        writeTask("In Progress", "Pending", "Run regression tests.");
        Path outside = Files.createDirectory(temporaryRoot.resolve("outside"));
        Path runtimeBoundary = temporaryRoot.resolve(".enhancer");
        try {
            Files.createSymbolicLink(runtimeBoundary, outside);
        } catch (UnsupportedOperationException | java.io.IOException
                | SecurityException exception) {
            assumeTrue(false, "symbolic-link creation is unavailable: " + exception);
        }

        assertThrows(CorruptedDevelopmentSessionCheckpointException.class, () ->
                manager().inspect());
        try (java.util.stream.Stream<Path> paths = Files.list(outside)) {
            assertTrue(paths.findAny().isEmpty());
        }
    }

    @Test
    void windowsJunctionRuntimeBoundaryFailsClosedWithoutWritingOutsideTheProject()
            throws Exception {
        assumeTrue(isWindows(), "directory junction regression is Windows-specific");
        writeTask("In Progress", "Pending", "Run regression tests.");
        Path outside = Files.createDirectory(temporaryRoot.resolve("junction-outside"));
        createJunction(temporaryRoot.resolve(".enhancer"), outside);

        assertThrows(CorruptedDevelopmentSessionCheckpointException.class, () ->
                manager().inspect());
        try (java.util.stream.Stream<Path> paths = Files.list(outside)) {
            assertTrue(paths.findAny().isEmpty());
        }
    }

    private static void createJunction(Path junction, Path target) throws Exception {
        String commandInterpreter = System.getenv().getOrDefault(
                "ComSpec", "C:\\Windows\\System32\\cmd.exe");
        Process process = new ProcessBuilder(
                commandInterpreter,
                "/d",
                "/c",
                "mklink",
                "/J",
                junction.toString(),
                target.toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        assertTrue(process.waitFor(10, TimeUnit.SECONDS),
                "junction creation timed out");
        assertEquals(0, process.exitValue(), "junction creation failed: " + output);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    private DevelopmentSessionCheckpointManager manager() throws java.io.IOException {
        return new DevelopmentSessionCheckpointManager(temporaryRoot, () -> RUN_ID);
    }

    private void writeArtifact(String relativePath, String content) throws Exception {
        Path path = temporaryRoot.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private void writeTask(String status, String verification, String criterion)
            throws Exception {
        Files.writeString(
                temporaryRoot.resolve("CURRENT_TASK.md"),
                """
                # Current Task

                ## Status

                %s

                ## Task

                Persist one session checkpoint.

                ## Task ID

                persist-development-session-checkpoints

                ## Justified By

                - Accepted checkpoint decision

                ## Acceptance Criteria

                - %s

                ## Out Of Scope

                - Automatic commits.

                ## Approval

                Approved by the user.

                ## Verification

                %s

                ## Next

                Continue roadmap work.
                """.formatted(status, criterion, verification),
                StandardCharsets.UTF_8);
    }
}
