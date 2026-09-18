package com.enhancer.model;

import java.time.Instant;
import java.util.Set;

/** Opaque independently governed inspection of one exact content digest. */
public final class ModelContentInspectionFacts {

    public static final String SCHEMA_VERSION = "model-content-inspection-facts-v1";

    private final String schemaVersion;
    private final String scannerIdentity;
    private final String scannerRevision;
    private final String rulesetIdentity;
    private final String rulesetRevision;
    private final Instant observedAt;
    private final Instant expiresAt;
    private final String contentDigest;
    private final ModelDataClassification classification;
    private final ModelInspectionCoverage coverage;
    private final ModelContentInspectionStatus status;
    private final Set<String> findingIdentities;

    private ModelContentInspectionFacts(
            String schemaVersion,
            String scannerIdentity,
            String scannerRevision,
            String rulesetIdentity,
            String rulesetRevision,
            Instant observedAt,
            Instant expiresAt,
            String contentDigest,
            ModelDataClassification classification,
            ModelInspectionCoverage coverage,
            ModelContentInspectionStatus status,
            Set<String> findingIdentities) {
        this.schemaVersion =
                ModelOutboundRouteValueChecks.label(schemaVersion, "schemaVersion");
        this.scannerIdentity =
                ModelOutboundRouteValueChecks.label(scannerIdentity, "scannerIdentity");
        this.scannerRevision =
                ModelOutboundRouteValueChecks.revision(scannerRevision, "scannerRevision");
        this.rulesetIdentity =
                ModelOutboundRouteValueChecks.label(rulesetIdentity, "rulesetIdentity");
        this.rulesetRevision =
                ModelOutboundRouteValueChecks.revision(rulesetRevision, "rulesetRevision");
        ModelOutboundRouteValueChecks.window(observedAt, expiresAt);
        this.observedAt = observedAt;
        this.expiresAt = expiresAt;
        this.contentDigest =
                ModelOutboundRouteValueChecks.digest(contentDigest, "contentDigest");
        this.classification = java.util.Objects.requireNonNull(
                classification, "classification must not be null");
        this.coverage =
                java.util.Objects.requireNonNull(coverage, "coverage must not be null");
        this.status = java.util.Objects.requireNonNull(status, "status must not be null");
        Set<String> copied =
                ModelOutboundRouteValueChecks.immutableSet(
                        findingIdentities, "findingIdentities");
        for (String finding : copied) {
            ModelOutboundRouteValueChecks.label(finding, "findingIdentity");
        }
        if (status == ModelContentInspectionStatus.REDACTION_REQUIRED
                && copied.isEmpty()) {
            throw new IllegalArgumentException(
                    "REDACTION_REQUIRED inspection must identify findings");
        }
        this.findingIdentities = copied;
    }

    static ModelContentInspectionFacts create(
            String schemaVersion,
            String scannerIdentity,
            String scannerRevision,
            String rulesetIdentity,
            String rulesetRevision,
            Instant observedAt,
            Instant expiresAt,
            String contentDigest,
            ModelDataClassification classification,
            ModelInspectionCoverage coverage,
            ModelContentInspectionStatus status,
            Set<String> findingIdentities) {
        return new ModelContentInspectionFacts(
                schemaVersion,
                scannerIdentity,
                scannerRevision,
                rulesetIdentity,
                rulesetRevision,
                observedAt,
                expiresAt,
                contentDigest,
                classification,
                coverage,
                status,
                findingIdentities);
    }

    public String schemaVersion() { return schemaVersion; }
    public String scannerIdentity() { return scannerIdentity; }
    public String scannerRevision() { return scannerRevision; }
    public String rulesetIdentity() { return rulesetIdentity; }
    public String rulesetRevision() { return rulesetRevision; }
    public Instant observedAt() { return observedAt; }
    public Instant expiresAt() { return expiresAt; }
    public String contentDigest() { return contentDigest; }
    public ModelDataClassification classification() { return classification; }
    public ModelInspectionCoverage coverage() { return coverage; }
    public ModelContentInspectionStatus status() { return status; }
    public Set<String> findingIdentities() { return findingIdentities; }

    @Override
    public String toString() {
        return "ModelContentInspectionFacts[scannerIdentity=" + scannerIdentity
                + ", scannerRevision=" + scannerRevision
                + ", rulesetIdentity=" + rulesetIdentity
                + ", rulesetRevision=" + rulesetRevision
                + ", classification=" + classification
                + ", coverage=" + coverage
                + ", status=" + status
                + ", findingCount=" + findingIdentities.size() + "]";
    }
}
