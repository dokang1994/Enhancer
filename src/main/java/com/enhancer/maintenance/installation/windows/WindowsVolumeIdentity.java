package com.enhancer.maintenance.installation.windows;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable opaque volume identity supplied by an injected Windows gateway. */
public record WindowsVolumeIdentity(String value) {
    private static final Pattern VALUE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");

    public WindowsVolumeIdentity {
        value = Objects.requireNonNull(value, "value must not be null");
        if (!VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException("volume identity is outside supported bounds");
        }
    }
}
