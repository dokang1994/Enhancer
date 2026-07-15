package com.enhancer.bus;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

final class BusContractSupport {
    static final int MAX_IDENTITY_CHARACTERS = 256;
    static final int MAX_REFERENCE_CHARACTERS = 1024;

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    private BusContractSupport() {
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

    static String canonicalUuid(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        try {
            if (!UUID.fromString(value).toString().equals(value)) {
                throw new IllegalArgumentException(name + " must be a canonical UUID");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(name + " must be a canonical UUID", exception);
        }
        return value;
    }
}
