package com.enhancer.model;

/** Closed first-match reasons why one profiled model request is not locally admissible. */
public enum ModelInvocationRejectionReason {
    TASK_TOOL_NOT_ALLOWED,
    EXECUTION_POLICY_TOOL_NOT_ALLOWED,
    REQUIRED_CAPABILITY_MISMATCH,
    GATEWAY_TIMEOUT_NOT_WITHIN_EXECUTION_POLICY,
    OUTBOUND_POLICY_REQUIRED
}
