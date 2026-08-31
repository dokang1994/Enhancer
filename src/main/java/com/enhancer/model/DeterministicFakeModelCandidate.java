package com.enhancer.model;

import java.util.Objects;

/**
 * Opaque process-local binding to the repository's exact deterministic fake.
 *
 * <p>All suitability facts except the gateway identity are fixed by this class and
 * cannot be supplied or widened by a caller.
 */
public final class DeterministicFakeModelCandidate {

    private final DeterministicFakeModelGateway gateway;

    private DeterministicFakeModelCandidate(DeterministicFakeModelGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway must not be null");
    }

    public static DeterministicFakeModelCandidate bind(DeterministicFakeModelGateway gateway) {
        return new DeterministicFakeModelCandidate(gateway);
    }

    public DeterministicFakeModelGateway gateway() {
        return gateway;
    }

    public String candidateId() {
        return "deterministic-fake-v1";
    }

    public String modelClass() {
        return "deterministic-fake";
    }

    public String requiredCapability() {
        return "deterministic-echo";
    }

    public ModelReasoningRequirement maximumReasoningRequirement() {
        return ModelReasoningRequirement.MINIMAL;
    }

    public String localityProvenance() {
        return "CLOSED_IN_PROCESS_FAKE";
    }

    public boolean tokenSemanticsAvailable() {
        return false;
    }

    public boolean hasProviderCharge() {
        return false;
    }

    public ModelDataClassification maximumDataClassification() {
        return ModelDataClassification.PUBLIC;
    }
}
