package com.enhancer.maintenance.installation.windows;

import java.util.Objects;

/** One exact group SID and the token attributes that can affect authorization. */
public record WindowsTokenGroupEvidence(WindowsSid sid, boolean enabled, boolean denyOnly) {
    public WindowsTokenGroupEvidence {
        sid = Objects.requireNonNull(sid, "sid must not be null");
        if (enabled && denyOnly) {
            throw new IllegalArgumentException("group cannot be enabled and deny-only");
        }
    }
}
