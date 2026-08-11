package com.enhancer.runtime;

/** Stable bounded reasons why a detached cancellation grant cannot authorize. */
enum SignedCancellationGrantDenial {
    MALFORMED_PROOF,
    UNSUPPORTED_PROOF,
    UNTRUSTED_ISSUER_OR_KEY,
    INVALID_SIGNATURE,
    TARGET_MISMATCH,
    REQUEST_MISMATCH,
    POLICY_MISMATCH,
    SUBJECT_NOT_AUTHORIZED,
    NOT_YET_VALID,
    EXPIRED,
    LIFETIME_EXCEEDED,
    KEY_NOT_VALID,
    KEY_REVOKED
}
