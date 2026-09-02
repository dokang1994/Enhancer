package com.enhancer.model;

import java.util.Objects;

/** Pure fake-only Unicode-scalar counting and checked response algebra. */
public final class DeterministicFakeTokenCounter {

    private static final long FIXED_RESPONSE_TOKENS = 152L;

    /** Counts well-formed Unicode scalar values exactly as supplied. */
    public long count(String value) {
        Objects.requireNonNull(value, "value must not be null");

        long count = 0L;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length()
                        || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    throw malformedSurrogate(index);
                }
                index++;
            } else if (Character.isLowSurrogate(current)) {
                throw malformedSurrogate(index);
            }
            count = Math.addExact(count, 1L);
        }
        return count;
    }

    static long responseUtf16Length(long promptUtf16Length) {
        requireNonNegative(promptUtf16Length, "promptUtf16Length");
        return responseCount(promptUtf16Length, promptUtf16Length);
    }

    static long responseTokenCount(long promptUtf16Length, long promptTokenCount) {
        requireNonNegative(promptUtf16Length, "promptUtf16Length");
        requireNonNegative(promptTokenCount, "promptTokenCount");
        return responseCount(promptUtf16Length, promptTokenCount);
    }

    private static long responseCount(long promptUtf16Length, long promptUnits) {
        return Math.addExact(
                Math.addExact(promptUnits, FIXED_RESPONSE_TOKENS),
                decimalDigitCount(promptUtf16Length));
    }

    private static long decimalDigitCount(long value) {
        long digits = 1L;
        while (value >= 10L) {
            value /= 10L;
            digits = Math.addExact(digits, 1L);
        }
        return digits;
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }

    private static IllegalArgumentException malformedSurrogate(int index) {
        return new IllegalArgumentException(
                "value contains a malformed surrogate at UTF-16 index " + index);
    }
}
