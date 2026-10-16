package com.enhancer.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Opaque independently governed capacity and cost facts for one remote candidate. */
public final class RemoteModelCandidateCapabilityFacts {

    public static final String SCHEMA_VERSION =
            "remote-model-candidate-capability-facts-v1";

    private final String schemaVersion;
    private final String factSourceIdentity;
    private final String sourceRevision;
    private final String capacityEvidenceIdentity;
    private final Instant observedAt;
    private final Instant expiresAt;
    private final String candidateIdentity;
    private final String destinationIdentity;
    private final String modelClass;
    private final String requiredCapability;
    private final String tokenSemanticsIdentity;
    private final String priceScheduleIdentity;
    private final ModelTokenBudget tokenBudgetBasis;
    private final ModelReasoningRequirement maximumReasoningRequirement;
    private final long maximumContextTokens;
    private final ModelTokenBudget maximumTokenBudget;
    private final Duration maximumInvocationTime;
    private final String costCurrencyCode;
    private final long maximumCostMicrounits;
    private final ModelDataClassification maximumDataClassification;

    private RemoteModelCandidateCapabilityFacts(
            String schemaVersion,
            String factSourceIdentity,
            String sourceRevision,
            String capacityEvidenceIdentity,
            Instant observedAt,
            Instant expiresAt,
            String candidateIdentity,
            String destinationIdentity,
            String modelClass,
            String requiredCapability,
            String tokenSemanticsIdentity,
            String priceScheduleIdentity,
            ModelTokenBudget tokenBudgetBasis,
            ModelReasoningRequirement maximumReasoningRequirement,
            long maximumContextTokens,
            ModelTokenBudget maximumTokenBudget,
            Duration maximumInvocationTime,
            String costCurrencyCode,
            long maximumCostMicrounits,
            ModelDataClassification maximumDataClassification) {
        this.schemaVersion =
                ModelOutboundRouteValueChecks.label(schemaVersion, "schemaVersion");
        this.factSourceIdentity = ModelOutboundRouteValueChecks.label(
                factSourceIdentity, "factSourceIdentity");
        this.sourceRevision =
                ModelOutboundRouteValueChecks.revision(sourceRevision, "sourceRevision");
        this.capacityEvidenceIdentity = ModelOutboundRouteValueChecks.label(
                capacityEvidenceIdentity, "capacityEvidenceIdentity");
        ModelOutboundRouteValueChecks.window(observedAt, expiresAt);
        this.observedAt = observedAt;
        this.expiresAt = expiresAt;
        this.candidateIdentity = ModelOutboundRouteValueChecks.label(
                candidateIdentity, "candidateIdentity");
        this.destinationIdentity = ModelOutboundRouteValueChecks.label(
                destinationIdentity, "destinationIdentity");
        this.modelClass = ModelOutboundRouteValueChecks.label(modelClass, "modelClass");
        this.requiredCapability = ModelOutboundRouteValueChecks.label(
                requiredCapability, "requiredCapability");
        this.tokenSemanticsIdentity = ModelOutboundRouteValueChecks.label(
                tokenSemanticsIdentity, "tokenSemanticsIdentity");
        this.priceScheduleIdentity = ModelOutboundRouteValueChecks.label(
                priceScheduleIdentity, "priceScheduleIdentity");
        this.tokenBudgetBasis = Objects.requireNonNull(
                tokenBudgetBasis, "tokenBudgetBasis must not be null");
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

    static RemoteModelCandidateCapabilityFacts create(
            String schemaVersion,
            String factSourceIdentity,
            String sourceRevision,
            String capacityEvidenceIdentity,
            Instant observedAt,
            Instant expiresAt,
            String candidateIdentity,
            String destinationIdentity,
            String modelClass,
            String requiredCapability,
            String tokenSemanticsIdentity,
            String priceScheduleIdentity,
            ModelTokenBudget tokenBudgetBasis,
            ModelReasoningRequirement maximumReasoningRequirement,
            long maximumContextTokens,
            ModelTokenBudget maximumTokenBudget,
            Duration maximumInvocationTime,
            String costCurrencyCode,
            long maximumCostMicrounits,
            ModelDataClassification maximumDataClassification) {
        return new RemoteModelCandidateCapabilityFacts(
                schemaVersion,
                factSourceIdentity,
                sourceRevision,
                capacityEvidenceIdentity,
                observedAt,
                expiresAt,
                candidateIdentity,
                destinationIdentity,
                modelClass,
                requiredCapability,
                tokenSemanticsIdentity,
                priceScheduleIdentity,
                tokenBudgetBasis,
                maximumReasoningRequirement,
                maximumContextTokens,
                maximumTokenBudget,
                maximumInvocationTime,
                costCurrencyCode,
                maximumCostMicrounits,
                maximumDataClassification);
    }

    public String schemaVersion() { return schemaVersion; }
    public String factSourceIdentity() { return factSourceIdentity; }
    public String sourceRevision() { return sourceRevision; }
    public String capacityEvidenceIdentity() { return capacityEvidenceIdentity; }
    public Instant observedAt() { return observedAt; }
    public Instant expiresAt() { return expiresAt; }
    public String candidateIdentity() { return candidateIdentity; }
    public String destinationIdentity() { return destinationIdentity; }
    public String modelClass() { return modelClass; }
    public String requiredCapability() { return requiredCapability; }
    public String tokenSemanticsIdentity() { return tokenSemanticsIdentity; }
    public String priceScheduleIdentity() { return priceScheduleIdentity; }
    public ModelTokenBudget tokenBudgetBasis() { return tokenBudgetBasis; }
    public ModelReasoningRequirement maximumReasoningRequirement() {
        return maximumReasoningRequirement;
    }
    public long maximumContextTokens() { return maximumContextTokens; }
    public ModelTokenBudget maximumTokenBudget() { return maximumTokenBudget; }
    public Duration maximumInvocationTime() { return maximumInvocationTime; }
    public String costCurrencyCode() { return costCurrencyCode; }
    public long maximumCostMicrounits() { return maximumCostMicrounits; }
    public ModelDataClassification maximumDataClassification() {
        return maximumDataClassification;
    }

    @Override
    public String toString() {
        return "RemoteModelCandidateCapabilityFacts[schemaVersion=" + schemaVersion
                + ", factSourceIdentity=" + factSourceIdentity
                + ", sourceRevision=" + sourceRevision
                + ", capacityEvidenceIdentity=" + capacityEvidenceIdentity
                + ", candidateIdentity=" + candidateIdentity
                + ", destinationIdentity=" + destinationIdentity
                + ", modelClass=" + modelClass
                + ", requiredCapability=" + requiredCapability
                + ", tokenSemanticsIdentity=" + tokenSemanticsIdentity
                + ", priceScheduleIdentity=" + priceScheduleIdentity
                + ", costCurrencyCode=" + costCurrencyCode
                + ", maximumReasoningRequirement=" + maximumReasoningRequirement
                + ", maximumDataClassification=" + maximumDataClassification + "]";
    }
}
