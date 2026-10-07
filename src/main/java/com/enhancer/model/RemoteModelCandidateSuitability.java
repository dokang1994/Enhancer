package com.enhancer.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Opaque invocation-local proof that one exact remote candidate can satisfy one request.
 *
 * <p>Construction is package-private so only a separately governed pure evaluator, or
 * package-local contract fixture, can create a proof. This value grants no provider,
 * credential, network, transmission, persistence, or spend authority.
 */
public final class RemoteModelCandidateSuitability {

    public static final String CONTRACT_VERSION = "remote-model-candidate-suitability-v1";

    private final String contractVersion;
    private final ProfiledModelRequest profiledRequest;
    private final ModelOutboundRouteAdmissionDecision.Eligible eligibility;
    private final ModelRouteCandidateFacts candidateFacts;
    private final String suitabilitySourceIdentity;
    private final String sourceRevision;
    private final Instant observedAt;
    private final Instant expiresAt;
    private final String candidateIdentity;
    private final String destinationIdentity;
    private final String tokenSemanticsIdentity;
    private final String capacityEvidenceIdentity;
    private final String modelClass;
    private final String requiredCapability;
    private final ModelReasoningRequirement maximumReasoningRequirement;
    private final long maximumContextTokens;
    private final ModelTokenBudget maximumTokenBudget;
    private final Duration maximumInvocationTime;
    private final String costCurrencyCode;
    private final long maximumCostMicrounits;
    private final ModelDataClassification maximumDataClassification;

    private RemoteModelCandidateSuitability(
            String contractVersion,
            ProfiledModelRequest profiledRequest,
            ModelOutboundRouteAdmissionDecision.Eligible eligibility,
            ModelRouteCandidateFacts candidateFacts,
            String suitabilitySourceIdentity,
            String sourceRevision,
            Instant observedAt,
            Instant expiresAt,
            String candidateIdentity,
            String destinationIdentity,
            String tokenSemanticsIdentity,
            String capacityEvidenceIdentity,
            String modelClass,
            String requiredCapability,
            ModelReasoningRequirement maximumReasoningRequirement,
            long maximumContextTokens,
            ModelTokenBudget maximumTokenBudget,
            Duration maximumInvocationTime,
            String costCurrencyCode,
            long maximumCostMicrounits,
            ModelDataClassification maximumDataClassification) {
        this.contractVersion =
                ModelOutboundRouteValueChecks.label(contractVersion, "contractVersion");
        this.profiledRequest = Objects.requireNonNull(
                profiledRequest, "profiledRequest must not be null");
        this.eligibility = Objects.requireNonNull(
                eligibility, "eligibility must not be null");
        this.candidateFacts = Objects.requireNonNull(
                candidateFacts, "candidateFacts must not be null");
        this.suitabilitySourceIdentity = ModelOutboundRouteValueChecks.label(
                suitabilitySourceIdentity, "suitabilitySourceIdentity");
        this.sourceRevision =
                ModelOutboundRouteValueChecks.revision(sourceRevision, "sourceRevision");
        ModelOutboundRouteValueChecks.window(observedAt, expiresAt);
        this.observedAt = observedAt;
        this.expiresAt = expiresAt;
        this.candidateIdentity = ModelOutboundRouteValueChecks.label(
                candidateIdentity, "candidateIdentity");
        this.destinationIdentity = ModelOutboundRouteValueChecks.label(
                destinationIdentity, "destinationIdentity");
        this.tokenSemanticsIdentity = ModelOutboundRouteValueChecks.label(
                tokenSemanticsIdentity, "tokenSemanticsIdentity");
        this.capacityEvidenceIdentity = ModelOutboundRouteValueChecks.label(
                capacityEvidenceIdentity, "capacityEvidenceIdentity");
        this.modelClass = ModelOutboundRouteValueChecks.label(modelClass, "modelClass");
        this.requiredCapability = ModelOutboundRouteValueChecks.label(
                requiredCapability, "requiredCapability");
        this.maximumReasoningRequirement = Objects.requireNonNull(
                maximumReasoningRequirement,
                "maximumReasoningRequirement must not be null");
        if (maximumContextTokens <= 0
                || maximumContextTokens > ModelExecutionProfile.MAX_CONTEXT_TOKENS) {
            throw new IllegalArgumentException(
                    "maximumContextTokens must be positive and bounded");
        }
        this.maximumContextTokens = maximumContextTokens;
        this.maximumTokenBudget = Objects.requireNonNull(
                maximumTokenBudget, "maximumTokenBudget must not be null");
        this.maximumInvocationTime = Objects.requireNonNull(
                maximumInvocationTime, "maximumInvocationTime must not be null");
        if (maximumInvocationTime.isZero()
                || maximumInvocationTime.isNegative()
                || maximumInvocationTime.compareTo(
                        ModelExecutionProfile.MAX_INVOCATION_TIME) > 0
                || maximumInvocationTime.getNano() % 1_000_000 != 0) {
            throw new IllegalArgumentException(
                    "maximumInvocationTime must be positive, bounded, and use millisecond precision");
        }
        ModelCostBudget validatedCost =
                new ModelCostBudget(costCurrencyCode, maximumCostMicrounits);
        this.costCurrencyCode = validatedCost.currencyCode();
        this.maximumCostMicrounits = validatedCost.maxMicrounits();
        this.maximumDataClassification = Objects.requireNonNull(
                maximumDataClassification,
                "maximumDataClassification must not be null");
    }

