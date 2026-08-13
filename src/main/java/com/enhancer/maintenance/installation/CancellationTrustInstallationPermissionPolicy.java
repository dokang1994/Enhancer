package com.enhancer.maintenance.installation;

import static com.enhancer.maintenance.installation.InstallationAccess.CHANGE_OWNER;
import static com.enhancer.maintenance.installation.InstallationAccess.CHANGE_PERMISSIONS;
import static com.enhancer.maintenance.installation.InstallationAccess.CREATE;
import static com.enhancer.maintenance.installation.InstallationAccess.DELETE;
import static com.enhancer.maintenance.installation.InstallationAccess.DELETE_CHILD;
import static com.enhancer.maintenance.installation.InstallationAccess.EXECUTE;
import static com.enhancer.maintenance.installation.InstallationAccess.READ;
import static com.enhancer.maintenance.installation.InstallationAccess.RENAME;
import static com.enhancer.maintenance.installation.InstallationAccess.REPLACE;
import static com.enhancer.maintenance.installation.InstallationAccess.TRAVERSE;
import static com.enhancer.maintenance.installation.InstallationAccess.WRITE;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Fixed v1 permission matrix. It describes enforcement and grants no authorization. */
public final class CancellationTrustInstallationPermissionPolicy {
    public static final String REVISION = "cancellation-trust-installation-permissions-v1";
    private static final Map<InstallationArtifactKind,
            Map<InstallationPrincipalRole, InstallationPermissionRule>> MATRIX = matrix();
    private static final List<InstallationPermissionRule> RULES = rules(MATRIX);

    private CancellationTrustInstallationPermissionPolicy() { }

    public static InstallationPermissionRule rule(
            InstallationArtifactKind artifactKind,
            InstallationPrincipalRole principalRole) {
        return MATRIX.get(Objects.requireNonNull(artifactKind, "artifactKind must not be null"))
                .get(Objects.requireNonNull(principalRole, "principalRole must not be null"));
    }

    public static List<InstallationPermissionRule> rules() {
        return RULES;
    }

    private static Map<InstallationArtifactKind,
            Map<InstallationPrincipalRole, InstallationPermissionRule>> matrix() {
        Map<InstallationArtifactKind, Map<InstallationPrincipalRole,
                InstallationPermissionRule>> result = new EnumMap<>(InstallationArtifactKind.class);
        for (InstallationArtifactKind kind : InstallationArtifactKind.values()) {
            Map<InstallationPrincipalRole, InstallationPermissionRule> roles =
                    new EnumMap<>(InstallationPrincipalRole.class);
            for (InstallationPrincipalRole role : InstallationPrincipalRole.values()) {
                roles.put(role, new InstallationPermissionRule(kind, role, allowed(kind, role)));
            }
            result.put(kind, Map.copyOf(roles));
        }
        return Map.copyOf(result);
    }

    private static List<InstallationPermissionRule> rules(
            Map<InstallationArtifactKind,
                    Map<InstallationPrincipalRole, InstallationPermissionRule>> matrix) {
        List<InstallationPermissionRule> result = new ArrayList<>();
        for (InstallationArtifactKind kind : InstallationArtifactKind.values()) {
            for (InstallationPrincipalRole role : InstallationPrincipalRole.values()) {
                result.add(matrix.get(kind).get(role));
            }
        }
        return List.copyOf(result);
    }

    private static Set<InstallationAccess> allowed(
            InstallationArtifactKind kind, InstallationPrincipalRole role) {
        if (role == InstallationPrincipalRole.INSTALLER_PUBLISHER) {
            return publisher(kind);
        }
        if (role == InstallationPrincipalRole.OPERATOR) {
            return operator(kind);
        }
        return runtime(kind);
    }

    private static Set<InstallationAccess> publisher(InstallationArtifactKind kind) {
        return switch (kind) {
            case CONTENT_ADDRESSED_POLICY -> set(READ, CREATE);
            case FIXED_METADATA -> set(READ, CREATE, REPLACE, RENAME);
            case MAINTENANCE_LOCK, POLICY_CANDIDATE, METADATA_CANDIDATE ->
                    set(READ, CREATE, WRITE, RENAME);
            case OPERATOR_CANDIDATE_INBOX -> set(READ, TRAVERSE);
            default -> set(READ, EXECUTE, TRAVERSE, CREATE, WRITE, REPLACE, RENAME,
                    DELETE_CHILD, CHANGE_OWNER, CHANGE_PERMISSIONS);
        };
    }

    private static Set<InstallationAccess> operator(InstallationArtifactKind kind) {
        return switch (kind) {
            case INSTALLATION_ANCESTOR -> set(TRAVERSE);
            case APPLICATION_JAR, FIXED_METADATA, CONTENT_ADDRESSED_POLICY -> set(READ);
            case OPERATOR_DISTRIBUTION -> set(READ, EXECUTE, TRAVERSE);
            case TRUST_DIRECTORY -> set(READ, TRAVERSE);
            case OPERATOR_CANDIDATE_INBOX -> set(READ, TRAVERSE, CREATE, WRITE);
            case ACTIVATION_POINT -> set(READ);
            default -> Set.of();
        };
    }

    private static Set<InstallationAccess> runtime(InstallationArtifactKind kind) {
        return switch (kind) {
            case INSTALLATION_ANCESTOR -> set(READ, TRAVERSE);
            case APPLICATION_JAR, RUNTIME_DISTRIBUTION -> set(READ, EXECUTE, TRAVERSE);
            case FIXED_METADATA, CONTENT_ADDRESSED_POLICY -> set(READ);
            case TRUST_DIRECTORY -> set(READ, TRAVERSE);
            case ACTIVATION_POINT -> set(READ, EXECUTE, TRAVERSE);
            default -> Set.of();
        };
    }

    private static Set<InstallationAccess> set(
            InstallationAccess first, InstallationAccess... rest) {
        return Set.copyOf(EnumSet.of(first, rest));
    }
}
