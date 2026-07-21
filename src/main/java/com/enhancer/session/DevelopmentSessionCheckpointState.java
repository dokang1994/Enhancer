package com.enhancer.session;

/** Durable execution position of one repository development session. */
public enum DevelopmentSessionCheckpointState {
    STEP_PENDING,
    STEP_SUCCEEDED,
    STEP_FAILED,
    STABLE
}
