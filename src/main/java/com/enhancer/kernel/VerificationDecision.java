package com.enhancer.kernel;

import java.util.Objects;
import java.util.Set;

public record VerificationDecision(
        VerificationStatus status,
        VerificationCode code,
        String reason) {

    public static final int MAX_REASON_CHARACTERS = 512;

    private static final Set<VerificationCode> REJECTED_CODES = Set.of(
            VerificationCode.TOOL_RESULT_FAILURE,
            VerificationCode.REQUEST_RESULT_MISMATCH,
            VerificationCode.CORRUPTED_EVIDENCE,
            VerificationCode.INVALID_EVIDENCE,
            VerificationCode.CONTENT_MISMATCH);
    private static final Set<VerificationCode> UNVERIFIED_CODES = Set.of(
            VerificationCode.MISSING_CONTENT_DIGEST,
            VerificationCode.MISSING_EVIDENCE,
            VerificationCode.EVIDENCE_UNAVAILABLE);

    public VerificationDecision {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        if (reason.length() > MAX_REASON_CHARACTERS) {
            throw new IllegalArgumentException(
                    "reason must not exceed " + MAX_REASON_CHARACTERS + " characters");
        }

        boolean consistent = switch (status) {
            case VERIFIED -> code == VerificationCode.VERIFIED;
            case REJECTED -> REJECTED_CODES.contains(code);
            case UNVERIFIED -> UNVERIFIED_CODES.contains(code);
            case NOT_PERFORMED -> code == VerificationCode.WORKER_NOT_AWAITING_VERIFICATION;
        };
        if (!consistent) {
            throw new IllegalArgumentException(
                    "verification status " + status + " contradicts code " + code);
        }
    }

    public static VerificationDecision verified(String reason) {
        return new VerificationDecision(
                VerificationStatus.VERIFIED,
                VerificationCode.VERIFIED,
                reason);
    }

    public static VerificationDecision rejected(VerificationCode code, String reason) {
        return new VerificationDecision(VerificationStatus.REJECTED, code, reason);
    }

    public static VerificationDecision unverified(VerificationCode code, String reason) {
        return new VerificationDecision(VerificationStatus.UNVERIFIED, code, reason);
    }

    public static VerificationDecision notPerformed(String reason) {
        return new VerificationDecision(
                VerificationStatus.NOT_PERFORMED,
                VerificationCode.WORKER_NOT_AWAITING_VERIFICATION,
                reason);
    }
}
