package com.enhancer.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Shared intrinsic-shape checks; this class performs no policy evaluation. */
final class ModelOutboundRouteValueChecks {

    static final int MAX_IDENTITY_CHARACTERS = 256;
    static final int MAX_REVISION_CHARACTERS = 256;
    static final int MAX_COLLECTION_ENTRIES = 256;

    private static final Pattern STABLE_LABEL =
            Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    private ModelOutboundRouteValueChecks() {
    }

    static String label(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.length() > MAX_IDENTITY_CHARACTERS
                || !STABLE_LABEL.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    fieldName + " must be a bounded stable lowercase label");
        }
        return value;
    }

    static String revision(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.length() > MAX_REVISION_CHARACTERS
                || !STABLE_LABEL.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    fieldName + " must be a bounded stable lowercase revision");
        }
        return value;
    }

    static void window(Instant observedAt, Instant expiresAt) {
        Objects.requireNonNull(observedAt, "observedAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (!observedAt.isBefore(expiresAt)) {
            throw new IllegalArgumentException("observedAt must precede expiresAt");
        }
    }

    static String digest(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (!SHA_256.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    fieldName + " must be a lowercase SHA-256 digest");
        }
        return value;
    }

    static <T> Set<T> immutableSet(Set<T> source, String fieldName) {
        Objects.requireNonNull(source, fieldName + " must not be null");
        if (source.size() > MAX_COLLECTION_ENTRIES) {
            throw new IllegalArgumentException(fieldName + " exceeds the supported bound");
        }
        LinkedHashSet<T> copy = new LinkedHashSet<>();
        for (T value : source) {
            copy.add(Objects.requireNonNull(value, fieldName + " must not contain null"));
        }
        return Collections.unmodifiableSet(copy);
    }
}
