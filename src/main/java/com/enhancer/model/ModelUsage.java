package com.enhancer.model;

/** Bounded non-negative input and output unit counts reported for one invocation. */
public record ModelUsage(long inputUnits, long outputUnits) {
    public static final long MAX_UNITS = 1_000_000_000L;

    public ModelUsage {
        validateUnits(inputUnits, "inputUnits");
        validateUnits(outputUnits, "outputUnits");
    }

    private static void validateUnits(long units, String fieldName) {
        if (units < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        if (units > MAX_UNITS) {
            throw new IllegalArgumentException(
                    fieldName + " exceeds the supported bound " + MAX_UNITS);
        }
    }
}
