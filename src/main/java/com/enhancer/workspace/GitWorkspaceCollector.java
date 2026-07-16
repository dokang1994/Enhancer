package com.enhancer.workspace;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Read-only Workspace source adapter over the Git working tree. Under the explicitly granted
 * external command authority it executes exactly two fixed read-only commands with no shell,
 * inherited Git overrides, external diff helpers, or text conversion. It applies a hard timeout
 * and bounded output cap, and stores only a digest of each output. Every failure is an explicit
 * Unavailable observation; no mutating invocation exists here, and this authority does not
 * extend to any other component.
 */
public final class GitWorkspaceCollector {
    public static final int MAX_OUTPUT_BYTES = 4 * 1024 * 1024;

    static final List<String> STATUS_COMMAND = List.of(
            "git", "--no-optional-locks", "-c", "core.fsmonitor=false",
            "status", "--porcelain");
    static final List<String> DIFF_COMMAND = List.of(
            "git", "--no-optional-locks", "-c", "core.fsmonitor=false",
            "diff", "--no-ext-diff", "--no-textconv");

    private static final String PROVENANCE = "git-cli";
    private static final long TIMEOUT_SECONDS = 5;
    private static final int MAX_REASON_CHARACTERS =
            WorkspaceSourceObservation.MAX_REASON_CHARACTERS;

    public List<WorkspaceSourceObservation> observe(Path projectRoot, Instant observedAt) {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(observedAt, "observedAt must not be null");

        return List.of(
                observe(
                        projectRoot,
                        observedAt,
                        WorkspaceSourceKind.GIT_STATUS,
                        "working-tree",
                        STATUS_COMMAND),
                observe(
                        projectRoot,
                        observedAt,
                        WorkspaceSourceKind.GIT_DIFF,
                        "working-tree-diff",
                        DIFF_COMMAND));
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
        return reason.length() <= MAX_REASON_CHARACTERS
                ? reason
                : reason.substring(0, MAX_REASON_CHARACTERS);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
