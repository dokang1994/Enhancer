package com.enhancer.maintenance.installation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Immutable effective-access allow rule for one artifact kind and principal role. */
public record InstallationPermissionRule(
        InstallationArtifactKind artifactKind,
        InstallationPrincipalRole principalRole,
        Set<InstallationAccess> allowed) {

    public InstallationPermissionRule {
        artifactKind = Objects.requireNonNull(artifactKind, "artifactKind must not be null");
        principalRole = Objects.requireNonNull(principalRole, "principalRole must not be null");
        Set<InstallationAccess> checked = Objects.requireNonNull(
                allowed, "allowed must not be null");
        allowed = checked.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(checked));
    }
}
