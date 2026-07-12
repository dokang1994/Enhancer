package com.enhancer.context;

import java.util.List;
import java.util.Objects;

public record ProjectContext(List<ProjectDocument> documents) {
    public ProjectContext {
        Objects.requireNonNull(documents, "documents must not be null");
        documents = List.copyOf(documents);
    }
}
