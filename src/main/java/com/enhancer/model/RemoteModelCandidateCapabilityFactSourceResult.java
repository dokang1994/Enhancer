package com.enhancer.model;

import java.util.Objects;

/** Result of one pinned capability-catalog acquisition attempt. */
public sealed interface RemoteModelCandidateCapabilityFactSourceResult
        permits RemoteModelCandidateCapabilityFactSourceResult.Acquired,
                RemoteModelCandidateCapabilityFactSourceResult.Refused {

    /** Successfully acquired immutable candidate facts. */
    record Acquired(RemoteModelCandidateCapabilityFacts facts)
            implements RemoteModelCandidateCapabilityFactSourceResult {
        public Acquired {
            Objects.requireNonNull(facts, "facts must not be null");
        }
    }

    /** Fail-closed acquisition outcome with a closed reason. */
    record Refused(RemoteModelCandidateCapabilityFactSourceRefusalReason reason)
            implements RemoteModelCandidateCapabilityFactSourceResult {
        public Refused {
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }
}
