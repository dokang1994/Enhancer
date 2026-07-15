package com.enhancer.brain;

import java.util.Objects;
import java.util.regex.Pattern;

public record RepositoryMemoryEntry(
        String path,
        int readOrder,
        String sourceSha256,
        MemoryFreshness freshness) {

    public static final int MAX_PATH_CHARACTERS = 1024;

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public RepositoryMemoryEntry {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(sourceSha256, "sourceSha256 must not be null");
        Objects.requireNonNull(freshness, "freshness must not be null");
        if (path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        if (path.length() > MAX_PATH_CHARACTERS) {
            throw new IllegalArgumentException(
                    "path must not exceed " + MAX_PATH_CHARACTERS + " characters");
        }
        if (readOrder < 1) {
            throw new IllegalArgumentException("readOrder must be positive");
        }
        if (!SHA_256.matcher(sourceSha256).matches()) {
            throw new IllegalArgumentException(
                    "sourceSha256 must be 64 lowercase hexadecimal characters");
        }
    }
}
