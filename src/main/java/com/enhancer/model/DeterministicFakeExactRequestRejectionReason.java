package com.enhancer.model;

/** Closed deterministic first-match exact-request budget rejection reasons. */
public enum DeterministicFakeExactRequestRejectionReason {
    MALFORMED_PROMPT,
    INPUT_TOKEN_BUDGET_EXCEEDED,
    PREDICTED_RESPONSE_UTF16_LENGTH_BUDGET_EXCEEDED,
    PREDICTED_OUTPUT_TOKEN_BUDGET_EXCEEDED,
    PREDICTED_TOTAL_TOKEN_BUDGET_EXCEEDED
}
