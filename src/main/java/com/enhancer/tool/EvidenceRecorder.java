package com.enhancer.tool;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public final class EvidenceRecorder {
    private final EvidenceStore store;

    public EvidenceRecorder(EvidenceStore store) {
        this.store = Objects.requireNonNull(store, "store must not be null");
    }

    public VerificationEvidence capture(
            String runId,
            String summary,
            String fullOutput) throws IOException {
        Objects.requireNonNull(fullOutput, "fullOutput must not be null");

        if (fullOutput.length() <= VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS) {
            return VerificationEvidence.capture(summary, fullOutput, Optional.empty());
        }

        VerificationEvidence.capture(summary, fullOutput, Optional.of("pending-validation"));
        StoredEvidence stored = store.persist(runId, fullOutput);
        return VerificationEvidence.capture(summary, fullOutput, Optional.of(stored.reference()));
    }
}
