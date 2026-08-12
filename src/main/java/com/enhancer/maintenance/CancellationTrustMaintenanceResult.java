package com.enhancer.maintenance;

import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/** Exact public result of a successful installed-trust maintenance operation. */
public record CancellationTrustMaintenanceResult(
        CancellationTrustMaintenanceStatus status,
        Path policyFile,
        String policySha256,
        String metadataSha256) {
    private static final Pattern LOWERCASE_SHA256 = Pattern.compile("[0-9a-f]{64}");

    public CancellationTrustMaintenanceResult {
        status = Objects.requireNonNull(status, "status must not be null");
        policyFile = Objects.requireNonNull(policyFile, "policyFile must not be null");
        if (!policyFile.isAbsolute() || !policyFile.equals(policyFile.normalize())) {
            throw new IllegalArgumentException(
                    "policyFile must be an absolute normalized path");
        }
        policySha256 = Objects.requireNonNull(policySha256, "policySha256 must not be null");
        metadataSha256 = Objects.requireNonNull(
                metadataSha256, "metadataSha256 must not be null");
        if (!LOWERCASE_SHA256.matcher(policySha256).matches()
                || !LOWERCASE_SHA256.matcher(metadataSha256).matches()) {
            throw new IllegalArgumentException("digests must be lowercase SHA-256");
        }
    }
}
