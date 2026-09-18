package com.enhancer.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Opaque independently governed facts for one structurally remote candidate.
 *
 * <p>Construction is package-private so only a separately governed source in this
 * package can create production facts.
 */
public final class ModelRouteCandidateFacts {

    public static final String SCHEMA_VERSION = "model-route-candidate-facts-v1";

    private final String schemaVersion;
    private final String candidateIdentity;
    private final String factSourceIdentity;
    private final String sourceRevision;
    private final Instant observedAt;
    private final Instant expiresAt;
    private final String modelClass;
    private final String requiredCapability;
    private final String destinationIdentity;

    private ModelRouteCandidateFacts(
            String schemaVersion,
            String candidateIdentity,
            String factSourceIdentity,
            String sourceRevision,
            Instant observedAt,
            Instant expiresAt,
            String modelClass,
            String requiredCapability,
            String destinationIdentity) {
        this.schemaVersion =
                ModelOutboundRouteValueChecks.label(schemaVersion, "schemaVersion");
        this.candidateIdentity =
                ModelOutboundRouteValueChecks.label(candidateIdentity, "candidateIdentity");
        this.factSourceIdentity =
                ModelOutboundRouteValueChecks.label(factSourceIdentity, "factSourceIdentity");
        this.sourceRevision =
                ModelOutboundRouteValueChecks.revision(sourceRevision, "sourceRevision");
        ModelOutboundRouteValueChecks.window(observedAt, expiresAt);
        this.observedAt = observedAt;
        this.expiresAt = expiresAt;
        this.modelClass = ModelOutboundRouteValueChecks.label(modelClass, "modelClass");
        this.requiredCapability = ModelOutboundRouteValueChecks.label(
                requiredCapability, "requiredCapability");
        this.destinationIdentity = ModelOutboundRouteValueChecks.label(
                destinationIdentity, "destinationIdentity");
    }

    static ModelRouteCandidateFacts create(
            String schemaVersion,
            String candidateIdentity,
            String factSourceIdentity,
            String sourceRevision,
            Instant observedAt,
            Instant expiresAt,
            String modelClass,
            String requiredCapability,
            String destinationIdentity) {
        return new ModelRouteCandidateFacts(
                schemaVersion,
                candidateIdentity,
                factSourceIdentity,
                sourceRevision,
                observedAt,
                expiresAt,
                modelClass,
                requiredCapability,
                destinationIdentity);
    }

    public String schemaVersion() {
        return schemaVersion;
    }

    public String candidateIdentity() {
        return candidateIdentity;
    }

    public String factSourceIdentity() {
        return factSourceIdentity;
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

    public String modelClass() {
        return modelClass;
    }

    public String requiredCapability() {
        return requiredCapability;
    }

    public ModelRouteKind routeKind() {
        return ModelRouteKind.REMOTE;
    }

    public String destinationIdentity() {
        return destinationIdentity;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ModelRouteCandidateFacts that)) {
            return false;
        }
        return schemaVersion.equals(that.schemaVersion)
                && candidateIdentity.equals(that.candidateIdentity)
                && factSourceIdentity.equals(that.factSourceIdentity)
                && sourceRevision.equals(that.sourceRevision)
                && observedAt.equals(that.observedAt)
                && expiresAt.equals(that.expiresAt)
                && modelClass.equals(that.modelClass)
                && requiredCapability.equals(that.requiredCapability)
                && destinationIdentity.equals(that.destinationIdentity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                schemaVersion,
                candidateIdentity,
                factSourceIdentity,
                sourceRevision,
                observedAt,
                expiresAt,
                modelClass,
                requiredCapability,
                destinationIdentity);
    }

    @Override
    public String toString() {
        return "ModelRouteCandidateFacts[candidateIdentity=" + candidateIdentity
                + ", factSourceIdentity=" + factSourceIdentity
                + ", sourceRevision=" + sourceRevision
                + ", modelClass=" + modelClass
                + ", routeKind=" + routeKind()
                + ", destinationIdentity=" + destinationIdentity + "]";
    }
}
