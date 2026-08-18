package com.enhancer.maintenance.installation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Pure immutable transaction cursor; it performs and proves no installation effect. */
public record InstallationTransactionState(
        int schemaVersion,
        CancellationTrustInstallationPlan plan,
        InstallationEnvironmentEvidence environment,
        String sourceReleaseVersion,
        String permissionPolicySha256,
        Optional<String> expectedCurrentActivationIdentity,
        String requestedActivationIdentity,
        List<InstallationPhaseEvidence> succeededPhaseEvidencePrefix,
        long revision,
        InstallationPhase phase,
        StepStatus stepStatus) {
    public static final int SCHEMA_VERSION = 2;
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final List<InstallationPhase> ORDER = InstallationPhase.requiredOrder();

    public InstallationTransactionState {
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException("schemaVersion is unsupported");
        }
        plan = Objects.requireNonNull(plan, "plan must not be null");
        environment = Objects.requireNonNull(environment, "environment must not be null");
        if (!plan.transactionId().equals(environment.transactionId())) {
            throw new IllegalArgumentException("environment transaction identity must match plan");
        }
        if (!plan.principals().equals(environment.resolvedPrincipals())) {
            throw new IllegalArgumentException("resolved principals must match plan");
        }
        sourceReleaseVersion = InstallationPrincipal.boundedText(
                sourceReleaseVersion, "sourceReleaseVersion");
        permissionPolicySha256 = digest(permissionPolicySha256, "permissionPolicySha256");
        expectedCurrentActivationIdentity = Objects.requireNonNull(
                expectedCurrentActivationIdentity,
                "expectedCurrentActivationIdentity must not be null");
        expectedCurrentActivationIdentity = expectedCurrentActivationIdentity.map(value ->
                InstallationPrincipal.boundedText(value, "expectedCurrentActivationIdentity"));
        requestedActivationIdentity = InstallationPrincipal.boundedText(
                requestedActivationIdentity, "requestedActivationIdentity");
        succeededPhaseEvidencePrefix = List.copyOf(Objects.requireNonNull(
                succeededPhaseEvidencePrefix,
                "succeededPhaseEvidencePrefix must not be null"));
        if (plan.operation() == InstallationOperation.INSTALL
                && expectedCurrentActivationIdentity.isPresent()) {
            throw new IllegalArgumentException(
                    "INSTALL cannot have expected current activation identity");
        }
        if (plan.operation() == InstallationOperation.ROTATE
                && expectedCurrentActivationIdentity.isEmpty()) {
            throw new IllegalArgumentException(
                    "ROTATE requires expected current activation identity");
        }
        phase = Objects.requireNonNull(phase, "phase must not be null");
        stepStatus = Objects.requireNonNull(stepStatus, "stepStatus must not be null");
        int phaseIndex = ORDER.indexOf(phase);
        if (phaseIndex < 0) {
            throw new IllegalArgumentException("phase is not in the required order");
        }
        long expectedRevision = (long) phaseIndex * 2
                + (stepStatus == StepStatus.SUCCEEDED ? 1 : 0);
        if (revision != expectedRevision) {
            throw new IllegalArgumentException(
                    "revision must exactly match the ordered phase status");
        }
        int expectedEvidenceCount = phaseIndex
                + (stepStatus == StepStatus.SUCCEEDED ? 1 : 0);
        if (succeededPhaseEvidencePrefix.size() != expectedEvidenceCount) {
            throw new IllegalArgumentException(
                    "evidence prefix must exactly match the ordered phase status");
        }
        for (int index = 0; index < succeededPhaseEvidencePrefix.size(); index++) {
            InstallationPhaseEvidence evidence = Objects.requireNonNull(
                    succeededPhaseEvidencePrefix.get(index),
                    "evidence prefix must not contain null");
            InstallationPhase expectedPhase = ORDER.get(index);
            if (!plan.transactionId().equals(evidence.transactionId())
                    || evidence.phase() != expectedPhase
                    || evidence.pendingRevision() != (long) index * 2) {
                throw new IllegalArgumentException(
                        "evidence prefix must bind the exact transaction phase order");
            }
            boolean activationPhase = expectedPhase == InstallationPhase.ACTIVATE;
            boolean activationMatches = activationPhase
                    ? evidence.observedActivationIdentity().equals(
                            Optional.of(requestedActivationIdentity))
                    : evidence.observedActivationIdentity().isEmpty();
            if (!activationMatches) {
                throw new IllegalArgumentException(
                        "evidence activation identity must match the exact phase binding");
            }
        }
    }

    public static InstallationTransactionState start(
            CancellationTrustInstallationPlan plan,
            InstallationEnvironmentEvidence environment,
            String sourceReleaseVersion,
            String permissionPolicySha256,
            Optional<String> expectedCurrentActivationIdentity,
            String requestedActivationIdentity) {
        return new InstallationTransactionState(
                SCHEMA_VERSION,
                plan,
                environment,
                sourceReleaseVersion,
                permissionPolicySha256,
                expectedCurrentActivationIdentity,
                requestedActivationIdentity,
                List.of(),
                0,
                ORDER.get(0),
                StepStatus.PENDING);
    }

    public InstallationTransactionState markSucceeded(InstallationPhaseEvidence evidence) {
        if (stepStatus != StepStatus.PENDING) {
            throw new IllegalStateException("only a pending phase can be marked succeeded");
        }
        List<InstallationPhaseEvidence> nextPrefix = new java.util.ArrayList<>(
                succeededPhaseEvidencePrefix);
        nextPrefix.add(Objects.requireNonNull(evidence, "evidence must not be null"));
        return with(nextPrefix, revision + 1, phase, StepStatus.SUCCEEDED);
    }

    public InstallationTransactionState beginNext() {
        if (stepStatus != StepStatus.SUCCEEDED) {
            throw new IllegalStateException("the current phase must succeed before advancing");
        }
        int nextIndex = ORDER.indexOf(phase) + 1;
        if (nextIndex >= ORDER.size()) {
            throw new IllegalStateException("the final phase has no successor");
        }
        return with(succeededPhaseEvidencePrefix, revision + 1,
                ORDER.get(nextIndex), StepStatus.PENDING);
    }

    /** Exact transition predicate for point-store implementations; it performs no effect. */
    public boolean isImmediateSuccessor(InstallationTransactionState candidate) {
        if (candidate == null) {
            return false;
        }
        try {
            if (stepStatus == StepStatus.PENDING) {
                if (candidate.succeededPhaseEvidencePrefix().size()
                        != succeededPhaseEvidencePrefix.size() + 1) {
                    return false;
                }
                InstallationPhaseEvidence appended = candidate
                        .succeededPhaseEvidencePrefix()
                        .get(candidate.succeededPhaseEvidencePrefix().size() - 1);
                return markSucceeded(appended).equals(candidate);
            }
            return !isTerminalRecord() && beginNext().equals(candidate);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return false;
        }
    }

    public boolean requiresReconciliation() {
        return stepStatus == StepStatus.PENDING;
    }

    /** A terminal record is still not installation-success or durability evidence. */
    public boolean isTerminalRecord() {
        return phase == InstallationPhase.RECORD_FINAL_EVIDENCE
                && stepStatus == StepStatus.SUCCEEDED;
    }

    public RecoveryRegion recoveryRegion() {
        if (beforeSuccessful(InstallationPhase.PUBLISH_METADATA)) {
            return RecoveryRegion.BEFORE_FINAL_METADATA;
        }
        if (beforeSuccessful(InstallationPhase.ACTIVATE)) {
            return RecoveryRegion.AFTER_METADATA_BEFORE_ACTIVATION;
        }
        return RecoveryRegion.AFTER_ACTIVATION_EXACT_REPLAY;
    }

    private boolean beforeSuccessful(InstallationPhase boundary) {
        int currentIndex = ORDER.indexOf(phase);
        int boundaryIndex = ORDER.indexOf(boundary);
        return currentIndex < boundaryIndex
                || (currentIndex == boundaryIndex && stepStatus == StepStatus.PENDING);
    }

    private InstallationTransactionState with(
            List<InstallationPhaseEvidence> nextPrefix,
            long nextRevision,
            InstallationPhase nextPhase,
            StepStatus nextStatus) {
        return new InstallationTransactionState(
                schemaVersion,
                plan,
                environment,
                sourceReleaseVersion,
                permissionPolicySha256,
                expectedCurrentActivationIdentity,
                requestedActivationIdentity,
                nextPrefix,
                nextRevision,
                nextPhase,
                nextStatus);
    }

    private static String digest(String value, String name) {
        String checked = Objects.requireNonNull(value, name + " must not be null");
        if (!SHA256.matcher(checked).matches()) {
            throw new IllegalArgumentException(name + " must be lowercase SHA-256");
        }
        return checked;
    }

    public enum StepStatus {
        PENDING,
        SUCCEEDED
    }

    public enum RecoveryRegion {
        BEFORE_FINAL_METADATA,
        AFTER_METADATA_BEFORE_ACTIVATION,
        AFTER_ACTIVATION_EXACT_REPLAY
    }
}
