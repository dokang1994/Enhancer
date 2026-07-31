package com.enhancer.runtime;

import java.util.Objects;
import java.util.Optional;

/** Bounded opaque reference to one authoritative durable source fact. */
public record RuntimeEventReference(
        RuntimeEventReferenceKind kind,
        String reference,
        Optional<String> sha256) {

    public static final int MAX_REFERENCE_CHARACTERS = 1024;

    public RuntimeEventReference {
        Objects.requireNonNull(kind, "kind must not be null");
        reference = RuntimeEventContractSupport.bounded(
                reference,
                "reference",
                MAX_REFERENCE_CHARACTERS);
        sha256 = RuntimeEventContractSupport.optionalSha256(
                sha256,
                "sha256");
    }
}
