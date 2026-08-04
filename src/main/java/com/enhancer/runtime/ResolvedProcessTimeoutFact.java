package com.enhancer.runtime;

import java.util.Objects;

/** One validated process-timeout fact plus its stable reference and semantic digest. */
public record ResolvedProcessTimeoutFact(
        ProcessTimeoutFact fact,
        String reference,
        String sha256) {

    public ResolvedProcessTimeoutFact {
        Objects.requireNonNull(fact, "fact must not be null");
        reference = RuntimeEventContractSupport.bounded(
                reference,
                "reference",
                RuntimeEventReference.MAX_REFERENCE_CHARACTERS);
        if (!fact.reference().equals(reference)) {
            throw new IllegalArgumentException(
                    "process timeout reference does not match its fact");
        }
        sha256 = RuntimeEventContractSupport.sha256(sha256, "sha256");
    }
}
