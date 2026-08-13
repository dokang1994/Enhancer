package com.enhancer.maintenance.installation.windows;

import com.enhancer.maintenance.installation.InstallationArtifactKind;
import com.enhancer.maintenance.installation.InstallationPrincipalRole;
import com.enhancer.maintenance.installation.InstallationPrincipalSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Complete platform-neutral-input binding produced by an injected Windows gateway. */
public record WindowsEnvironmentSnapshot(
        UUID transactionId,
        InstallationPrincipalSet principals,
        Map<InstallationPrincipalRole, WindowsPrincipalTokenEvidence> tokens,
        WindowsVolumeIdentity volume,
        Map<InstallationArtifactKind, List<WindowsPathComponentEvidence>> paths,
        String adapterId,
        String adapterVersion) {
    public WindowsEnvironmentSnapshot {
        transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        principals = Objects.requireNonNull(principals, "principals must not be null");
        tokens = copyTokens(tokens);
        volume = Objects.requireNonNull(volume, "volume must not be null");
        paths = copyPaths(paths);
        adapterId = boundedText(adapterId, "adapterId");
        adapterVersion = boundedText(adapterVersion, "adapterVersion");
    }

    private static Map<InstallationPrincipalRole, WindowsPrincipalTokenEvidence> copyTokens(
            Map<InstallationPrincipalRole, WindowsPrincipalTokenEvidence> source) {
        Map<InstallationPrincipalRole, WindowsPrincipalTokenEvidence> checked =
                Objects.requireNonNull(source, "tokens must not be null");
        if (!checked.keySet().equals(EnumSet.allOf(InstallationPrincipalRole.class))) {
            throw new IllegalArgumentException("tokens must contain every role");
        }
        EnumMap<InstallationPrincipalRole, WindowsPrincipalTokenEvidence> result =
                new EnumMap<>(InstallationPrincipalRole.class);
        for (InstallationPrincipalRole role : InstallationPrincipalRole.values()) {
            WindowsPrincipalTokenEvidence token = Objects.requireNonNull(
                    checked.get(role), "token must not be null");
            if (token.role() != role) {
                throw new IllegalArgumentException("token role differs from key");
            }
            result.put(role, token);
        }
        return Map.copyOf(result);
    }

    private static Map<InstallationArtifactKind, List<WindowsPathComponentEvidence>> copyPaths(
            Map<InstallationArtifactKind, List<WindowsPathComponentEvidence>> source) {
        Map<InstallationArtifactKind, List<WindowsPathComponentEvidence>> checked =
                Objects.requireNonNull(source, "paths must not be null");
        if (!checked.keySet().equals(EnumSet.allOf(InstallationArtifactKind.class))) {
            throw new IllegalArgumentException("paths must contain every artifact kind");
        }
        EnumMap<InstallationArtifactKind, List<WindowsPathComponentEvidence>> result =
                new EnumMap<>(InstallationArtifactKind.class);
        for (InstallationArtifactKind kind : InstallationArtifactKind.values()) {
            List<WindowsPathComponentEvidence> components = List.copyOf(
                    Objects.requireNonNull(checked.get(kind), "components must not be null"));
            if (components.isEmpty()) {
                throw new IllegalArgumentException("every artifact requires path evidence");
            }
            if (components.size() > 256) {
                throw new IllegalArgumentException("path evidence exceeds supported bounds");
            }
            result.put(kind, components);
        }
        return Map.copyOf(result);
    }

    private static String boundedText(String source, String name) {
        String checked = Objects.requireNonNull(source, name + " must not be null");
        if (checked.isBlank() || checked.length() > 256) {
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
