package com.enhancer.maintenance.installation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Bounded semantic identity returned for one phase. This value contains and proves no
 * evidence body, storage integrity, durability, or installation effect.
 */
public record InstallationPhaseEvidence(
        int schemaVersion,
        UUID transactionId,
        InstallationPhase phase,
        long pendingRevision,
        String semanticEvidenceSha256,
        Optional<String> observedActivationIdentity) {
    public static final int SCHEMA_VERSION = 1;
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final List<InstallationPhase> ORDER = InstallationPhase.requiredOrder();

    public InstallationPhaseEvidence {
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException("schemaVersion is unsupported");
        }
        transactionId = Objects.requireNonNull(
                transactionId, "transactionId must not be null");
        phase = Objects.requireNonNull(phase, "phase must not be null");
        int phaseIndex = ORDER.indexOf(phase);
        if (phaseIndex < 0 || pendingRevision != (long) phaseIndex * 2) {
            throw new IllegalArgumentException(
                    "pendingRevision must identify the exact pending phase");
        }
        semanticEvidenceSha256 = Objects.requireNonNull(
                semanticEvidenceSha256, "semanticEvidenceSha256 must not be null");
        if (!SHA256.matcher(semanticEvidenceSha256).matches()) {
            throw new IllegalArgumentException(
                    "semanticEvidenceSha256 must be lowercase SHA-256");
        }
        observedActivationIdentity = Objects.requireNonNull(
                observedActivationIdentity,
                "observedActivationIdentity must not be null");
        observedActivationIdentity = observedActivationIdentity.map(value ->
                InstallationPrincipal.boundedText(value, "observedActivationIdentity"));
    }
}
