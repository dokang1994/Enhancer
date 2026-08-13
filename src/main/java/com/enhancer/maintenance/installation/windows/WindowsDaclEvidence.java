package com.enhancer.maintenance.installation.windows;

import java.util.Objects;

/** Structural DACL evidence; the adapter separately enforces the exact safe profile. */
public record WindowsDaclEvidence(
        WindowsSid owner,
        boolean present,
        boolean nullAcl,
        boolean protectedAcl,
        int inheritedAceCount,
        boolean canonicalOrder) {
    public WindowsDaclEvidence {
        owner = Objects.requireNonNull(owner, "owner must not be null");
        if (inheritedAceCount < 0 || inheritedAceCount > 4096) {
            throw new IllegalArgumentException("inheritedAceCount is outside supported bounds");
        }
    }
}
