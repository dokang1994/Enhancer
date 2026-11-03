package com.enhancer.model;

/** Closed fail-closed outcomes for protected fixed-binding catalog acquisition. */
public enum ProtectedRemoteCapabilityCatalogFactSourceRefusalReason {
    INSTALLATION_ROOT_UNAVAILABLE_OR_UNSAFE,
    BINDING_UNAVAILABLE_OR_UNSAFE,
    BINDING_OUTSIDE_SIZE_BOUND,
    BINDING_CHANGED_DURING_READ,
    BINDING_INVALID,
    BINDING_CHANGED_DURING_ACQUISITION,
    SOURCE_REFUSED
}