    static RemoteModelCandidateSuitability create(
            String contractVersion,
            ProfiledModelRequest profiledRequest,
            ModelOutboundRouteAdmissionDecision.Eligible eligibility,
            ModelRouteCandidateFacts candidateFacts,
            String suitabilitySourceIdentity,
            String sourceRevision,
            Instant observedAt,
            Instant expiresAt,
            String candidateIdentity,
            String destinationIdentity,
            String tokenSemanticsIdentity,
            String capacityEvidenceIdentity,
            String modelClass,
            String requiredCapability,
            ModelReasoningRequirement maximumReasoningRequirement,
            long maximumContextTokens,
            ModelTokenBudget maximumTokenBudget,
            Duration maximumInvocationTime,
            String costCurrencyCode,
            long maximumCostMicrounits,
            ModelDataClassification maximumDataClassification) {
        return new RemoteModelCandidateSuitability(
                contractVersion,
                profiledRequest,
                eligibility,
                candidateFacts,
                suitabilitySourceIdentity,
                sourceRevision,
                observedAt,
                expiresAt,
                candidateIdentity,
                destinationIdentity,
                tokenSemanticsIdentity,
                capacityEvidenceIdentity,
                modelClass,
                requiredCapability,
                maximumReasoningRequirement,
                maximumContextTokens,
                maximumTokenBudget,
                maximumInvocationTime,
                costCurrencyCode,
                maximumCostMicrounits,
                maximumDataClassification);
    }

    public String contractVersion() {
        return contractVersion;
    }

    public ProfiledModelRequest profiledRequest() {
        return profiledRequest;
    }

    public ModelOutboundRouteAdmissionDecision.Eligible eligibility() {
        return eligibility;
    }

    public ModelRouteCandidateFacts candidateFacts() {
        return candidateFacts;
    }

    public String suitabilitySourceIdentity() {
        return suitabilitySourceIdentity;
    }

    public String sourceRevision() {
        return sourceRevision;
    }

    public Instant observedAt() {
        return observedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public String candidateIdentity() {
        return candidateIdentity;
    }

    public String destinationIdentity() {
        return destinationIdentity;
    }

    public String tokenSemanticsIdentity() {
        return tokenSemanticsIdentity;
    }

    public String capacityEvidenceIdentity() {
        return capacityEvidenceIdentity;
    }

    public String modelClass() {
        return modelClass;
    }

    public String requiredCapability() {
        return requiredCapability;
    }

    public ModelReasoningRequirement maximumReasoningRequirement() {
        return maximumReasoningRequirement;
    }

    public long maximumContextTokens() {
        return maximumContextTokens;
    }

    public ModelTokenBudget maximumTokenBudget() {
        return maximumTokenBudget;
    }

    public Duration maximumInvocationTime() {
        return maximumInvocationTime;
    }

    public String costCurrencyCode() {
        return costCurrencyCode;
    }

    public long maximumCostMicrounits() {
        return maximumCostMicrounits;
    }

    public ModelDataClassification maximumDataClassification() {
        return maximumDataClassification;
    }

    @Override
    public String toString() {
        return "RemoteModelCandidateSuitability[contractVersion=" + contractVersion
                + ", suitabilitySourceIdentity=" + suitabilitySourceIdentity
                + ", sourceRevision=" + sourceRevision
                + ", tokenSemanticsIdentity=" + tokenSemanticsIdentity
                + ", capacityEvidenceIdentity=" + capacityEvidenceIdentity + "]";
    }
}
