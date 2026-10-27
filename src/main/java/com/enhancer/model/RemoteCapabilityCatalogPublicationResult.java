package com.enhancer.model;

import java.util.Objects;

/** Closed non-authoritative observation of one RFC-0032 publication attempt. */
public sealed interface RemoteCapabilityCatalogPublicationResult permits
        RemoteCapabilityCatalogPublicationResult.Succeeded,
        RemoteCapabilityCatalogPublicationResult.Refused {

    record Succeeded(
            RemoteCapabilityCatalogPublicationOperation operation,
            RemoteCapabilityCatalogPublicationStatus status,
            String installationIdentity,
            String evidenceIdentity,
            String catalogIdentity,
            String catalogSha256,
            String bindingSha256)
            implements RemoteCapabilityCatalogPublicationResult {

        public Succeeded {
            Objects.requireNonNull(operation, "operation must not be null");
            Objects.requireNonNull(status, "status must not be null");
            installationIdentity = RemoteCapabilityCatalogPublicationValues.identity(
                    installationIdentity, "installationIdentity");
            evidenceIdentity = RemoteCapabilityCatalogPublicationValues.identity(
                    evidenceIdentity, "evidenceIdentity");
            catalogIdentity = RemoteCapabilityCatalogPublicationValues.identity(
                    catalogIdentity, "catalogIdentity");
            catalogSha256 = RemoteCapabilityCatalogPublicationValues.sha256(
                    catalogSha256, "catalogSha256");
            bindingSha256 = RemoteCapabilityCatalogPublicationValues.sha256(
                    bindingSha256, "bindingSha256");
            if (status == RemoteCapabilityCatalogPublicationStatus.INSTALLED
                    && operation != RemoteCapabilityCatalogPublicationOperation.INSTALL) {
                throw new IllegalArgumentException(
                        "INSTALLED requires an INSTALL operation");
            }
            if (status == RemoteCapabilityCatalogPublicationStatus.ROTATED
                    && operation != RemoteCapabilityCatalogPublicationOperation.ROTATE) {
                throw new IllegalArgumentException(
                        "ROTATED requires a ROTATE operation");
            }
        }

        @Override
        public String toString() {
            return "Succeeded[operation=" + operation
                    + ", status=" + status
                    + ", installationIdentity=" + installationIdentity
                    + ", evidenceIdentity=" + evidenceIdentity
                    + ", catalogIdentity=" + catalogIdentity
                    + ", catalogSha256=<redacted>, bindingSha256=<redacted>]";
        }
    }

    record Refused(
            RemoteCapabilityCatalogPublicationOperation operation,
            RemoteCapabilityCatalogPublicationRefusalReason reason,
            String installationIdentity,
            String evidenceIdentity)
            implements RemoteCapabilityCatalogPublicationResult {

        public Refused {
            Objects.requireNonNull(operation, "operation must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
            installationIdentity = RemoteCapabilityCatalogPublicationValues.identity(
                    installationIdentity, "installationIdentity");
            evidenceIdentity = RemoteCapabilityCatalogPublicationValues.identity(
                    evidenceIdentity, "evidenceIdentity");
        }

        @Override
        public String toString() {
            return "Refused[operation=" + operation
                    + ", reason=" + reason
                    + ", installationIdentity=" + installationIdentity
                    + ", evidenceIdentity=" + evidenceIdentity + "]";
        }
    }
}
