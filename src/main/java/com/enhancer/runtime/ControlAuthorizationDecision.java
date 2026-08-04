package com.enhancer.runtime;

import com.enhancer.bus.ControlSignal;
import java.time.Instant;
import java.util.Objects;

/** Typed result from a trusted Gate 12 control-request authorizer. */
public sealed interface ControlAuthorizationDecision {
    int MAX_ACTOR_CHARACTERS = 256;
    int MAX_DENIAL_REASON_CHARACTERS = 256;

    record Approved(
            String authorizationId,
            String actorId,
            String goalId,
            String controlMessageId,
            ControlSignal signal,
            Instant authorizedAt) implements ControlAuthorizationDecision {
        public Approved {
            authorizationId = RuntimeIdentity.canonicalUuid(
                    authorizationId, "authorizationId");
            actorId = bounded(actorId, "actorId", MAX_ACTOR_CHARACTERS);
            goalId = RuntimeIdentity.canonicalUuid(goalId, "goalId");
            controlMessageId = RuntimeIdentity.canonicalUuid(
                    controlMessageId, "controlMessageId");
            Objects.requireNonNull(signal, "signal must not be null");
            if (signal != ControlSignal.CANCEL) {
                throw new IllegalArgumentException(
                        "this authorization contract permits only CANCEL");
            }
            Objects.requireNonNull(authorizedAt, "authorizedAt must not be null");
        }
    }

    record Denied(String reason) implements ControlAuthorizationDecision {
        public Denied {
            reason = bounded(reason, "reason", MAX_DENIAL_REASON_CHARACTERS);
        }
    }

    private static String bounded(String value, String field, int maximum) {
        Objects.requireNonNull(value, field + " must not be null");
        String checked = value.strip();
        if (checked.isEmpty() || checked.length() > maximum) {
            throw new IllegalArgumentException(
                    field + " must be non-blank and at most " + maximum + " characters");
        }
        return checked;
    }
}
