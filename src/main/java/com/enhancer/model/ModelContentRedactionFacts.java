package com.enhancer.model;

import java.time.Instant;
import java.util.Set;

/** Opaque independently governed redaction facts for one exact digest pair. */
public final class ModelContentRedactionFacts {

    public static final String SCHEMA_VERSION = "model-content-redaction-facts-v1";

    private final String schemaVersion;
    private final String sourcePromptDigest;
    private final String outboundPromptDigest;
    private final String redactorIdentity;
    private final String redactorRevision;
    private final String rulesetIdentity;
    private final String rulesetRevision;
    private final Instant observedAt;
    private final Instant expiresAt;
    private final ModelInspectionCoverage coverage;
    private final Set<String> coveredFindingIdentities;

    private ModelContentRedactionFacts(
            String schemaVersion,
            String sourcePromptDigest,
            String outboundPromptDigest,
            String redactorIdentity,
            String redactorRevision,
            String rulesetIdentity,
            String rulesetRevision,
            Instant observedAt,
            Instant expiresAt,
            ModelInspectionCoverage coverage,
            Set<String> coveredFindingIdentities) {
        this.schemaVersion =
                ModelOutboundRouteValueChecks.label(schemaVersion, "schemaVersion");
        this.sourcePromptDigest = ModelOutboundRouteValueChecks.digest(
                sourcePromptDigest, "sourcePromptDigest");
        this.outboundPromptDigest = ModelOutboundRouteValueChecks.digest(
                outboundPromptDigest, "outboundPromptDigest");
        this.redactorIdentity =
                ModelOutboundRouteValueChecks.label(redactorIdentity, "redactorIdentity");
        this.redactorRevision = ModelOutboundRouteValueChecks.revision(
                redactorRevision, "redactorRevision");
        this.rulesetIdentity =
                ModelOutboundRouteValueChecks.label(rulesetIdentity, "rulesetIdentity");
        this.rulesetRevision = ModelOutboundRouteValueChecks.revision(
                rulesetRevision, "rulesetRevision");
        ModelOutboundRouteValueChecks.window(observedAt, expiresAt);
        this.observedAt = observedAt;
        this.expiresAt = expiresAt;
        this.coverage = java.util.Objects.requireNonNull(
                coverage, "coverage must not be null");
        Set<String> copied = ModelOutboundRouteValueChecks.immutableSet(
                coveredFindingIdentities, "coveredFindingIdentities");
        for (String finding : copied) {
            ModelOutboundRouteValueChecks.label(finding, "coveredFindingIdentity");
        }
        this.coveredFindingIdentities = copied;
    }

    static ModelContentRedactionFacts create(
            String schemaVersion,
            String sourcePromptDigest,
            String outboundPromptDigest,
            String redactorIdentity,
            String redactorRevision,
            String rulesetIdentity,
            String rulesetRevision,
            Instant observedAt,
            Instant expiresAt,
            ModelInspectionCoverage coverage,
            Set<String> coveredFindingIdentities) {
        return new ModelContentRedactionFacts(
                schemaVersion,
                sourcePromptDigest,
                outboundPromptDigest,
                redactorIdentity,
                redactorRevision,
                rulesetIdentity,
                rulesetRevision,
                observedAt,
                expiresAt,
                coverage,
                coveredFindingIdentities);
    }

    public String schemaVersion() { return schemaVersion; }
    public String sourcePromptDigest() { return sourcePromptDigest; }
    public String outboundPromptDigest() { return outboundPromptDigest; }
    public String redactorIdentity() { return redactorIdentity; }
    public String redactorRevision() { return redactorRevision; }
    public String rulesetIdentity() { return rulesetIdentity; }
    public String rulesetRevision() { return rulesetRevision; }
    public Instant observedAt() { return observedAt; }
    public Instant expiresAt() { return expiresAt; }
    public ModelInspectionCoverage coverage() { return coverage; }
    public Set<String> coveredFindingIdentities() { return coveredFindingIdentities; }

    @Override
    public String toString() {
        return "ModelContentRedactionFacts[redactorIdentity=" + redactorIdentity
                + ", redactorRevision=" + redactorRevision
                + ", rulesetIdentity=" + rulesetIdentity
                + ", rulesetRevision=" + rulesetRevision
                + ", coverage=" + coverage
                + ", coveredFindingCount=" + coveredFindingIdentities.size() + "]";
    }
}
