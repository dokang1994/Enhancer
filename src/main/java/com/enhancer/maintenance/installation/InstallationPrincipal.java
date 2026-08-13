package com.enhancer.maintenance.installation;

import java.util.Objects;

/** Opaque stable operating-system identity assigned to one fixed installation role. */
public record InstallationPrincipal(
        InstallationPrincipalRole role,
        String stableOperatingSystemIdentity) {
    private static final int MAXIMUM_IDENTITY_CHARACTERS = 256;

    public InstallationPrincipal {
        role = Objects.requireNonNull(role, "role must not be null");
        stableOperatingSystemIdentity = boundedText(
                stableOperatingSystemIdentity, "stableOperatingSystemIdentity");
    }

    static String boundedText(String value, String name) {
        String checked = Objects.requireNonNull(value, name + " must not be null");
        if (checked.isBlank() || checked.length() > MAXIMUM_IDENTITY_CHARACTERS) {
            throw new IllegalArgumentException(name + " is outside supported bounds");
        }
        for (int index = 0; index < checked.length(); index++) {
            if (Character.isISOControl(checked.charAt(index))) {
                throw new IllegalArgumentException(name + " must not contain controls");
            }
        }
        return checked;
    }
}
