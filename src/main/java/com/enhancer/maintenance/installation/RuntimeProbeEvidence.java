package com.enhancer.maintenance.installation;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Read-only trust-loader probe evidence produced for the exact runtime identity. */
public record RuntimeProbeEvidence(
        UUID transactionId,
        InstallationPrincipal runtimePrincipal,
        String metadataSha256,
        String policySha256,
        boolean readSucceeded,
        boolean mutationObserved) {
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    public RuntimeProbeEvidence {
        transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        runtimePrincipal = Objects.requireNonNull(runtimePrincipal,
                "runtimePrincipal must not be null");
        if (runtimePrincipal.role() != InstallationPrincipalRole.RUNTIME) {
            throw new IllegalArgumentException("runtimePrincipal must have runtime role");
        }
        metadataSha256 = digest(metadataSha256, "metadataSha256");
        policySha256 = digest(policySha256, "policySha256");
        if (!readSucceeded || mutationObserved) {
            throw new IllegalArgumentException("runtime probe must be read-only and successful");
        }
    }

    private static String digest(String value, String name) {
        String checked = Objects.requireNonNull(value, name + " must not be null");
        if (!SHA256.matcher(checked).matches()) {
            throw new IllegalArgumentException(name + " must be lowercase SHA-256");
        }
        return checked;
    }
}
