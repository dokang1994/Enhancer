package com.enhancer.tool;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record StoredEvidence(
        String runId,
        String evidenceId,
        String reference,
        Instant storedAt,
        long contentLength,
        String sha256) {

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public StoredEvidence {
        requireCanonicalUuid(runId, "runId");
        requireCanonicalUuid(evidenceId, "evidenceId");
        Objects.requireNonNull(reference, "reference must not be null");
        Objects.requireNonNull(storedAt, "storedAt must not be null");
        Objects.requireNonNull(sha256, "sha256 must not be null");

        String expectedReference = "evidence/" + runId + "/" + evidenceId;
        if (!reference.equals(expectedReference)) {
            throw new IllegalArgumentException("reference does not match evidence identity");
        }
        if (contentLength < 0) {
            throw new IllegalArgumentException("contentLength must not be negative");
        }
        if (!SHA_256.matcher(sha256).matches()) {
            throw new IllegalArgumentException("sha256 must be 64 lowercase hexadecimal characters");
        }
    }

    static void requireCanonicalUuid(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        try {
            if (!UUID.fromString(value).toString().equals(value)) {
                throw new IllegalArgumentException(fieldName + " must be a canonical UUID");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + " must be a canonical UUID", exception);
        }
    }
}
