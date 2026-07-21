package com.enhancer.session;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Content identity of one project-relative working-tree artifact at a checkpoint. */
public record DevelopmentSessionArtifact(
        String path,
        boolean present,
        Optional<String> contentSha256) {
    private static final int MAX_PATH_CHARACTERS = 1024;
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public DevelopmentSessionArtifact {
        path = bounded(path, "path", MAX_PATH_CHARACTERS);
        Objects.requireNonNull(contentSha256, "contentSha256 must not be null");
        if (present != contentSha256.isPresent()) {
            throw new IllegalArgumentException(
                    "present artifacts require a digest and missing artifacts prohibit one");
        }
        contentSha256.ifPresent(digest -> {
            if (!SHA_256.matcher(digest).matches()) {
                throw new IllegalArgumentException(
                        "contentSha256 must be 64 lowercase hexadecimal characters");
            }
        });
    }

    private static String bounded(String value, String name, int maximum) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank() || value.length() > maximum) {
            throw new IllegalArgumentException(
                    name + " must be non-blank and at most " + maximum + " characters");
        }
        return value;
    }
}
