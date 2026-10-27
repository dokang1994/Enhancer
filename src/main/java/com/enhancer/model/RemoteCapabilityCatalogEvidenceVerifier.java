package com.enhancer.model;

/** Uncomposed port for independent evidence approval of exact candidate bytes. */
interface RemoteCapabilityCatalogEvidenceVerifier {
    RemoteCapabilityCatalogEvidenceDecision verify(
            RemoteCapabilityCatalogEvidenceVerificationRequest request);
}
