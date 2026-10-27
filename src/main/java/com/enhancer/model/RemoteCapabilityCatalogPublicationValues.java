package com.enhancer.model;

import java.util.Objects;
import java.util.regex.Pattern;

/** Shared value checks for the pure RFC-0032 publication contract. */
final class RemoteCapabilityCatalogPublicationValues {
    private static final Pattern IDENTITY =
            Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    private RemoteCapabilityCatalogPublicationValues() {
    }

    static String identity(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.length() > 256 || !IDENTITY.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    name + " must be a bounded stable lowercase label");
        }
        return value;
    }

    static String sha256(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (!SHA_256.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    name + " must be a lowercase SHA-256 digest");
        }
        return value;
    }
}
