package com.enhancer.runtime;

import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/** Protected installation binding for one exact pinned cancellation policy. */
public record InstalledCancellationTrustMetadata(
        Path policyFile,
        String expectedSha256) {
    private static final Pattern LOWERCASE_SHA256 = Pattern.compile("[0-9a-f]{64}");

    public InstalledCancellationTrustMetadata {
        Objects.requireNonNull(policyFile, "policyFile must not be null");
        if (!policyFile.isAbsolute() || !policyFile.equals(policyFile.normalize())) {
            throw new IllegalArgumentException(
                    "policyFile must be an absolute normalized path");
        }
        Objects.requireNonNull(expectedSha256, "expectedSha256 must not be null");
        if (!LOWERCASE_SHA256.matcher(expectedSha256).matches()) {
            throw new IllegalArgumentException(
                    "expectedSha256 must be lowercase SHA-256");
        }
    }
}
