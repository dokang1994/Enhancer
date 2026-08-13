package com.enhancer.maintenance.installation.windows;

import com.enhancer.maintenance.installation.InstallationArtifact;
import com.enhancer.maintenance.installation.InstallationPrincipalRole;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Exact post-operation security snapshot for one planned artifact identity. */
public record WindowsArtifactSecuritySnapshot(
        UUID transactionId,
        InstallationArtifact artifact,
        WindowsObjectType objectType,
        WindowsFileIdentity identity,
        WindowsDaclEvidence dacl,
        Map<InstallationPrincipalRole, WindowsPrincipalArtifactAccess> access) {
    public WindowsArtifactSecuritySnapshot {
        transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        artifact = Objects.requireNonNull(artifact, "artifact must not be null");
        objectType = Objects.requireNonNull(objectType, "objectType must not be null");
        identity = Objects.requireNonNull(identity, "identity must not be null");
        dacl = Objects.requireNonNull(dacl, "dacl must not be null");
        Map<InstallationPrincipalRole, WindowsPrincipalArtifactAccess> checked =
                Objects.requireNonNull(access, "access must not be null");
        if (!checked.keySet().equals(EnumSet.allOf(InstallationPrincipalRole.class))) {
            throw new IllegalArgumentException("access must contain every role");
        }
        EnumMap<InstallationPrincipalRole, WindowsPrincipalArtifactAccess> copied =
                new EnumMap<>(InstallationPrincipalRole.class);
        for (InstallationPrincipalRole role : InstallationPrincipalRole.values()) {
            WindowsPrincipalArtifactAccess roleAccess = Objects.requireNonNull(
                    checked.get(role), "role access must not be null");
            if (roleAccess.role() != role) {
                throw new IllegalArgumentException("role access key differs from value");
            }
            copied.put(role, roleAccess);
        }
        access = Map.copyOf(copied);
    }
}
