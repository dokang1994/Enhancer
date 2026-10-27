package com.enhancer.model;

import java.util.Objects;

/** Exact evidence query over one validated same-byte candidate snapshot. */
record RemoteCapabilityCatalogEvidenceVerificationRequest(
        String installationIdentity,
        String candidateInputIdentity,
        String evidenceIdentity,
        RemoteCapabilityCatalogCandidateSnapshot candidateSnapshot) {

    RemoteCapabilityCatalogEvidenceVerificationRequest {
        installationIdentity = RemoteCapabilityCatalogPublicationValues.identity(
                installationIdentity, "installationIdentity");
        candidateInputIdentity = RemoteCapabilityCatalogPublicationValues.identity(
                candidateInputIdentity, "candidateInputIdentity");
        evidenceIdentity = RemoteCapabilityCatalogPublicationValues.identity(
                evidenceIdentity, "evidenceIdentity");
        Objects.requireNonNull(candidateSnapshot, "candidateSnapshot must not be null");
    }

    @Override
    public String toString() {
        return "RemoteCapabilityCatalogEvidenceVerificationRequest["
                + "installationIdentity=" + installationIdentity
                + ", candidateInputIdentity=<redacted>"
                + ", evidenceIdentity=" + evidenceIdentity
                + ", candidateSnapshot=<redacted>]";
    }
}
