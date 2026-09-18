package com.enhancer.model;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/** One exact destination-policy rule; construction is package-private. */
public final class ModelDestinationAllowRule {

    private final String candidateIdentity;
    private final String destinationIdentity;
    private final Set<ModelOutboundPurpose> permittedPurposes;
    private final Set<ModelRetentionUse> permittedRetentionUses;
    private final Duration maximumRetentionDuration;
    private final ModelDataClassification maximumDataClassification;
    private final String pricingFactSourceIdentity;

    private ModelDestinationAllowRule(
            String candidateIdentity,
            String destinationIdentity,
            Set<ModelOutboundPurpose> permittedPurposes,
            Set<ModelRetentionUse> permittedRetentionUses,
            Duration maximumRetentionDuration,
            ModelDataClassification maximumDataClassification,
            String pricingFactSourceIdentity) {
        this.candidateIdentity = ModelOutboundRouteValueChecks.label(
                candidateIdentity, "candidateIdentity");
        this.destinationIdentity = ModelOutboundRouteValueChecks.label(
                destinationIdentity, "destinationIdentity");
        this.permittedPurposes = ModelOutboundRouteValueChecks.immutableSet(
                permittedPurposes, "permittedPurposes");
        this.permittedRetentionUses = ModelOutboundRouteValueChecks.immutableSet(
                permittedRetentionUses, "permittedRetentionUses");
        Objects.requireNonNull(
                maximumRetentionDuration,
                "maximumRetentionDuration must not be null");
        if (maximumRetentionDuration.isNegative()
                || maximumRetentionDuration.compareTo(
                        ModelRetentionFacts.MAXIMUM_SUPPORTED_DURATION) > 0
                || (permittedRetentionUses.isEmpty()
                        != maximumRetentionDuration.isZero())) {
            throw new IllegalArgumentException(
                    "retention permission must be bounded and internally consistent");
        }
        this.maximumRetentionDuration = maximumRetentionDuration;
        this.maximumDataClassification = Objects.requireNonNull(
                maximumDataClassification,
                "maximumDataClassification must not be null");
        this.pricingFactSourceIdentity = ModelOutboundRouteValueChecks.label(
                pricingFactSourceIdentity, "pricingFactSourceIdentity");
    }

    static ModelDestinationAllowRule create(
            String candidateIdentity,
            String destinationIdentity,
            Set<ModelOutboundPurpose> permittedPurposes,
            Set<ModelRetentionUse> permittedRetentionUses,
            Duration maximumRetentionDuration,
            ModelDataClassification maximumDataClassification,
            String pricingFactSourceIdentity) {
        return new ModelDestinationAllowRule(
                candidateIdentity,
                destinationIdentity,
                permittedPurposes,
                permittedRetentionUses,
                maximumRetentionDuration,
                maximumDataClassification,
                pricingFactSourceIdentity);
    }

    public String candidateIdentity() {
        return candidateIdentity;
    }

    public String destinationIdentity() {
        return destinationIdentity;
    }

    public Set<ModelOutboundPurpose> permittedPurposes() {
        return permittedPurposes;
    }

    public Set<ModelRetentionUse> permittedRetentionUses() {
        return permittedRetentionUses;
    }

    public Duration maximumRetentionDuration() {
        return maximumRetentionDuration;
    }

    public ModelDataClassification maximumDataClassification() {
        return maximumDataClassification;
    }

    public String pricingFactSourceIdentity() {
        return pricingFactSourceIdentity;
    }
}
