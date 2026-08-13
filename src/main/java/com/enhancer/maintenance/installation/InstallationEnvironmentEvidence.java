package com.enhancer.maintenance.installation;

import java.util.Objects;
import java.util.UUID;

/** Bounded normalized environment evidence; it is neither authority nor success. */
public record InstallationEnvironmentEvidence(
        UUID transactionId,
        String adapterId,
        String adapterVersion,
        InstallationPrincipalSet resolvedPrincipals,
        String filesystemIdentity,
        boolean sameFilesystem,
        boolean linksAbsent) {

    public InstallationEnvironmentEvidence {
        transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        adapterId = InstallationPrincipal.boundedText(adapterId, "adapterId");
        adapterVersion = InstallationPrincipal.boundedText(adapterVersion, "adapterVersion");
        resolvedPrincipals = Objects.requireNonNull(
                resolvedPrincipals, "resolvedPrincipals must not be null");
        filesystemIdentity = InstallationPrincipal.boundedText(
                filesystemIdentity, "filesystemIdentity");
        if (!sameFilesystem || !linksAbsent) {
            throw new IllegalArgumentException("environment evidence must be fail-closed");
        }
    }
}
