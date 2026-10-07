package com.enhancer.model;

import java.util.Objects;

/** Opaque ephemeral RFC-0029 selection retaining every exact selected input identity. */
public final class SelectedModelRoute {

    public static final String CONTRACT_VERSION = "model-router-v1";

    private final String contractVersion;
    private final ProfiledModelRequest profiledRequest;
    private final ModelRouteOption selectedOption;
    private final ModelOutboundRouteAdmissionDecision.Eligible eligibility;
    private final RemoteModelCandidateSuitability suitability;
    private final ModelRouteCandidateFacts candidateFacts;
    private final ModelDestinationPolicyFacts destinationFacts;
    private final int optionOrdinal;

    private SelectedModelRoute(
            String contractVersion,
            ProfiledModelRequest profiledRequest,
            ModelRouteOption selectedOption,
            ModelOutboundRouteAdmissionDecision.Eligible eligibility,
            RemoteModelCandidateSuitability suitability,
            ModelRouteCandidateFacts candidateFacts,
            ModelDestinationPolicyFacts destinationFacts,
            int optionOrdinal) {
        this.contractVersion = Objects.requireNonNull(
                contractVersion, "contractVersion must not be null");
        this.profiledRequest = Objects.requireNonNull(
                profiledRequest, "profiledRequest must not be null");
        this.selectedOption = Objects.requireNonNull(
                selectedOption, "selectedOption must not be null");
        this.eligibility = Objects.requireNonNull(
                eligibility, "eligibility must not be null");
        this.suitability = Objects.requireNonNull(
                suitability, "suitability must not be null");
        this.candidateFacts = Objects.requireNonNull(
                candidateFacts, "candidateFacts must not be null");
        this.destinationFacts = Objects.requireNonNull(
                destinationFacts, "destinationFacts must not be null");
        if (optionOrdinal != 0) {
            throw new IllegalArgumentException("the first Router selects only ordinal zero");
        }
        this.optionOrdinal = optionOrdinal;
    }

    static SelectedModelRoute create(
            ProfiledModelRequest profiledRequest,
            ModelRouteOption selectedOption,
            ModelOutboundRouteAdmissionDecision.Eligible eligibility,
            RemoteModelCandidateSuitability suitability,
            ModelRouteCandidateFacts candidateFacts,
            ModelDestinationPolicyFacts destinationFacts,
            int optionOrdinal) {
        return new SelectedModelRoute(
                CONTRACT_VERSION,
                profiledRequest,
                selectedOption,
                eligibility,
                suitability,
                candidateFacts,
                destinationFacts,
                optionOrdinal);
    }

    public String contractVersion() {
        return contractVersion;
    }

    public ProfiledModelRequest profiledRequest() {
        return profiledRequest;
    }

    public ModelRouteOption selectedOption() {
        return selectedOption;
    }

    public ModelOutboundRouteAdmissionDecision.Eligible eligibility() {
        return eligibility;
    }

    public RemoteModelCandidateSuitability suitability() {
        return suitability;
    }

    public ModelRouteCandidateFacts candidateFacts() {
        return candidateFacts;
    }

    public ModelDestinationPolicyFacts destinationFacts() {
        return destinationFacts;
    }

    public int optionOrdinal() {
        return optionOrdinal;
    }

    @Override
    public String toString() {
        return "SelectedModelRoute[contractVersion=" + contractVersion
                + ", optionOrdinal=" + optionOrdinal
                + ", suitabilitySourceIdentity="
                + suitability.suitabilitySourceIdentity() + "]";
    }
}
