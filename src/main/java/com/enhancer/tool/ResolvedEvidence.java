package com.enhancer.tool;

import java.util.Objects;

public record ResolvedEvidence(
        StoredEvidence metadata,
        String content) {

    public ResolvedEvidence {
        Objects.requireNonNull(metadata, "metadata must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }
}
