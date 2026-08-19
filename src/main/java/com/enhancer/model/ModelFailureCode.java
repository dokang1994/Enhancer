package com.enhancer.model;

/** The four typed gateway failure conditions RFC-0013 requires at minimum. */
public enum ModelFailureCode {
    PROVIDER_UNAVAILABLE,
    RESPONSE_INVALID,
    BUDGET_EXCEEDED,
    TIMED_OUT
}
