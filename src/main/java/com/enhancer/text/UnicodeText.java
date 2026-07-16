package com.enhancer.text;

import java.util.Objects;

/**
 * UTF-16 length bounding that never cuts between the two code units of a surrogate pair.
 */
public final class UnicodeText {
    private UnicodeText() {
    }

    public static String prefix(String value, int maximumCharacters) {
        Objects.requireNonNull(value, "value must not be null");
        requireMaximum(maximumCharacters);
        if (value.length() <= maximumCharacters) {
            return value;
        }
        int end = maximumCharacters;
        if (end > 0
                && end < value.length()
                && Character.isHighSurrogate(value.charAt(end - 1))
                && Character.isLowSurrogate(value.charAt(end))) {
            end--;
        }
        return value.substring(0, end);
    }

    public static String suffix(String value, int maximumCharacters) {
        Objects.requireNonNull(value, "value must not be null");
        requireMaximum(maximumCharacters);
        if (value.length() <= maximumCharacters) {
            return value;
        }
        int start = value.length() - maximumCharacters;
        if (start > 0
                && start < value.length()
                && Character.isLowSurrogate(value.charAt(start))
                && Character.isHighSurrogate(value.charAt(start - 1))) {
            start++;
        }
        return value.substring(start);
    }

    private static void requireMaximum(int maximumCharacters) {
        if (maximumCharacters < 0) {
            throw new IllegalArgumentException(
                    "maximumCharacters must not be negative");
        }
    }
}
