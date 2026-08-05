package com.enhancer.runtime;

/** Durable state of one explicitly acknowledged runtime-event publication point. */
public enum RuntimeEventPointAcknowledgementStatus {
    ACKNOWLEDGED,
    ALREADY_ACKNOWLEDGED
}
