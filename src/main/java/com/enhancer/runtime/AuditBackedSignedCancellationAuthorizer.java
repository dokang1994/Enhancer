package com.enhancer.runtime;

import com.enhancer.bus.ControlSignal;
import com.enhancer.bus.MessageEnvelope;
import java.io.IOException;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/** Revalidates one transient detached grant and persists audit before approval. */
public final class AuditBackedSignedCancellationAuthorizer
        implements ControlRequestAuthorizer {
    private final byte[] proof;
    private final SignedCancellationGrantVerifier verifier;
    private final CancellationAuthorizationAuditStore auditStore;

    public AuditBackedSignedCancellationAuthorizer(
            byte[] proof,
            CancellationGrantTrustPolicy policy,
            Clock clock,
            CancellationAuthorizationAuditStore auditStore) {
        this.proof = Objects.requireNonNull(proof, "proof must not be null").clone();
        if (this.proof.length > DetachedSignedCancellationGrant.MAX_PROOF_BYTES) {
            throw new IllegalArgumentException(
                    "signed cancellation proof exceeds supported bound");
        }
        this.verifier = new SignedCancellationGrantVerifier(policy, clock);
        this.auditStore = Objects.requireNonNull(
                auditStore, "auditStore must not be null");
    }

    @Override
    public ControlAuthorizationDecision authorize(
            String canonicalGoalId,
            MessageEnvelope retainedRequest) throws IOException {
        DetachedSignedCancellationGrant grant;
        try {
            grant = DetachedSignedCancellationGrant.parse(proof);
        } catch (IOException malformed) {
            return denied(SignedCancellationGrantDenial.MALFORMED_PROOF);
        }
        SignedCancellationGrantVerifier.Result result = verifier.verify(
                canonicalGoalId, retainedRequest, grant);
        if (result instanceof SignedCancellationGrantVerifier.Denied denied) {
            return denied(denied.reason());
        }
        SignedCancellationGrantVerifier.VerifiedAuthorization authorization =
                ((SignedCancellationGrantVerifier.Verified) result).authorization();
        Optional<CancellationAuthorizationAuditRecord> existing = auditStore.find(
                authorization.authorizationId());
        CancellationAuthorizationAuditRecord candidate;
        if (existing.isPresent()) {
            candidate = existing.orElseThrow();
            if (!candidate.matchesCurrent(authorization)) {
                throw new IOException(
                        "authorization identity is bound to different verified content");
            }
        } else {
            candidate = CancellationAuthorizationAuditRecord.from(authorization);
        }
        CancellationAuthorizationAuditRecord persisted = auditStore.persist(candidate);
        if (!persisted.equals(candidate) || !persisted.matchesCurrent(authorization)) {
            throw new IOException(
                    "authorization audit store returned mismatched persisted content");
        }
        return new ControlAuthorizationDecision.Approved(
                persisted.authorizationId(),
                persisted.actorId(),
                persisted.goalId(),
                persisted.controlMessageId(),
                ControlSignal.CANCEL,
                persisted.issuedAt());
    }

    private static ControlAuthorizationDecision.Denied denied(
            SignedCancellationGrantDenial reason) {
        return new ControlAuthorizationDecision.Denied(reason.name());
    }
}
