package com.enhancer.maintenance.installation.windows;

import java.nio.file.Path;
import java.util.Objects;

/** Identity and reparse status for one exact planned path component. */
public record WindowsPathComponentEvidence(
        Path path,
        WindowsObjectType objectType,
        WindowsFileIdentity identity,
        boolean reparsePoint) {
    public WindowsPathComponentEvidence {
        path = Objects.requireNonNull(path, "path must not be null");
        if (!path.isAbsolute() || !path.equals(path.normalize())) {
            throw new IllegalArgumentException("path must be absolute and normalized");
        }
        objectType = Objects.requireNonNull(objectType, "objectType must not be null");
        identity = Objects.requireNonNull(identity, "identity must not be null");
    }
}
