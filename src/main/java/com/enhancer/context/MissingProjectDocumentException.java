package com.enhancer.context;

import java.io.IOException;
import java.nio.file.Path;

public final class MissingProjectDocumentException extends IOException {
    private static final long serialVersionUID = 1L;

    private final Path documentPath;

    public MissingProjectDocumentException(Path documentPath) {
        super("Required project document is missing: " + documentPath);
        this.documentPath = documentPath;
    }

    public Path documentPath() {
        return documentPath;
    }
}
