package com.enhancer.runtime;

import com.enhancer.bus.ControlSignal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Immutable non-secret first-verification audit for one signed cancellation grant. */
public record CancellationAuthorizationAuditRecord(
        String schemaVersion,
        String authorizationId,
        String audience,
        String issuerId,
        String subjectId,
        String actorId,
        String goalId,
        String controlMessageId,
        ControlSignal signal,
        String requestSha256,
        String proofSha256,
        String keyId,
        String publicKeySha256,
        String signatureAlgorithm,
        String trustConfigurationId,
        String trustConfigurationRevision,
        String policyRevision,
        Instant issuedAt,
        Instant expiresAt,
        Instant verifiedAt,
        Optional<Instant> keyRevokedAt,
        String verifierVersion) {
    public static final String SCHEMA_VERSION = "cancellation-authorization-audit-v1";
    private static final Pattern BOUNDED_IDENTITY = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9._:-]{0,255}");
    private static final Pattern LOWERCASE_SHA256 = Pattern.compile("[0-9a-f]{64}");

    public CancellationAuthorizationAuditRecord {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException(
                    "cancellation authorization audit schema is unsupported");
        }
        authorizationId = RuntimeIdentity.canonicalUuid(
                authorizationId, "authorizationId");
        audience = bounded(audience, "audience");
        issuerId = bounded(issuerId, "issuerId");
        subjectId = bounded(subjectId, "subjectId");
        actorId = bounded(actorId, "actorId");
        goalId = RuntimeIdentity.canonicalUuid(goalId, "goalId");
        controlMessageId = RuntimeIdentity.canonicalUuid(
                controlMessageId, "controlMessageId");
        Objects.requireNonNull(signal, "signal must not be null");
        if (signal != ControlSignal.CANCEL) {
            throw new IllegalArgumentException(
                    "cancellation authorization audit permits only CANCEL");
        }
        requestSha256 = sha256(requestSha256, "requestSha256");
        proofSha256 = sha256(proofSha256, "proofSha256");
        keyId = bounded(keyId, "keyId");
        publicKeySha256 = sha256(publicKeySha256, "publicKeySha256");
        signatureAlgorithm = bounded(signatureAlgorithm, "signatureAlgorithm");
        if (!"Ed25519".equals(signatureAlgorithm)) {
            throw new IllegalArgumentException(
                    "cancellation authorization audit algorithm is unsupported");
        }
        trustConfigurationId = bounded(
                trustConfigurationId, "trustConfigurationId");
        trustConfigurationRevision = bounded(
                trustConfigurationRevision, "trustConfigurationRevision");
        policyRevision = bounded(policyRevision, "policyRevision");
        Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(verifiedAt, "verifiedAt must not be null");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
        if (!verifiedAt.isBefore(expiresAt)) {
            throw new IllegalArgumentException(
                    "verifiedAt must precede expiresAt");
        }
        keyRevokedAt = Objects.requireNonNull(
                keyRevokedAt, "keyRevokedAt must not be null");
        verifierVersion = bounded(verifierVersion, "verifierVersion");
    }

    public static CancellationAuthorizationAuditRecord create(
            String authorizationId,
            String audience,
            String issuerId,
            String subjectId,
            String actorId,
            String goalId,
            String controlMessageId,
            ControlSignal signal,
            String requestSha256,
            String proofSha256,
            String keyId,
            String publicKeySha256,
            String signatureAlgorithm,
            String trustConfigurationId,
            String trustConfigurationRevision,
            String policyRevision,
            Instant issuedAt,
            Instant expiresAt,
            Instant verifiedAt,
            Optional<Instant> keyRevokedAt,
            String verifierVersion) {
        return new CancellationAuthorizationAuditRecord(
                SCHEMA_VERSION,
                authorizationId,
                audience,
                issuerId,
                subjectId,
                actorId,
                goalId,
                controlMessageId,
                signal,
                requestSha256,
                proofSha256,
                keyId,
                publicKeySha256,
                signatureAlgorithm,
                trustConfigurationId,
                trustConfigurationRevision,
                policyRevision,
                issuedAt,
                expiresAt,
                verifiedAt,
                keyRevokedAt,
                verifierVersion);
    }

    static CancellationAuthorizationAuditRecord from(
            SignedCancellationGrantVerifier.VerifiedAuthorization authorization) {
        return create(
                authorization.authorizationId(),
                authorization.audience(),
                authorization.issuerId(),
                authorization.subjectId(),
                authorization.actorId(),
                authorization.goalId(),
                authorization.controlMessageId(),
                ControlSignal.CANCEL,
                authorization.requestSha256(),
                authorization.proofSha256(),
                authorization.keyId(),
                authorization.publicKeySha256(),
                authorization.signatureAlgorithm(),
                authorization.trustConfigurationId(),
                authorization.trustConfigurationRevision(),
                authorization.policyRevision(),
                authorization.issuedAt(),
                authorization.expiresAt(),
                authorization.verifiedAt(),
                authorization.keyRevokedAt(),
                authorization.verifierVersion());
    }

    boolean matchesCurrent(
            SignedCancellationGrantVerifier.VerifiedAuthorization authorization) {
        return authorizationId.equals(authorization.authorizationId())
                && audience.equals(authorization.audience())
                && issuerId.equals(authorization.issuerId())
                && subjectId.equals(authorization.subjectId())
                && actorId.equals(authorization.actorId())
                && goalId.equals(authorization.goalId())
                && controlMessageId.equals(authorization.controlMessageId())
                && requestSha256.equals(authorization.requestSha256())
                && proofSha256.equals(authorization.proofSha256())
                && keyId.equals(authorization.keyId())
                && publicKeySha256.equals(authorization.publicKeySha256())
                && signatureAlgorithm.equals(authorization.signatureAlgorithm())
                && trustConfigurationId.equals(authorization.trustConfigurationId())
                && trustConfigurationRevision.equals(
                        authorization.trustConfigurationRevision())
                && policyRevision.equals(authorization.policyRevision())
                && issuedAt.equals(authorization.issuedAt())
                && expiresAt.equals(authorization.expiresAt())
                && keyRevokedAt.equals(authorization.keyRevokedAt())
                && verifierVersion.equals(authorization.verifierVersion());
    }

    public String reference() {
        return "cancellation-authorization/" + authorizationId;
    }

    private static String bounded(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (!BOUNDED_IDENTITY.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be a bounded identity");
        }
        return value;
    }

    private static String sha256(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (!LOWERCASE_SHA256.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
        return value;
    }
}
