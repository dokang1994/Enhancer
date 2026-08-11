package com.enhancer.runtime;

import com.enhancer.bus.MessageEnvelope;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/** Verifies one detached signed cancellation grant against an injected public policy. */
final class SignedCancellationGrantVerifier {
    static final String VERIFIER_VERSION = "signed-cancellation-verifier-v1";
    private static final String SIGNATURE_ALGORITHM = "Ed25519";
    private static final byte[] ACTOR_DOMAIN =
            "signed-cancellation-actor-v1".getBytes(StandardCharsets.UTF_8);

    private final CancellationGrantTrustPolicy policy;
    private final Clock clock;

    SignedCancellationGrantVerifier(
            CancellationGrantTrustPolicy policy,
            Clock clock) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    Result verify(
            String canonicalGoalId,
            MessageEnvelope retainedRequest,
            DetachedSignedCancellationGrant grant) {
        String goalId = RuntimeIdentity.canonicalUuid(
                canonicalGoalId, "canonicalGoalId");
        MessageEnvelope request = Objects.requireNonNull(
                retainedRequest, "retainedRequest must not be null");
        DetachedSignedCancellationGrant checkedGrant = Objects.requireNonNull(
                grant, "grant must not be null");
        DetachedSignedCancellationGrant.Claims claims = checkedGrant.claims();

        if (!claims.goalId().equals(goalId)) {
            return denied(SignedCancellationGrantDenial.TARGET_MISMATCH);
        }
        if (!claims.controlMessageId().equals(request.messageId())) {
            return denied(SignedCancellationGrantDenial.REQUEST_MISMATCH);
        }
        String requestSha256;
        try {
            requestSha256 = DetachedSignedCancellationGrant.requestSha256(request);
        } catch (IllegalArgumentException exception) {
            return denied(SignedCancellationGrantDenial.REQUEST_MISMATCH);
        }
        if (!MessageDigest.isEqual(
                HexFormat.of().parseHex(claims.requestSha256()),
                HexFormat.of().parseHex(requestSha256))) {
            return denied(SignedCancellationGrantDenial.REQUEST_MISMATCH);
        }
        if (!policy.audience().equals(claims.audience())
                || !policy.policyRevision().equals(claims.policyRevision())) {
            return denied(SignedCancellationGrantDenial.POLICY_MISMATCH);
        }

        Optional<CancellationGrantTrustPolicy.TrustedKey> found = policy.find(
                claims.issuerId(), claims.keyId());
        if (found.isEmpty()) {
            return denied(SignedCancellationGrantDenial.UNTRUSTED_ISSUER_OR_KEY);
        }
        CancellationGrantTrustPolicy.TrustedKey key = found.orElseThrow();
        if (!key.authorizes(claims.subjectId())) {
            return denied(SignedCancellationGrantDenial.SUBJECT_NOT_AUTHORIZED);
        }

        Instant now = clock.instant();
        if (claims.issuedAt().isAfter(now.plus(policy.clockSkew()))) {
            return denied(SignedCancellationGrantDenial.NOT_YET_VALID);
        }
        if (!claims.expiresAt().isAfter(now)) {
            return denied(SignedCancellationGrantDenial.EXPIRED);
        }
        Duration lifetime = Duration.between(claims.issuedAt(), claims.expiresAt());
        if (lifetime.compareTo(policy.maximumGrantLifetime()) > 0) {
            return denied(SignedCancellationGrantDenial.LIFETIME_EXCEEDED);
        }
        if (claims.issuedAt().isBefore(key.validFrom())
                || !claims.issuedAt().isBefore(key.validUntil())
                || now.isBefore(key.validFrom())
                || !now.isBefore(key.validUntil())) {
            return denied(SignedCancellationGrantDenial.KEY_NOT_VALID);
        }
        if (key.revokedAt().isPresent()
                && !now.isBefore(key.revokedAt().orElseThrow())) {
            return denied(SignedCancellationGrantDenial.KEY_REVOKED);
        }
        if (!validSignature(key, checkedGrant)) {
            return denied(SignedCancellationGrantDenial.INVALID_SIGNATURE);
        }

        return new Verified(new VerifiedAuthorization(
                claims.authorizationId(),
                claims.audience(),
                claims.issuerId(),
                claims.subjectId(),
                actorId(claims.issuerId(), claims.subjectId()),
                claims.goalId(),
                claims.controlMessageId(),
                claims.requestSha256(),
                checkedGrant.proofSha256(),
                claims.keyId(),
                key.publicKeySha256(),
                SIGNATURE_ALGORITHM,
                policy.configurationId(),
                policy.configurationRevision(),
                claims.policyRevision(),
                claims.issuedAt(),
                claims.expiresAt(),
                now,
                key.revokedAt(),
                VERIFIER_VERSION));
    }

