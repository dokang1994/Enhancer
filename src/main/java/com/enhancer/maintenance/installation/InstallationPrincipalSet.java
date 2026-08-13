package com.enhancer.maintenance.installation;

import java.util.Objects;
import java.util.Set;

/** Exact pairwise-distinct identities for publisher, operator, and runtime. */
public record InstallationPrincipalSet(
        InstallationPrincipal installerPublisher,
        InstallationPrincipal operator,
        InstallationPrincipal runtime) {

    public InstallationPrincipalSet {
        installerPublisher = requireRole(installerPublisher,
                InstallationPrincipalRole.INSTALLER_PUBLISHER, "installerPublisher");
        operator = requireRole(operator, InstallationPrincipalRole.OPERATOR, "operator");
        runtime = requireRole(runtime, InstallationPrincipalRole.RUNTIME, "runtime");
        Set<String> identities = Set.of(
                installerPublisher.stableOperatingSystemIdentity(),
                operator.stableOperatingSystemIdentity(),
                runtime.stableOperatingSystemIdentity());
        if (identities.size() != InstallationPrincipalRole.values().length) {
            throw new IllegalArgumentException("principal identities must be pairwise distinct");
        }
    }

    private static InstallationPrincipal requireRole(
            InstallationPrincipal principal,
            InstallationPrincipalRole role,
            String name) {
        InstallationPrincipal checked = Objects.requireNonNull(
                principal, name + " must not be null");
        if (checked.role() != role) {
            throw new IllegalArgumentException(name + " has the wrong role");
        }
        return checked;
    }
}
