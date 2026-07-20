package com.enhancer.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        assertEquals(WorkspaceSourceState.UNAVAILABLE, diff.state());
        assertTrue(diff.contentSha256().isEmpty());
        assertTrue(diff.reason().orElseThrow().contains("clean filters"));

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
    void disablesARepositoryConfiguredExternalDiffHelper() throws Exception {
        assumeTrue(gitAvailable(), "git is not available on this host");
        Path repository = Files.createDirectories(temporaryRoot.resolve("external-diff-repository"));
        git(repository, "init");
        Files.writeString(
                repository.resolve("tracked.txt"),
                "original\n",
                StandardCharsets.UTF_8);
        git(repository, "add", "tracked.txt");
        git(repository, "config", "diff.external", "enhancer-helper-that-does-not-exist");
        Files.writeString(
                repository.resolve("tracked.txt"),
                "changed\n",
                StandardCharsets.UTF_8);

        List<WorkspaceSourceObservation> observations = new GitWorkspaceCollector().observe(
                repository,
                OBSERVED_AT);

        assertEquals(WorkspaceSourceState.AVAILABLE, observations.get(0).state());
        assertEquals(WorkspaceSourceState.UNAVAILABLE, observations.get(1).state(),
                "unsafe worktree diff observation must stay disabled");
    }

    @Test
    void doesNotInvokeARequiredCleanFilterWhileObservingAStatDirtyFile() throws Exception {
        assumeTrue(gitAvailable(), "git is not available on this host");
        Path repository = Files.createDirectories(temporaryRoot.resolve("clean-filter-repository"));
        git(repository, "init");
        Files.writeString(repository.resolve(".gitattributes"),
                "tracked.txt filter=proof\n", StandardCharsets.UTF_8);
        Files.writeString(repository.resolve("tracked.txt"),
                "original\n", StandardCharsets.UTF_8);
        git(repository, "add", ".gitattributes", "tracked.txt");
        git(repository, "config", "filter.proof.clean", "enhancer-clean-filter-must-not-run");
        git(repository, "config", "filter.proof.required", "true");
        Files.writeString(repository.resolve("tracked.txt"),
                "modified\n", StandardCharsets.UTF_8);

        List<WorkspaceSourceObservation> observations = new GitWorkspaceCollector().observe(
                repository,
                OBSERVED_AT);

        assertEquals(WorkspaceSourceState.AVAILABLE, observations.get(0).state(),
                "status metadata must not enter the clean-filter pipeline");
        assertEquals(WorkspaceSourceState.UNAVAILABLE, observations.get(1).state(),
                "worktree diff must remain disabled until a filter-free method exists");
        assertTrue(observations.get(1).reason().orElseThrow().contains("clean filters"));
    }

    @Test
    void resolvesAnAbsoluteExecutableOutsideTheObservedProject() throws Exception {
        Path repository = Files.createDirectories(temporaryRoot.resolve("untrusted-project"));
        Path trustedDirectory = Files.createDirectories(temporaryRoot.resolve("trusted-bin"));
        String executableName = isWindows() ? "git.exe" : "git";
        Path projectCandidate = Files.writeString(
                repository.resolve(executableName), "project-controlled", StandardCharsets.UTF_8);
        Path trustedCandidate = Files.writeString(
                trustedDirectory.resolve(executableName), "host-controlled", StandardCharsets.UTF_8);
        projectCandidate.toFile().setExecutable(true);
        trustedCandidate.toFile().setExecutable(true);
        Map<String, String> environment = Map.of(
                "PATH", repository + java.io.File.pathSeparator + trustedDirectory);

        Path resolved = GitWorkspaceCollector.resolveGitExecutable(repository, environment)
                .orElseThrow();

        assertEquals(trustedCandidate.toRealPath(), resolved);
        assertFalse(resolved.startsWith(repository.toRealPath()));
        assertTrue(resolved.isAbsolute());
    }

    @Test
    void refusesEveryCandidateTheObservedProjectCouldControl() throws Exception {
        Path repository = Files.createDirectories(temporaryRoot.resolve("hostile-project"));
        String executableName = isWindows() ? "git.exe" : "git";
        Path projectCandidate = Files.writeString(
                repository.resolve(executableName), "project-controlled", StandardCharsets.UTF_8);
        projectCandidate.toFile().setExecutable(true);
        Path nestedDirectory = Files.createDirectories(repository.resolve("tools"));
        Path nestedCandidate = Files.writeString(
                nestedDirectory.resolve(executableName), "also-project-controlled",
                StandardCharsets.UTF_8);
        nestedCandidate.toFile().setExecutable(true);

        // A repository that ships its own git and puts it on PATH must never be invoked, at the
        // project root or nested beneath it. With no trusted entry there is no fallback.
        Map<String, String> environment = Map.of(
                "PATH", repository + java.io.File.pathSeparator + nestedDirectory);

        assertEquals(
                Optional.empty(),
                GitWorkspaceCollector.resolveGitExecutable(repository, environment),
                "a candidate inside the observed project must never be resolved");
    }

    @Test
    void refusesRelativeMissingAndNonRegularCandidates() throws Exception {
        Path repository = Files.createDirectories(temporaryRoot.resolve("project"));
        Path relativeDirectory = Files.createDirectories(temporaryRoot.resolve("relative-bin"));
        String executableName = isWindows() ? "git.exe" : "git";
        Files.writeString(relativeDirectory.resolve(executableName), "unreachable",
                StandardCharsets.UTF_8);

        // A relative PATH entry resolves against the child's working directory rather than the
        // host's, so it is not trustworthy provenance even when a file sits there.
        assertEquals(
                Optional.empty(),
                GitWorkspaceCollector.resolveGitExecutable(
                        repository, Map.of("PATH", "relative-bin")));

        // An entry naming a directory that does not exist contributes no candidate.
        assertEquals(
                Optional.empty(),
                GitWorkspaceCollector.resolveGitExecutable(
                        repository,
                        Map.of("PATH", temporaryRoot.resolve("absent-bin").toString())));

        // A directory named git.exe is not a regular file and must not be executed.
        Path directoryCandidate = Files.createDirectories(
                temporaryRoot.resolve("directory-bin").resolve(executableName));
        assertEquals(
                Optional.empty(),
                GitWorkspaceCollector.resolveGitExecutable(
                        repository,
                        Map.of("PATH", directoryCandidate.getParent().toString())));
    }

    @Test
    void treatsAnAbsentOrBlankPathAsNoGitRatherThanABareCommandName() throws Exception {
        Path repository = Files.createDirectories(temporaryRoot.resolve("no-path-project"));

        assertEquals(
                Optional.empty(),
                GitWorkspaceCollector.resolveGitExecutable(repository, Map.of()),
                "no PATH must not fall back to invoking a bare command name");
        assertEquals(
                Optional.empty(),
                GitWorkspaceCollector.resolveGitExecutable(repository, Map.of("PATH", "   ")));

        // The lookup is case-insensitive on the variable name, as Windows supplies "Path".
        Path trustedDirectory = Files.createDirectories(temporaryRoot.resolve("case-bin"));
        String executableName = isWindows() ? "git.exe" : "git";
        Path trustedCandidate = Files.writeString(
                trustedDirectory.resolve(executableName), "host-controlled",
                StandardCharsets.UTF_8);
        trustedCandidate.toFile().setExecutable(true);

        assertEquals(
                Optional.of(trustedCandidate.toRealPath()),
                GitWorkspaceCollector.resolveGitExecutable(
                        repository, Map.of("Path", trustedDirectory.toString())));
    }

    @Test
    void boundsGitOutputAndRuntimeExplicitly() {
        // The output cap and watchdog are the containment the authority decision rests on. They
        // are asserted as pinned constants because inducing either needs a purpose-built git that
        // stalls or floods, which this suite deliberately does not ship.
        assertEquals(4 * 1024 * 1024, GitWorkspaceCollector.MAX_OUTPUT_BYTES);
        assertEquals(5L, GitWorkspaceCollector.TIMEOUT_SECONDS);
    }

    @Test
    void removesInheritedGitOverridesWithoutRemovingTheExecutablePath() {
        Map<String, String> environment = new HashMap<>();
        environment.put("PATH", "expected-path");
        environment.put("GIT_DIR", "outside.git");
        environment.put("GIT_WORK_TREE", "outside-work-tree");
        environment.put("GIT_EXTERNAL_DIFF", "external-helper");
        environment.put("GIT_CONFIG_COUNT", "1");
        environment.put("GIT_CONFIG_KEY_0", "diff.external");
        environment.put("GIT_CONFIG_VALUE_0", "injected-helper");

        GitWorkspaceCollector.sanitizeEnvironment(environment);

        assertEquals("expected-path", environment.get("PATH"));
        assertFalse(environment.keySet().stream().anyMatch(key -> key.startsWith("GIT_")),
                "the child process must not inherit Git repository or helper overrides");
    }

    @Test
    void fixedCommandUsesFilterFreeIndexMetadataAndDisablesWorktreeDiff() {
        assertTrue(GitWorkspaceCollector.STATUS_ARGUMENTS.contains("ls-files"));
        assertTrue(GitWorkspaceCollector.STATUS_ARGUMENTS.contains("--deleted"));
        assertTrue(GitWorkspaceCollector.STATUS_ARGUMENTS.contains("--others"));
        assertFalse(GitWorkspaceCollector.STATUS_ARGUMENTS.contains("--modified"));
        assertFalse(GitWorkspaceCollector.STATUS_ARGUMENTS.contains("status"));
        assertEquals(1, List.of(GitWorkspaceCollector.STATUS_ARGUMENTS).size());
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

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT)
                .contains("win");
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
