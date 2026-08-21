package com.enhancer.model;

import java.util.Objects;

/** Ephemeral result of the pure RFC-0016 authority-intersection evaluation. */
public sealed interface ModelInvocationAdmissionDecision
        permits ModelInvocationAdmissionDecision.Admitted,
                ModelInvocationAdmissionDecision.Rejected {

    /** Local eligibility retaining the exact already complete request/profile pair. */
    record Admitted(ProfiledModelRequest profiledRequest)
            implements ModelInvocationAdmissionDecision {
        public Admitted {
            Objects.requireNonNull(profiledRequest, "profiledRequest must not be null");
        }
    }

    /** Fail-closed result retaining the first matching rejection reason. */
    record Rejected(ModelInvocationRejectionReason reason)
            implements ModelInvocationAdmissionDecision {
        public Rejected {
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }
}
