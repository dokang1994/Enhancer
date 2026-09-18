package com.enhancer.model;

import java.time.Duration;
import java.util.Set;

/** Independently governed actual destination retention behavior. */
public record ModelRetentionFacts(
        Set<ModelRetentionUse> uses,
        Duration maximumDuration) {

    public static final Duration MAXIMUM_SUPPORTED_DURATION = Duration.ofDays(3_650);

    public ModelRetentionFacts {
        uses = ModelOutboundRouteValueChecks.immutableSet(uses, "uses");
        if (maximumDuration == null) {
            throw new NullPointerException("maximumDuration must not be null");
        }
        if (maximumDuration.isNegative()
                || maximumDuration.compareTo(MAXIMUM_SUPPORTED_DURATION) > 0) {
            throw new IllegalArgumentException(
                    "maximumDuration must be non-negative and bounded");
        }
        if (uses.isEmpty() != maximumDuration.isZero()) {
            throw new IllegalArgumentException(
                    "empty retention requires zero duration and retained use requires positive duration");
        }
    }
}
