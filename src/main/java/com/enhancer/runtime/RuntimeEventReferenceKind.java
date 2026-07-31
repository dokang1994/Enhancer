package com.enhancer.runtime;

/** Type of authoritative durable fact referenced by a runtime event. */
public enum RuntimeEventReferenceKind {
    RUNTIME_STATE,
    RETRY_DECISION,
    CONTROL_MESSAGE,
    RESULT_MESSAGE,
    RUN_RECORD,
    EVIDENCE,
    SCHEDULER_QUEUE
}
