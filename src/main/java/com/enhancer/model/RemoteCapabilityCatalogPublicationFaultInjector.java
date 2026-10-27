package com.enhancer.model;

/** Test-owned crash-boundary seam; production composition remains absent. */
interface RemoteCapabilityCatalogPublicationFaultInjector {
    void check(Point point) throws Fault;

    enum Point {
        AFTER_CANDIDATE_SNAPSHOT_READ,
        AFTER_CATALOG_PUBLICATION,
        AFTER_BINDING_PUBLICATION
    }

    final class Fault extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
