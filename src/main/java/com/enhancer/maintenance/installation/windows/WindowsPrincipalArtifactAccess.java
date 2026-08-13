package com.enhancer.maintenance.installation.windows;

import com.enhancer.maintenance.installation.InstallationAccess;
import com.enhancer.maintenance.installation.InstallationPrincipalRole;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Raw Windows rights plus their complete normalized transaction-operation partition. */
public record WindowsPrincipalArtifactAccess(
        InstallationPrincipalRole role,
        WindowsRawAccessPartition targetRights,
        WindowsRawAccessPartition parentRights,
        Set<InstallationAccess> normalizedAllowed,
        Set<InstallationAccess> normalizedDenied) {
    public WindowsPrincipalArtifactAccess {
        role = Objects.requireNonNull(role, "role must not be null");
        targetRights = Objects.requireNonNull(targetRights, "targetRights must not be null");
        parentRights = Objects.requireNonNull(parentRights, "parentRights must not be null");
        normalizedAllowed = copy(normalizedAllowed, "normalizedAllowed");
        normalizedDenied = copy(normalizedDenied, "normalizedDenied");
        if (!Collections.disjoint(normalizedAllowed, normalizedDenied)) {
            throw new IllegalArgumentException("normalized partitions must be disjoint");
        }
        EnumSet<InstallationAccess> complete = EnumSet.noneOf(InstallationAccess.class);
        complete.addAll(normalizedAllowed);
        complete.addAll(normalizedDenied);
        if (!complete.equals(EnumSet.allOf(InstallationAccess.class))) {
            throw new IllegalArgumentException("normalized partition must be complete");
        }
    }

    private static Set<InstallationAccess> copy(Set<InstallationAccess> source, String name) {
        Set<InstallationAccess> checked = Objects.requireNonNull(
                source, name + " must not be null");
        return checked.isEmpty() ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(checked));
    }
}
