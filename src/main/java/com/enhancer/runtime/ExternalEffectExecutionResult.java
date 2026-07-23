package com.enhancer.runtime;

import com.enhancer.tool.ResolvedEvidence;
import java.util.Objects;

/** Terminal external-effect record with resolved bound evidence and invocation provenance. */
public record ExternalEffectExecutionResult(
        ExternalEffectRecord record,
        ResolvedEvidence evidence,
        boolean adapterInvoked) {

    public ExternalEffectExecutionResult {
        Objects.requireNonNull(record, "record must not be null");
        Objects.requireNonNull(evidence, "evidence must not be null");
        if (!record.status().isTerminal()) {
            throw new IllegalArgumentException(
                    "external effect execution result must be terminal");
        }
        ExternalEffectOutcomeEvidence binding = record.outcomeEvidence()
                .orElseThrow();
        if (!binding.reference().equals(evidence.metadata().reference())
                || !binding.sha256().equals(evidence.metadata().sha256())) {
            throw new IllegalArgumentException(
                    "resolved evidence does not match terminal effect binding");
        }
    }
}
