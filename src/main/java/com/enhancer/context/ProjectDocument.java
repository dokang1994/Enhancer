package com.enhancer.context;

import java.util.Objects;

public record ProjectDocument(String path, int readOrder, String content) {
    public ProjectDocument {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(content, "content must not be null");
        if (readOrder < 1) {
            throw new IllegalArgumentException("readOrder must be positive");
        }
    }
}
