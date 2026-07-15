package com.enhancer.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitWorkspaceCollectorTest {
    private static final Instant OBSERVED_AT = Instant.parse("2026-07-15T13:00:00Z");

    @TempDir
    Path temporaryRoot;

    @Test
    void observesStatusAndDiffDigestsInsideARealRepository() throws Exception {
        assumeTrue(gitAvailable(), "git is not available on this host");
        Path repository = Files.createDirectories(temporaryRoot.resolve("repository"));
        git(repository, "init");
        Files.writeString(
                repository.resolve("tracked.txt"),
                "tracked-content\n",
                StandardCharsets.UTF_8);

        List<WorkspaceSourceObservation> observations = new GitWorkspaceCollector().observe(
                repository,
                OBSERVED_AT);

        assertEquals(2, observations.size());
        WorkspaceSourceObservation status = observations.get(0);
        assertEquals(WorkspaceSourceKind.GIT_STATUS, status.kind());
        assertEquals("working-tree", status.sourceId());
        assertEquals("git-cli", status.provenance());
        assertEquals(OBSERVED_AT, status.observedAt());
        assertEquals(WorkspaceSourceState.AVAILABLE, status.state());
        assertTrue(status.contentSha256().orElseThrow().matches("[0-9a-f]{64}"));

        WorkspaceSourceObservation diff = observations.get(1);
        assertEquals(WorkspaceSourceKind.GIT_DIFF, diff.kind());
        assertEquals("working-tree-diff", diff.sourceId());
        assertEquals(WorkspaceSourceState.AVAILABLE, diff.state());
        assertTrue(diff.contentSha256().orElseThrow().matches("[0-9a-f]{64}"));

        List<WorkspaceSourceObservation> again = new GitWorkspaceCollector().observe(
                repository,
                OBSERVED_AT);
        assertEquals(
                status.contentSha256(),
                again.get(0).contentSha256(),
                "an unchanged tree must produce an unchanged status digest");

        Files.writeString(
                repository.resolve("added.txt"),
                "added-content\n",
                StandardCharsets.UTF_8);
        List<WorkspaceSourceObservation> changed = new GitWorkspaceCollector().observe(
                repository,
                OBSERVED_AT);
        assertTrue(changed.get(0).contentSha256().isPresent());
        assertTrue(!changed.get(0).contentSha256().equals(status.contentSha256()),
                "a changed tree must produce a changed status digest");
    }

    @Test
    void observesANonRepositoryAsExplicitlyUnavailable() throws Exception {
        assumeTrue(gitAvailable(), "git is not available on this host");
        Path plainDirectory = Files.createDirectories(temporaryRoot.resolve("plain"));

        List<WorkspaceSourceObservation> observations = new GitWorkspaceCollector().observe(
                plainDirectory,
                OBSERVED_AT);

        assertEquals(2, observations.size());
        for (WorkspaceSourceObservation observation : observations) {
            assertEquals(WorkspaceSourceState.UNAVAILABLE, observation.state());
            assertEquals(Optional.empty(), observation.contentSha256());
            assertTrue(observation.reason().isPresent());
        }
    }

    @Test
    void rejectsMissingInputs() {
        GitWorkspaceCollector collector = new GitWorkspaceCollector();

        assertThrows(NullPointerException.class, () -> collector.observe(null, OBSERVED_AT));
        assertThrows(
                NullPointerException.class,
                () -> collector.observe(temporaryRoot, null));
    }

    private static boolean gitAvailable() {
        try {
            Process process = new ProcessBuilder("git", "--version")
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void git(Path workingDirectory, String... arguments) throws Exception {
        String[] command = new String[arguments.length + 1];
        command[0] = "git";
        System.arraycopy(arguments, 0, command, 1, arguments.length);
        Process process = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true)
                .start();
        process.getInputStream().readAllBytes();
        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "git command timed out");
        assertEquals(0, process.exitValue(), "git command failed");
    }
}
