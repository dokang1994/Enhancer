package com.enhancer.workspace;

import com.enhancer.io.BoundedFileOperations;
import com.enhancer.io.FileSizeLimitExceededException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only Workspace source adapter for the governed run's target file. It reads the target
 * once under real-path containment to compute a digest; content is never retained. A missing or
 * oversized target is an explicit Unavailable observation, while a containment violation is a
 * configuration error and is rejected rather than observed.
 */
public final class TargetFileMetadataCollector {
    public static final long MAX_TARGET_BYTES = 64L * 1024 * 1024;

    private static final String PROVENANCE = "target-file-reader";

    public WorkspaceSourceObservation observe(
            Path projectRoot,
            String relativeTargetPath,
            Instant observedAt) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(relativeTargetPath, "relativeTargetPath must not be null");
        Objects.requireNonNull(observedAt, "observedAt must not be null");
        if (relativeTargetPath.isBlank()) {
            throw new IllegalArgumentException("relativeTargetPath must not be blank");
        }

        Path relative = Path.of(relativeTargetPath);
        if (relative.isAbsolute()) {
            throw new IllegalArgumentException("target path must be relative");
        }
        Path realProjectRoot = projectRoot.toRealPath();
        Path candidate = realProjectRoot.resolve(relative).normalize();
        if (!candidate.startsWith(realProjectRoot)) {
            throw new IllegalArgumentException(
                    "target path resolves outside the project root: " + relativeTargetPath);
        }

        if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
            return unavailable(relativeTargetPath, observedAt, "target file does not exist");
        }
        Path realTarget = candidate.toRealPath();
        if (!realTarget.startsWith(realProjectRoot)) {
            throw new IllegalArgumentException(
                    "target path resolves outside the project root: " + relativeTargetPath);
        }
        if (!Files.isRegularFile(realTarget, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException(
                    "target path is not a regular file: " + relativeTargetPath);
        }
        if (Files.size(realTarget) > MAX_TARGET_BYTES) {
            return unavailable(
                    relativeTargetPath,
                    observedAt,
                    "target file exceeds the supported " + MAX_TARGET_BYTES + " byte bound");
        }

        try {
            return new WorkspaceSourceObservation(
                    WorkspaceSourceKind.REPOSITORY_FILE,
                    relativeTargetPath,
                    PROVENANCE,
                    observedAt,
                    Optional.empty(),
                    WorkspaceSourceState.AVAILABLE,
                    Optional.of(HexFormat.of().formatHex(
                            BoundedFileOperations.sha256(
                                    realTarget,
                                    MAX_TARGET_BYTES))),
                    Optional.empty());
        } catch (FileSizeLimitExceededException exception) {
            return unavailable(
                    relativeTargetPath,
                    observedAt,
                    "target file changed beyond the supported "
                            + MAX_TARGET_BYTES + " byte bound while reading");
        }
    }

    private WorkspaceSourceObservation unavailable(
            String relativeTargetPath,
            Instant observedAt,
            String reason) {
        return new WorkspaceSourceObservation(
                WorkspaceSourceKind.REPOSITORY_FILE,
                relativeTargetPath,
                PROVENANCE,
                observedAt,
                Optional.empty(),
                WorkspaceSourceState.UNAVAILABLE,
                Optional.empty(),
                Optional.of(reason));
    }

}
