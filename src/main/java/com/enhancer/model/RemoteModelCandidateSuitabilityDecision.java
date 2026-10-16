package com.enhancer.model;

import java.util.Objects;

/** Closed result of pure governed remote candidate suitability evaluation. */
public sealed interface RemoteModelCandidateSuitabilityDecision
        permits RemoteModelCandidateSuitabilityDecision.Suitable,
                RemoteModelCandidateSuitabilityDecision.Refused {

    record Suitable(RemoteModelCandidateSuitability suitability)
            implements RemoteModelCandidateSuitabilityDecision {
        public Suitable {
            Objects.requireNonNull(suitability, "suitability must not be null");
        }
    }

    record Refused(RemoteModelCandidateSuitabilityRefusalReason reason)
            implements RemoteModelCandidateSuitabilityDecision {
        public Refused {
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }
}
