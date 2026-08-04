package com.enhancer.runtime;

/** Type of authoritative durable fact referenced by a runtime event. */
public enum RuntimeEventReferenceKind {
    RUNTIME_STATE,
    RETRY_DECISION,
    CONTROL_MESSAGE,
    CONTROL_APPLICATION,
    RESULT_MESSAGE,
    RUN_RECORD,
    LEASE_TIMEOUT,
    PROCESS_TIMEOUT,
    EVIDENCE,
    SCHEDULER_QUEUE
}
