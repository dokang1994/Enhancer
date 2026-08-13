package com.enhancer.maintenance.installation;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Pure already-authorized plan identity; it performs no installation operation. */
public record CancellationTrustInstallationPlan(
        UUID transactionId,
        InstallationOperation operation,
        InstallationPrincipalSet principals,
        Path installationRoot,
        Path applicationJar,
        Path runtimeDistributionRoot,
        Path operatorDistributionRoot,
        Path operatorCandidateInbox,
        Path activationPoint,
        Path auditRoot,
        String sourceManifestSha256,
        String applicationJarSha256,
        String runtimeDistributionSha256,
        String operatorDistributionSha256,
        String permissionPolicyRevision,
        String policySha256,
        String requestedMetadataSha256,
        Optional<String> expectedCurrentMetadataSha256) {
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final String METADATA_FILE = "enhancer-cancellation-trust-metadata-v1";
    private static final String TRUST_DIRECTORY = "enhancer-cancellation-trust-policies-v1";
    private static final String LOCK_FILE = "enhancer-cancellation-trust-maintenance-v1.lock";
    private static final String POLICY_PREFIX = "enhancer-cancellation-trust-policy-";
    private static final String POLICY_SUFFIX = ".conf";

    public CancellationTrustInstallationPlan {
        transactionId = Objects.requireNonNull(transactionId, "transactionId must not be null");
        operation = Objects.requireNonNull(operation, "operation must not be null");
        principals = Objects.requireNonNull(principals, "principals must not be null");
        installationRoot = InstallationArtifact.exactPath(installationRoot, "installationRoot");
        applicationJar = InstallationArtifact.exactPath(applicationJar, "applicationJar");
        runtimeDistributionRoot = InstallationArtifact.exactPath(
                runtimeDistributionRoot, "runtimeDistributionRoot");
        operatorDistributionRoot = InstallationArtifact.exactPath(
                operatorDistributionRoot, "operatorDistributionRoot");
        operatorCandidateInbox = InstallationArtifact.exactPath(
                operatorCandidateInbox, "operatorCandidateInbox");
        activationPoint = InstallationArtifact.exactPath(activationPoint, "activationPoint");
        auditRoot = InstallationArtifact.exactPath(auditRoot, "auditRoot");
        if (!applicationJar.getFileName().toString().endsWith(".jar")) {
            throw new IllegalArgumentException("applicationJar must have a .jar name");
        }
        for (Path path : List.of(applicationJar, runtimeDistributionRoot,
                operatorDistributionRoot, operatorCandidateInbox, activationPoint, auditRoot)) {
            if (!path.startsWith(installationRoot)) {
                throw new IllegalArgumentException("all plan paths must be below installationRoot");
            }
        }
        if (!applicationJar.startsWith(runtimeDistributionRoot)) {
            throw new IllegalArgumentException("applicationJar must be below runtimeDistributionRoot");
        }
        if (overlaps(runtimeDistributionRoot, operatorDistributionRoot)) {
            throw new IllegalArgumentException("runtime and operator roots must not overlap");
        }
        sourceManifestSha256 = digest(sourceManifestSha256, "sourceManifestSha256");
        applicationJarSha256 = digest(applicationJarSha256, "applicationJarSha256");
        runtimeDistributionSha256 = digest(runtimeDistributionSha256,
                "runtimeDistributionSha256");
        operatorDistributionSha256 = digest(operatorDistributionSha256,
                "operatorDistributionSha256");
        permissionPolicyRevision = Objects.requireNonNull(
                permissionPolicyRevision, "permissionPolicyRevision must not be null");
        if (!permissionPolicyRevision.equals(
                CancellationTrustInstallationPermissionPolicy.REVISION)) {
            throw new IllegalArgumentException("permissionPolicyRevision is unsupported");
        }
        policySha256 = digest(policySha256, "policySha256");
        requestedMetadataSha256 = digest(
                requestedMetadataSha256, "requestedMetadataSha256");
        expectedCurrentMetadataSha256 = Objects.requireNonNull(
                expectedCurrentMetadataSha256,
                "expectedCurrentMetadataSha256 must not be null");
        expectedCurrentMetadataSha256 = expectedCurrentMetadataSha256
                .map(value -> digest(value, "expectedCurrentMetadataSha256"));
        if (operation == InstallationOperation.INSTALL
                && expectedCurrentMetadataSha256.isPresent()) {
            throw new IllegalArgumentException("INSTALL cannot have expected current metadata");
        }
        if (operation == InstallationOperation.ROTATE
                && expectedCurrentMetadataSha256.isEmpty()) {
            throw new IllegalArgumentException("ROTATE requires expected current metadata");
        }
        requireDistinctArtifactPaths(
                transactionId,
                installationRoot,
                applicationJar,
                runtimeDistributionRoot,
                operatorDistributionRoot,
                operatorCandidateInbox,
                activationPoint,
                auditRoot,
                policySha256);
    }

    public List<InstallationArtifact> artifacts() {
        Map<InstallationArtifactKind, InstallationArtifact> artifacts = artifactMap();
        return java.util.Arrays.stream(InstallationArtifactKind.values())
                .map(artifacts::get)
                .toList();
    }

    public InstallationArtifact artifact(InstallationArtifactKind kind) {
        return artifactMap().get(Objects.requireNonNull(kind, "kind must not be null"));
    }

    public List<InstallationPhase> requiredOrder() {
        return InstallationPhase.requiredOrder();
    }

    private Map<InstallationArtifactKind, InstallationArtifact> artifactMap() {
        Path installationDirectory = applicationJar.getParent();
        Path trustDirectory = installationDirectory.resolve(TRUST_DIRECTORY);
        Map<InstallationArtifactKind, InstallationArtifact> result =
                new EnumMap<>(InstallationArtifactKind.class);
        put(result, InstallationArtifactKind.INSTALLATION_ANCESTOR, installationRoot);
        put(result, InstallationArtifactKind.APPLICATION_JAR, applicationJar);
        put(result, InstallationArtifactKind.RUNTIME_DISTRIBUTION, runtimeDistributionRoot);
        put(result, InstallationArtifactKind.OPERATOR_DISTRIBUTION, operatorDistributionRoot);
        put(result, InstallationArtifactKind.FIXED_METADATA,
                installationDirectory.resolve(METADATA_FILE));
        put(result, InstallationArtifactKind.TRUST_DIRECTORY, trustDirectory);
        put(result, InstallationArtifactKind.CONTENT_ADDRESSED_POLICY,
                trustDirectory.resolve(POLICY_PREFIX + policySha256 + POLICY_SUFFIX));
        put(result, InstallationArtifactKind.MAINTENANCE_LOCK,
                installationDirectory.resolve(LOCK_FILE));
        put(result, InstallationArtifactKind.POLICY_CANDIDATE,
                trustDirectory.resolve(".policy-candidate-" + transactionId + ".tmp"));
        put(result, InstallationArtifactKind.METADATA_CANDIDATE,
                installationDirectory.resolve(".metadata-candidate-" + transactionId + ".tmp"));
        put(result, InstallationArtifactKind.OPERATOR_CANDIDATE_INBOX, operatorCandidateInbox);
        put(result, InstallationArtifactKind.ACTIVATION_POINT, activationPoint);
        put(result, InstallationArtifactKind.INSTALLATION_AUDIT_ROOT, auditRoot);
        return Map.copyOf(result);
    }

    private static void requireDistinctArtifactPaths(
            UUID transactionId,
            Path installationRoot,
            Path applicationJar,
            Path runtimeDistributionRoot,
            Path operatorDistributionRoot,
            Path operatorCandidateInbox,
            Path activationPoint,
            Path auditRoot,
            String policySha256) {
        Path installationDirectory = applicationJar.getParent();
        Path trustDirectory = installationDirectory.resolve(TRUST_DIRECTORY);
        List<Path> paths = List.of(
                installationRoot,
                applicationJar,
                runtimeDistributionRoot,
                operatorDistributionRoot,
                installationDirectory.resolve(METADATA_FILE),
                trustDirectory,
                trustDirectory.resolve(POLICY_PREFIX + policySha256 + POLICY_SUFFIX),
                installationDirectory.resolve(LOCK_FILE),
                trustDirectory.resolve(".policy-candidate-" + transactionId + ".tmp"),
                installationDirectory.resolve(".metadata-candidate-" + transactionId + ".tmp"),
                operatorCandidateInbox,
                activationPoint,
                auditRoot);
        if (paths.stream().distinct().count() != InstallationArtifactKind.values().length) {
            throw new IllegalArgumentException("planned artifact paths must be distinct");
        }
    }

    private static void put(
            Map<InstallationArtifactKind, InstallationArtifact> result,
            InstallationArtifactKind kind,
            Path path) {
        result.put(kind, new InstallationArtifact(kind, path));
    }

    private static String digest(String value, String name) {
        String checked = Objects.requireNonNull(value, name + " must not be null");
        if (!SHA256.matcher(checked).matches()) {
            throw new IllegalArgumentException(name + " must be lowercase SHA-256");
        }
        return checked;
    }

    private static boolean overlaps(Path first, Path second) {
        return first.startsWith(second) || second.startsWith(first);
    }
}
