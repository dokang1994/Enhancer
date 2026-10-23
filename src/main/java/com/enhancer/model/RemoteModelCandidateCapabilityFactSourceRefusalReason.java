package com.enhancer.model;

/** Closed fail-closed outcomes for pinned capability-catalog acquisition. */
public enum RemoteModelCandidateCapabilityFactSourceRefusalReason {
    CATALOG_UNAVAILABLE_OR_UNSAFE,
    CATALOG_OUTSIDE_SIZE_BOUND,
    CATALOG_CHANGED_DURING_READ,
    CATALOG_PIN_MISMATCH,
    CATALOG_INVALID_UTF8,
    CATALOG_SCHEMA_UNSUPPORTED,
    CATALOG_NOT_CANONICAL,
    CATALOG_CONTENT_INVALID,
    RECORD_NOT_FOUND,
    RECORD_STALE
}
