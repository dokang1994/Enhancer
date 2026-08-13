package com.enhancer.maintenance.installation.windows;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable volume-plus-file identity supplied by an injected Windows gateway. */
public record WindowsFileIdentity(WindowsVolumeIdentity volume, String fileId) {
    private static final Pattern FILE_ID = Pattern.compile("[0-9a-f]{32}");

    public WindowsFileIdentity {
        volume = Objects.requireNonNull(volume, "volume must not be null");
        fileId = Objects.requireNonNull(fileId, "fileId must not be null");
        if (!FILE_ID.matcher(fileId).matches()) {
            throw new IllegalArgumentException("fileId must be canonical lowercase hex");
        }
    }
}
