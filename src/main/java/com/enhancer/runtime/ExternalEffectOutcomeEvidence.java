package com.enhancer.runtime;

import java.util.Objects;
import java.util.regex.Pattern;

/** Immutable Evidence Store binding for one terminal external-effect outcome. */
public record ExternalEffectOutcomeEvidence(
        String reference,
        String sha256) {

    public static final int MAX_REFERENCE_CHARACTERS = 1024;
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public ExternalEffectOutcomeEvidence {
        Objects.requireNonNull(reference, "reference must not be null");
        Objects.requireNonNull(sha256, "sha256 must not be null");
        if (reference.isBlank()) {
            throw new IllegalArgumentException("reference must not be blank");
        }
        if (reference.length() > MAX_REFERENCE_CHARACTERS) {
            throw new IllegalArgumentException(
                    "reference must not exceed "
                            + MAX_REFERENCE_CHARACTERS
                            + " characters");
        }
        if (!SHA_256.matcher(sha256).matches()) {
            throw new IllegalArgumentException(
                    "sha256 must be 64 lowercase hexadecimal characters");
        }
    }
}