    static String actorId(String issuerId, String subjectId) {
        MessageDigest digest = sha256();
        updateFrame(digest, ACTOR_DOMAIN);
        updateFrame(digest, issuerId.getBytes(StandardCharsets.UTF_8));
        updateFrame(digest, subjectId.getBytes(StandardCharsets.UTF_8));
        return "signed-cancellation-actor-v1:"
                + HexFormat.of().formatHex(digest.digest());
    }

    private boolean validSignature(
            CancellationGrantTrustPolicy.TrustedKey key,
            DetachedSignedCancellationGrant grant) {
        try {
            Signature verifier = Signature.getInstance(SIGNATURE_ALGORITHM);
            verifier.initVerify(key.publicKey());
            verifier.update(grant.signingBytes());
            return verifier.verify(grant.signature());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Ed25519 is unavailable", exception);
        } catch (GeneralSecurityException exception) {
            return false;
        }
    }

    private static Denied denied(SignedCancellationGrantDenial reason) {
        return new Denied(reason);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void updateFrame(MessageDigest digest, byte[] value) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value.length).array());
        digest.update(value);
    }

    sealed interface Result permits Verified, Denied {
    }

    record Verified(VerifiedAuthorization authorization) implements Result {
        Verified {
            Objects.requireNonNull(
                    authorization, "authorization must not be null");
        }
    }

    record Denied(SignedCancellationGrantDenial reason) implements Result {
        Denied {
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    record VerifiedAuthorization(
            String authorizationId,
            String audience,
            String issuerId,
            String subjectId,
            String actorId,
            String goalId,
            String controlMessageId,
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
        VerifiedAuthorization {
            Objects.requireNonNull(authorizationId, "authorizationId must not be null");
            Objects.requireNonNull(audience, "audience must not be null");
            Objects.requireNonNull(issuerId, "issuerId must not be null");
            Objects.requireNonNull(subjectId, "subjectId must not be null");
            Objects.requireNonNull(actorId, "actorId must not be null");
            Objects.requireNonNull(goalId, "goalId must not be null");
            Objects.requireNonNull(controlMessageId, "controlMessageId must not be null");
            Objects.requireNonNull(requestSha256, "requestSha256 must not be null");
            Objects.requireNonNull(proofSha256, "proofSha256 must not be null");
            Objects.requireNonNull(keyId, "keyId must not be null");
            Objects.requireNonNull(publicKeySha256, "publicKeySha256 must not be null");
            Objects.requireNonNull(signatureAlgorithm, "signatureAlgorithm must not be null");
            Objects.requireNonNull(
                    trustConfigurationId, "trustConfigurationId must not be null");
            Objects.requireNonNull(
                    trustConfigurationRevision,
                    "trustConfigurationRevision must not be null");
            Objects.requireNonNull(policyRevision, "policyRevision must not be null");
            Objects.requireNonNull(issuedAt, "issuedAt must not be null");
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
            Objects.requireNonNull(verifiedAt, "verifiedAt must not be null");
            Objects.requireNonNull(keyRevokedAt, "keyRevokedAt must not be null");
            Objects.requireNonNull(verifierVersion, "verifierVersion must not be null");
        }
    }
}
