package com.enhancer.model;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/** Immutable pure representation of one canonical installed catalog binding. */
final class RemoteCapabilityCatalogBinding {
    private final Path catalogPath;
    private final String catalogSha256;
    private final byte[] canonicalBytes;
    private final String bindingSha256;

    RemoteCapabilityCatalogBinding(
            Path catalogPath,
            String catalogSha256,
            byte[] canonicalBytes,
            String bindingSha256) {
        this.catalogPath = Objects.requireNonNull(catalogPath,
                "catalogPath must not be null");
        this.catalogSha256 = Objects.requireNonNull(catalogSha256,
                "catalogSha256 must not be null");
        this.canonicalBytes = Objects.requireNonNull(canonicalBytes,
                "canonicalBytes must not be null").clone();
        this.bindingSha256 = Objects.requireNonNull(bindingSha256,
                "bindingSha256 must not be null");
    }

    Path catalogPath() {
        return catalogPath;
    }

    String catalogSha256() {
        return catalogSha256;
    }

    byte[] canonicalBytes() {
        return canonicalBytes.clone();
    }

    String bindingSha256() {
        return bindingSha256;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RemoteCapabilityCatalogBinding binding
                && catalogPath.equals(binding.catalogPath)
                && catalogSha256.equals(binding.catalogSha256)
                && Arrays.equals(canonicalBytes, binding.canonicalBytes)
                && bindingSha256.equals(binding.bindingSha256);
    }

    @Override
    public int hashCode() {
        return Objects.hash(catalogPath, catalogSha256,
                Arrays.hashCode(canonicalBytes), bindingSha256);
    }

    @Override
    public String toString() {
        return "RemoteCapabilityCatalogBinding[catalogPath=<redacted>, "
                + "catalogSha256=<redacted>, bindingSha256=<redacted>]";
    }
}
