package com.enhancer.maintenance.installation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Exact allowed/denied partition for all roles on one planned artifact. */
public record ArtifactPermissionEvidence(
        UUID transactionId,
        InstallationArtifact artifact,
        String permissionPolicyRevision,
        String ownerStableIdentity,
        Map<InstallationPrincipalRole, Set<InstallationAccess>> allowedEffectiveAccess,
        Map<InstallationPrincipalRole, Set<InstallationAccess>> deniedEffectiveAccess) {

    public ArtifactPermissionEvidence {
        transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        artifact = Objects.requireNonNull(artifact, "artifact must not be null");
        permissionPolicyRevision = Objects.requireNonNull(
                permissionPolicyRevision, "permissionPolicyRevision must not be null");
        if (!permissionPolicyRevision.equals(
                CancellationTrustInstallationPermissionPolicy.REVISION)) {
            throw new IllegalArgumentException("permissionPolicyRevision is unsupported");
        }
        ownerStableIdentity = InstallationPrincipal.boundedText(
                ownerStableIdentity, "ownerStableIdentity");
        allowedEffectiveAccess = copy(allowedEffectiveAccess, "allowedEffectiveAccess");
        deniedEffectiveAccess = copy(deniedEffectiveAccess, "deniedEffectiveAccess");
        for (InstallationPrincipalRole role : InstallationPrincipalRole.values()) {
            Set<InstallationAccess> allowed = allowedEffectiveAccess.get(role);
            Set<InstallationAccess> denied = deniedEffectiveAccess.get(role);
            if (!Collections.disjoint(allowed, denied)) {
                throw new IllegalArgumentException("allowed and denied access must be disjoint");
            }
            EnumSet<InstallationAccess> complete = EnumSet.noneOf(InstallationAccess.class);
            complete.addAll(allowed);
            complete.addAll(denied);
            if (!complete.equals(EnumSet.allOf(InstallationAccess.class))) {
                throw new IllegalArgumentException("access evidence must be complete");
            }
            if (!allowed.equals(CancellationTrustInstallationPermissionPolicy
                    .rule(artifact.kind(), role).allowed())) {
                throw new IllegalArgumentException("access evidence differs from fixed policy");
            }
        }
    }

    private static Map<InstallationPrincipalRole, Set<InstallationAccess>> copy(
            Map<InstallationPrincipalRole, Set<InstallationAccess>> source,
            String name) {
        Map<InstallationPrincipalRole, Set<InstallationAccess>> checked =
                Objects.requireNonNull(source, name + " must not be null");
        if (!checked.keySet().equals(EnumSet.allOf(InstallationPrincipalRole.class))) {
            throw new IllegalArgumentException(name + " must contain every role");
        }
        Map<InstallationPrincipalRole, Set<InstallationAccess>> result =
                new EnumMap<>(InstallationPrincipalRole.class);
        for (InstallationPrincipalRole role : InstallationPrincipalRole.values()) {
            Set<InstallationAccess> accesses = Objects.requireNonNull(
                    checked.get(role), name + " entry must not be null");
            result.put(role, accesses.isEmpty()
                    ? Set.of()
                    : Collections.unmodifiableSet(EnumSet.copyOf(accesses)));
        }
        return Map.copyOf(result);
    }
}
