package com.enhancer.maintenance.installation;

import java.nio.file.Path;
import java.util.Objects;

/** One exact planned artifact path; existence or ownership is not inferred. */
public record InstallationArtifact(InstallationArtifactKind kind, Path path) {
    public InstallationArtifact {
        kind = Objects.requireNonNull(kind, "kind must not be null");
        path = exactPath(path, "path");
    }

    static Path exactPath(Path value, String name) {
        Path checked = Objects.requireNonNull(value, name + " must not be null");
        if (!checked.isAbsolute() || !checked.equals(checked.normalize())) {
            throw new IllegalArgumentException(name + " must be absolute and normalized");
        }
        if (checked.toString().length() > 4096) {
            throw new IllegalArgumentException(name + " is outside supported bounds");
        }
        return checked;
    }
}
