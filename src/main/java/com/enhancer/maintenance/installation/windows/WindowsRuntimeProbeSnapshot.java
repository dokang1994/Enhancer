package com.enhancer.maintenance.installation.windows;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record WindowsRuntimeProbeSnapshot(
        UUID transactionId,
        WindowsSid runtimeSid,
        String metadataSha256,
        String policySha256,
        boolean metadataRead,
        boolean policyRead,
        boolean mutationObserved) {
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    public WindowsRuntimeProbeSnapshot {
        transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        runtimeSid = Objects.requireNonNull(runtimeSid, "runtimeSid must not be null");
        metadataSha256 = digest(metadataSha256, "metadataSha256");
        policySha256 = digest(policySha256, "policySha256");
    }

    private static String digest(String value, String name) {
        String checked = Objects.requireNonNull(value, name + " must not be null");
        if (!SHA256.matcher(checked).matches()) {
            throw new IllegalArgumentException(name + " must be lowercase SHA-256");
        }
        return checked;
    }
}
