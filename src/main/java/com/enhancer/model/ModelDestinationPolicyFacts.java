package com.enhancer.model;

import java.time.Instant;
import java.util.Set;

/** Opaque independently governed destination policy facts. */
public final class ModelDestinationPolicyFacts {

    public static final String SCHEMA_VERSION = "model-destination-policy-facts-v1";

    private final String schemaVersion;
    private final String destinationIdentity;
    private final String policySourceIdentity;
    private final String sourceRevision;
    private final Instant observedAt;
    private final Instant expiresAt;
    private final Set<ModelDestinationAllowRule> allowRules;

    private ModelDestinationPolicyFacts(
            String schemaVersion,
            String destinationIdentity,
            String policySourceIdentity,
            String sourceRevision,
            Instant observedAt,
            Instant expiresAt,
            Set<ModelDestinationAllowRule> allowRules) {
        this.schemaVersion =
                ModelOutboundRouteValueChecks.label(schemaVersion, "schemaVersion");
        this.destinationIdentity = ModelOutboundRouteValueChecks.label(
                destinationIdentity, "destinationIdentity");
        this.policySourceIdentity = ModelOutboundRouteValueChecks.label(
                policySourceIdentity, "policySourceIdentity");
        this.sourceRevision =
                ModelOutboundRouteValueChecks.revision(sourceRevision, "sourceRevision");
        ModelOutboundRouteValueChecks.window(observedAt, expiresAt);
        this.observedAt = observedAt;
        this.expiresAt = expiresAt;
        this.allowRules =
                ModelOutboundRouteValueChecks.immutableSet(allowRules, "allowRules");
        rejectAmbiguousRules(this.allowRules);
    }

    static ModelDestinationPolicyFacts create(
            String schemaVersion,
            String destinationIdentity,
            String policySourceIdentity,
            String sourceRevision,
            Instant observedAt,
            Instant expiresAt,
            Set<ModelDestinationAllowRule> allowRules) {
        return new ModelDestinationPolicyFacts(
                schemaVersion,
                destinationIdentity,
                policySourceIdentity,
                sourceRevision,
                observedAt,
                expiresAt,
                allowRules);
    }

    private static void rejectAmbiguousRules(Set<ModelDestinationAllowRule> rules) {
        for (ModelDestinationAllowRule left : rules) {
            for (ModelDestinationAllowRule right : rules) {
                if (left == right) {
                    continue;
                }
                if (left.candidateIdentity().equals(right.candidateIdentity())
                        && left.destinationIdentity().equals(right.destinationIdentity())) {
                    for (ModelOutboundPurpose purpose : left.permittedPurposes()) {
                        if (right.permittedPurposes().contains(purpose)) {
                            throw new IllegalArgumentException(
                                    "allowRules must not overlap for one route and purpose");
                        }
                    }
                }
            }
        }
    }

    public String schemaVersion() {
        return schemaVersion;
    }

    public String destinationIdentity() {
        return destinationIdentity;
    }

    public String policySourceIdentity() {
        return policySourceIdentity;
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

    public Set<ModelDestinationAllowRule> allowRules() {
        return allowRules;
    }

    @Override
    public String toString() {
        return "ModelDestinationPolicyFacts[destinationIdentity=" + destinationIdentity
                + ", policySourceIdentity=" + policySourceIdentity
                + ", sourceRevision=" + sourceRevision
                + ", allowRuleCount=" + allowRules.size() + "]";
    }
}
