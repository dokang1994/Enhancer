package com.enhancer.brain;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public record GraphProvenance(
        String sourceRef,
        Optional<String> sourceSha256,
        GraphElementFreshness freshness) {

    public static final int MAX_SOURCE_REF_CHARACTERS = 1024;

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public GraphProvenance {
        Objects.requireNonNull(sourceRef, "sourceRef must not be null");
        Objects.requireNonNull(sourceSha256, "sourceSha256 must not be null");
        Objects.requireNonNull(freshness, "freshness must not be null");
        if (sourceRef.isBlank()) {
            throw new IllegalArgumentException("sourceRef must not be blank");
        }
        if (sourceRef.length() > MAX_SOURCE_REF_CHARACTERS) {
            throw new IllegalArgumentException(
                    "sourceRef must not exceed " + MAX_SOURCE_REF_CHARACTERS + " characters");
        }
        sourceSha256.ifPresent(value -> {
            if (!SHA_256.matcher(value).matches()) {
                throw new IllegalArgumentException(
                        "sourceSha256 must be 64 lowercase hexadecimal characters");
            }
        });
        if (freshness == GraphElementFreshness.SOURCE_MISSING) {
            if (sourceSha256.isPresent()) {
                throw new IllegalArgumentException(
                        "Source-missing provenance cannot carry sourceSha256");
            }
        } else if (sourceSha256.isEmpty()) {
            throw new IllegalArgumentException(
                    freshness + " provenance requires sourceSha256");
        }
    }

    public boolean rebuildRequired() {
        return freshness.rebuildRequired();
    }
}
