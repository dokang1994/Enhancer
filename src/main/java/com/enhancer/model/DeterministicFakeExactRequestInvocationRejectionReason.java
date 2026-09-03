package com.enhancer.model;

/** Closed deterministic first-match pre-gateway invocation rejection reasons. */
public enum DeterministicFakeExactRequestInvocationRejectionReason {
    EXECUTION_POLICY_TOOL_NOT_ALLOWED,
    GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY,
    CANCELLATION_REQUESTED
}
