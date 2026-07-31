package com.enhancer.runtime;

/** Closed runtime-event-v1 taxonomy. Runtime events report durable facts and grant no authority. */
public enum RuntimeEventKind {
    RETRY_DECISION_RECORDED,
    RETRY_STARTED,
    STAGNATION_DETECTED,
    TIMEOUT_DETECTED,
    CANCELLATION_REQUEST_RECORDED,
    CANCELLATION_APPLIED,
    VERIFICATION_RECORDED,
    WORK_ITEM_TERMINATED
}
