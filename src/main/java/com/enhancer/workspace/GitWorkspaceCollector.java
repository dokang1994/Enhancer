package com.enhancer.workspace;

import com.enhancer.text.UnicodeText;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Read-only Workspace source adapter over the Git working tree. Under the explicitly granted
 * external command authority it executes one fixed read-only command through a trusted
 * absolute executable with no shell, inherited Git overrides, conversion filters, external diff
 * helpers, or text conversion. It applies a hard timeout
 * and bounded output cap, and stores only a digest of each output. Every failure is an explicit
 * Unavailable observation; no mutating invocation exists here, and this authority does not
 * extend to any other component.
 */
public final class GitWorkspaceCollector {
    public static final int MAX_OUTPUT_BYTES = 4 * 1024 * 1024;

    static final List<String> STATUS_ARGUMENTS = List.of(
            "--no-optional-locks", "-c", "core.fsmonitor=false",
            "ls-files", "--stage", "--deleted", "--others",
            "--exclude-standard");
    private static final String DIFF_DISABLED_REASON =
            "tracked worktree diff observation is disabled because Git may execute clean filters";

    private static final String PROVENANCE = "git-cli";
    static final long TIMEOUT_SECONDS = 5;
    private static final int MAX_REASON_CHARACTERS =
            WorkspaceSourceObservation.MAX_REASON_CHARACTERS;

    public List<WorkspaceSourceObservation> observe(Path projectRoot, Instant observedAt) {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(observedAt, "observedAt must not be null");

        Optional<Path> gitExecutable = resolveGitExecutable(projectRoot, System.getenv());
        if (gitExecutable.isEmpty()) {
            String reason = "trusted git executable is unavailable";
            return List.of(
                    unavailable(WorkspaceSourceKind.GIT_STATUS, "working-tree", observedAt, reason),
                    unavailable(WorkspaceSourceKind.GIT_DIFF, "working-tree-diff", observedAt, reason));
        }

        return List.of(
                observe(
                        projectRoot,
                        observedAt,
                        WorkspaceSourceKind.GIT_STATUS,
                        "working-tree",
                        command(gitExecutable.orElseThrow(), STATUS_ARGUMENTS)),
                unavailable(
                        WorkspaceSourceKind.GIT_DIFF,
                        "working-tree-diff",
                        observedAt,
                        DIFF_DISABLED_REASON));
    }

    static Optional<Path> resolveGitExecutable(
            Path projectRoot,
            Map<String, String> environment) {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(environment, "environment must not be null");
        String pathValue = environment.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase("PATH"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("");
        if (pathValue.isBlank()) {
            return Optional.empty();
        }

        try {
            Path absoluteProjectRoot = projectRoot.toAbsolutePath().normalize();
            Path realProjectRoot = projectRoot.toRealPath();
            String executableName = isWindows() ? "git.exe" : "git";
            for (String rawEntry : pathValue.split(java.util.regex.Pattern.quote(
                    File.pathSeparator))) {
                String entry = stripSurroundingQuotes(rawEntry.trim());
                if (entry.isEmpty()) {
                    continue;
                }
                Path directory;
                try {
                    directory = Path.of(entry);
                } catch (RuntimeException invalidPath) {
                    continue;
                }
                if (!directory.isAbsolute()) {
                    continue;
                }
                Path candidate = directory.resolve(executableName).toAbsolutePath().normalize();
                if (candidate.startsWith(absoluteProjectRoot)) {
                    continue;
                }
                if (!Files.isRegularFile(candidate)) {
                    continue;
                }
                Path realCandidate;
                try {
                    realCandidate = candidate.toRealPath();
                } catch (IOException inaccessibleCandidate) {
                    continue;
                }
                if (!realCandidate.startsWith(realProjectRoot)
                        && (isWindows() || Files.isExecutable(realCandidate))) {
                    return Optional.of(realCandidate);
                }
            }
        } catch (IOException inaccessibleProject) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static List<String> command(Path executable, List<String> arguments) {
        List<String> command = new ArrayList<>(arguments.size() + 1);
        command.add(executable.toString());
        command.addAll(arguments);
        return List.copyOf(command);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static String stripSurroundingQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private WorkspaceSourceObservation observe(
            Path projectRoot,
            Instant observedAt,
            WorkspaceSourceKind kind,
            String sourceId,
            List<String> command) {
        try {
            return available(kind, sourceId, observedAt, digest(projectRoot, command));
        } catch (IOException exception) {
            return unavailable(kind, sourceId, observedAt, reason(exception));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return unavailable(kind, sourceId, observedAt, "git observation was interrupted");
        }
    }

    private String digest(Path projectRoot, List<String> command)
            throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(projectRoot.toFile())
                .redirectError(ProcessBuilder.Redirect.DISCARD);
        sanitizeEnvironment(builder.environment());
        Path parent = projectRoot.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            builder.environment().put("GIT_CEILING_DIRECTORIES", parent.toString());
        }
        Process process = builder.start();
        AtomicBoolean timedOut = new AtomicBoolean(false);
        Thread watchdog = watchdog(process, timedOut);
        try {
            process.getOutputStream().close();
            MessageDigest digest = sha256();
            long total = 0;
            byte[] buffer = new byte[64 * 1024];
            try (InputStream output = process.getInputStream()) {
                int read;
                while ((read = output.read(buffer)) >= 0) {
                    total += read;
                    if (total > MAX_OUTPUT_BYTES) {
                        throw new IOException(
                                "git output exceeds the supported "
                                        + MAX_OUTPUT_BYTES + " byte bound");
                    }
                    digest.update(buffer, 0, read);
                }
            }
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    || timedOut.get()) {
                throw new IOException("git invocation timed out");
            }
            if (process.exitValue() != 0) {
                throw new IOException("git exited with status " + process.exitValue());
            }
            return HexFormat.of().formatHex(digest.digest());
        } finally {
            process.destroyForcibly();
            watchdog.interrupt();
        }
    }

    static void sanitizeEnvironment(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment must not be null");
        environment.keySet().removeIf(key ->
                key.regionMatches(true, 0, "GIT_", 0, "GIT_".length()));
    }

    private static Thread watchdog(Process process, AtomicBoolean timedOut) {
        Thread watchdog = new Thread(() -> {
            try {
                if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    timedOut.set(true);
                    process.destroyForcibly();
                }
            } catch (InterruptedException expected) {
                Thread.currentThread().interrupt();
            }
        }, "git-observation-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        return watchdog;
    }

    private WorkspaceSourceObservation available(
            WorkspaceSourceKind kind,
            String sourceId,
            Instant observedAt,
            String digest) {
        return new WorkspaceSourceObservation(
                kind,
                sourceId,
                PROVENANCE,
                observedAt,
                Optional.empty(),
                WorkspaceSourceState.AVAILABLE,
                Optional.of(digest),
                Optional.empty());
    }

    private WorkspaceSourceObservation unavailable(
            WorkspaceSourceKind kind,
            String sourceId,
            Instant observedAt,
            String reason) {
        return new WorkspaceSourceObservation(
                kind,
                sourceId,
                PROVENANCE,
                observedAt,
                Optional.empty(),
                WorkspaceSourceState.UNAVAILABLE,
                Optional.empty(),
                Optional.of(reason));
    }

    private static String reason(IOException exception) {
        String message = exception.getMessage();
        String reason = message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
        return UnicodeText.prefix(reason, MAX_REASON_CHARACTERS);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
