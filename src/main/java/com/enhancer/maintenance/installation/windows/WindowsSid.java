package com.enhancer.maintenance.installation.windows;

import java.util.Objects;
import java.util.regex.Pattern;

/** Canonical textual Windows security identifier, without account-name authority. */
public record WindowsSid(String canonicalValue) {
    private static final Pattern CANONICAL = Pattern.compile("S-(?:0|[1-9][0-9]*)-(?:0|[1-9][0-9]*)(?:-(?:0|[1-9][0-9]*))+");

    public WindowsSid {
        canonicalValue = Objects.requireNonNull(canonicalValue, "canonicalValue must not be null");
        if (canonicalValue.length() > 184 || !CANONICAL.matcher(canonicalValue).matches()) {
            throw new IllegalArgumentException("SID must be canonical");
        }
    }
}
