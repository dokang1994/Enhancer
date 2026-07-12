package com.enhancer.context;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ProjectContextReader {
    public ProjectContext read(Path projectRoot) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");

        List<ProjectDocument> documents = new ArrayList<>();
        RequiredProjectDocument[] requiredDocuments = RequiredProjectDocument.values();

        for (int index = 0; index < requiredDocuments.length; index++) {
            RequiredProjectDocument requiredDocument = requiredDocuments[index];
            Path documentPath = projectRoot.resolve(requiredDocument.path());
            if (!Files.isRegularFile(documentPath)) {
                throw new MissingProjectDocumentException(documentPath);
            }

            String content = Files.readString(documentPath, StandardCharsets.UTF_8);
            documents.add(new ProjectDocument(requiredDocument.path(), index + 1, content));
        }

        return new ProjectContext(documents);
    }
}
