package com.enhancer.model;

import java.util.Objects;

/** Ephemeral result of deterministic model candidate suitability evaluation. */
public sealed interface ModelCandidateSuitabilityDecision
        permits ModelCandidateSuitabilityDecision.Suitable,
                ModelCandidateSuitabilityDecision.Rejected {

    /** Exact eligible admission and candidate identities. */
    record Suitable(
            ModelInvocationAdmissionDecision.Admitted admitted,
            DeterministicFakeModelCandidate candidate)
            implements ModelCandidateSuitabilityDecision {
        public Suitable {
            Objects.requireNonNull(admitted, "admitted must not be null");
            Objects.requireNonNull(candidate, "candidate must not be null");
        }
    }

    /** First fail-closed rejection reason. */
    record Rejected(ModelCandidateSuitabilityRejectionReason reason)
            implements ModelCandidateSuitabilityDecision {
        public Rejected {
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }
}
