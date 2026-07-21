package com.enhancer.session;

import com.enhancer.io.BoundedFileOperations;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class DevelopmentSessionArtifactCollector {
    private static final long MAX_ARTIFACT_BYTES = 64L * 1024 * 1024;
    private final Path realProjectRoot;

    DevelopmentSessionArtifactCollector(Path projectRoot) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        this.realProjectRoot = projectRoot.toRealPath();
    }

    List<DevelopmentSessionArtifact> capture(List<String> paths) throws IOException {
        Objects.requireNonNull(paths, "paths must not be null");
        if (paths.size() > DevelopmentSessionCheckpoint.MAX_ARTIFACTS) {
            throw new IllegalArgumentException("too many artifact paths");
        }
        Set<String> normalizedPaths = new LinkedHashSet<>();
        for (String value : paths) {
            Path path = Path.of(Objects.requireNonNull(value, "artifact path must not be null"));
            if (path.isAbsolute()) {
                throw new IllegalArgumentException("artifact path must be relative: " + value);
            }
            Path normalized = path.normalize();
            if (normalized.toString().isBlank() || normalized.startsWith("..")) {
                throw new IllegalArgumentException("artifact path escapes project root: " + value);
            }
            String portable = normalized.toString().replace('\\', '/');
            if (!normalizedPaths.add(portable)) {
                throw new IllegalArgumentException("duplicate artifact path: " + portable);
            }
        }

        List<DevelopmentSessionArtifact> artifacts = new ArrayList<>();
        for (String relative : normalizedPaths) {
            Path target = realProjectRoot.resolve(relative).normalize();
            if (!target.startsWith(realProjectRoot)) {
                throw new IllegalArgumentException("artifact path escapes project root: " + relative);
            }
            if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                artifacts.add(new DevelopmentSessionArtifact(
                        relative, false, Optional.empty()));
                continue;
            }
            if (Files.isSymbolicLink(target)) {
                throw new IOException("artifact path must not be a symbolic link: " + relative);
            }
            Path realTarget = target.toRealPath();
            if (!realTarget.startsWith(realProjectRoot)
                    || !Files.isRegularFile(realTarget, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("artifact path must be a contained regular file: " + relative);
            }
            String digest = HexFormat.of().formatHex(
                    BoundedFileOperations.sha256(realTarget, MAX_ARTIFACT_BYTES));
            artifacts.add(new DevelopmentSessionArtifact(
                    relative, true, Optional.of(digest)));
        }
        return List.copyOf(artifacts);
    }
}
