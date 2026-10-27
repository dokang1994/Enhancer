package com.enhancer.model;

import java.util.Objects;
import java.util.Optional;

/** Authority-minimal intent for one governed catalog installation or rotation. */
public record RemoteCapabilityCatalogPublicationIntent(
        RemoteCapabilityCatalogPublicationOperation operation,
        String installationIdentity,
        String candidateInputIdentity,
        String evidenceIdentity,
        Optional<String> expectedCurrentBindingSha256) {

    public RemoteCapabilityCatalogPublicationIntent {
        Objects.requireNonNull(operation, "operation must not be null");
        installationIdentity = RemoteCapabilityCatalogPublicationValues.identity(
                installationIdentity, "installationIdentity");
        candidateInputIdentity = RemoteCapabilityCatalogPublicationValues.identity(
                candidateInputIdentity, "candidateInputIdentity");
        evidenceIdentity = RemoteCapabilityCatalogPublicationValues.identity(
                evidenceIdentity, "evidenceIdentity");
        Objects.requireNonNull(expectedCurrentBindingSha256,
                "expectedCurrentBindingSha256 must not be null");
        expectedCurrentBindingSha256 = expectedCurrentBindingSha256.map(digest ->
                RemoteCapabilityCatalogPublicationValues.sha256(
                        digest, "expectedCurrentBindingSha256"));
        if (operation == RemoteCapabilityCatalogPublicationOperation.INSTALL
                && expectedCurrentBindingSha256.isPresent()) {
            throw new IllegalArgumentException(
                    "INSTALL must not supply an expected current binding digest");
        }
        if (operation == RemoteCapabilityCatalogPublicationOperation.ROTATE
                && expectedCurrentBindingSha256.isEmpty()) {
            throw new IllegalArgumentException(
                    "ROTATE requires an expected current binding digest");
        }
    }

    @Override
    public String toString() {
        return "RemoteCapabilityCatalogPublicationIntent[operation=" + operation
                + ", installationIdentity=" + installationIdentity
                + ", evidenceIdentity=" + evidenceIdentity
                + ", candidateInputIdentity=<redacted>"
                + ", expectedCurrentBindingSha256=<redacted>]";
    }
}
