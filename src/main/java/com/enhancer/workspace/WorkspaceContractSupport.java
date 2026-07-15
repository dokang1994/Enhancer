package com.enhancer.workspace;

import java.util.Objects;
import java.util.regex.Pattern;

final class WorkspaceContractSupport {
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    private WorkspaceContractSupport() {
    }

    static String bounded(String value, String name, int maximumCharacters) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (value.length() > maximumCharacters) {
            throw new IllegalArgumentException(
                    name + " must not exceed " + maximumCharacters + " characters");
        }
        return value;
    }

    static String sha256(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (!SHA_256.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    name + " must be 64 lowercase hexadecimal characters");
        }
        return value;
    }
}
