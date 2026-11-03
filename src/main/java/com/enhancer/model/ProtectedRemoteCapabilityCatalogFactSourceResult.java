package com.enhancer.model;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Result of one protected fixed-binding capability-catalog acquisition. */
public sealed interface ProtectedRemoteCapabilityCatalogFactSourceResult
        permits ProtectedRemoteCapabilityCatalogFactSourceResult.Acquired,
                ProtectedRemoteCapabilityCatalogFactSourceResult.Refused {
    Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    /** Exact RFC-0031 facts acquired under one stable canonical binding. */
    record Acquired(
            RemoteModelCandidateCapabilityFacts facts,
            String bindingSha256)
            implements ProtectedRemoteCapabilityCatalogFactSourceResult {
        public Acquired {
            Objects.requireNonNull(facts, "facts must not be null");
            Objects.requireNonNull(bindingSha256,
                    "bindingSha256 must not be null");
            if (!SHA_256.matcher(bindingSha256).matches()) {
                throw new IllegalArgumentException(
                        "bindingSha256 must be a lowercase SHA-256 digest");
            }
        }

        @Override
        public String toString() {
            return "Acquired[facts=<redacted>, bindingSha256=<redacted>]";
        }
    }

    /** Fail-closed binding or delegated-source decision. */
    record Refused(
            ProtectedRemoteCapabilityCatalogFactSourceRefusalReason reason,
            Optional<RemoteModelCandidateCapabilityFactSourceRefusalReason> sourceReason)
            implements ProtectedRemoteCapabilityCatalogFactSourceResult {
        public Refused {
            Objects.requireNonNull(reason, "reason must not be null");
            sourceReason = Objects.requireNonNull(
                    sourceReason, "sourceReason must not be null");
            if ((reason == ProtectedRemoteCapabilityCatalogFactSourceRefusalReason
                    .SOURCE_REFUSED) != sourceReason.isPresent()) {
                throw new IllegalArgumentException(
                        "sourceReason must be present exactly for SOURCE_REFUSED");
            }
        }

        @Override
        public String toString() {
            return "Refused[reason=" + reason
                    + ", sourceReason=" + sourceReason + "]";
        }
    }
}
