package com.enhancer.model;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/** Opaque independently governed conservative price for one exact route and budget. */
public final class ModelRoutePricingFacts {

    public static final String SCHEMA_VERSION = "model-route-pricing-facts-v1";

    private static final Pattern CURRENCY_CODE = Pattern.compile("[A-Z]{3}");

    private final String schemaVersion;
    private final String factSourceIdentity;
    private final String sourceRevision;
    private final Instant observedAt;
    private final Instant expiresAt;
    private final String candidateIdentity;
    private final String destinationIdentity;
    private final String modelClass;
    private final String tokenSemanticsIdentity;
    private final String priceScheduleIdentity;
    private final ModelTokenBudget tokenBudgetBasis;
    private final String currencyCode;
    private final long worstCaseChargeMicrounits;

    private ModelRoutePricingFacts(
            String schemaVersion,
            String factSourceIdentity,
            String sourceRevision,
            Instant observedAt,
            Instant expiresAt,
            String candidateIdentity,
            String destinationIdentity,
            String modelClass,
            String tokenSemanticsIdentity,
            String priceScheduleIdentity,
            ModelTokenBudget tokenBudgetBasis,
            String currencyCode,
            long worstCaseChargeMicrounits) {
        this.schemaVersion =
                ModelOutboundRouteValueChecks.label(schemaVersion, "schemaVersion");
        this.factSourceIdentity = ModelOutboundRouteValueChecks.label(
                factSourceIdentity, "factSourceIdentity");
        this.sourceRevision =
                ModelOutboundRouteValueChecks.revision(sourceRevision, "sourceRevision");
        ModelOutboundRouteValueChecks.window(observedAt, expiresAt);
        this.observedAt = observedAt;
        this.expiresAt = expiresAt;
        this.candidateIdentity = ModelOutboundRouteValueChecks.label(
                candidateIdentity, "candidateIdentity");
        this.destinationIdentity = ModelOutboundRouteValueChecks.label(
                destinationIdentity, "destinationIdentity");
        this.modelClass = ModelOutboundRouteValueChecks.label(modelClass, "modelClass");
        this.tokenSemanticsIdentity = ModelOutboundRouteValueChecks.label(
                tokenSemanticsIdentity, "tokenSemanticsIdentity");
        this.priceScheduleIdentity = ModelOutboundRouteValueChecks.label(
                priceScheduleIdentity, "priceScheduleIdentity");
        this.tokenBudgetBasis = Objects.requireNonNull(
                tokenBudgetBasis, "tokenBudgetBasis must not be null");
        this.currencyCode = Objects.requireNonNull(
                currencyCode, "currencyCode must not be null");
        if (!CURRENCY_CODE.matcher(currencyCode).matches()) {
            throw new IllegalArgumentException(
                    "currencyCode must be an upper-case three-letter code");
        }
        if (worstCaseChargeMicrounits < 0
                || worstCaseChargeMicrounits > ModelCostBudget.MAX_MICROUNITS) {
            throw new IllegalArgumentException(
                    "worstCaseChargeMicrounits must be non-negative and bounded");
        }
        this.worstCaseChargeMicrounits = worstCaseChargeMicrounits;
    }

    static ModelRoutePricingFacts create(
            String schemaVersion,
            String factSourceIdentity,
            String sourceRevision,
            Instant observedAt,
            Instant expiresAt,
            String candidateIdentity,
            String destinationIdentity,
            String modelClass,
            String tokenSemanticsIdentity,
            String priceScheduleIdentity,
            ModelTokenBudget tokenBudgetBasis,
            String currencyCode,
            long worstCaseChargeMicrounits) {
        return new ModelRoutePricingFacts(
                schemaVersion,
                factSourceIdentity,
                sourceRevision,
                observedAt,
                expiresAt,
                candidateIdentity,
                destinationIdentity,
                modelClass,
                tokenSemanticsIdentity,
                priceScheduleIdentity,
                tokenBudgetBasis,
                currencyCode,
                worstCaseChargeMicrounits);
    }

    public String schemaVersion() { return schemaVersion; }
    public String factSourceIdentity() { return factSourceIdentity; }
    public String sourceRevision() { return sourceRevision; }
    public Instant observedAt() { return observedAt; }
    public Instant expiresAt() { return expiresAt; }
    public String candidateIdentity() { return candidateIdentity; }
    public String destinationIdentity() { return destinationIdentity; }
    public String modelClass() { return modelClass; }
    public String tokenSemanticsIdentity() { return tokenSemanticsIdentity; }
    public String priceScheduleIdentity() { return priceScheduleIdentity; }
    public ModelTokenBudget tokenBudgetBasis() { return tokenBudgetBasis; }
    public String currencyCode() { return currencyCode; }
    public long worstCaseChargeMicrounits() { return worstCaseChargeMicrounits; }

    @Override
    public String toString() {
        return "ModelRoutePricingFacts[factSourceIdentity=" + factSourceIdentity
                + ", sourceRevision=" + sourceRevision
                + ", candidateIdentity=" + candidateIdentity
                + ", destinationIdentity=" + destinationIdentity
                + ", modelClass=" + modelClass
                + ", tokenSemanticsIdentity=" + tokenSemanticsIdentity
                + ", priceScheduleIdentity=" + priceScheduleIdentity
                + ", currencyCode=" + currencyCode + "]";
    }
}
