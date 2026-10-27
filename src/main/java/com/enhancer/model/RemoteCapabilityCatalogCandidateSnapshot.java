package com.enhancer.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Defensive same-byte candidate snapshot for RFC-0032 validation and evidence review. */
final class RemoteCapabilityCatalogCandidateSnapshot {
    private static final int MAX_BYTES = 4 * 1024 * 1024;

    private final byte[] bytes;
    private final RemoteModelCapabilityCatalogCodec.Catalog catalog;
    private final String sha256;

    private RemoteCapabilityCatalogCandidateSnapshot(
            byte[] bytes,
            RemoteModelCapabilityCatalogCodec.Catalog catalog,
            String sha256) {
        this.bytes = bytes.clone();
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.sha256 = Objects.requireNonNull(sha256, "sha256 must not be null");
    }

    static RemoteCapabilityCatalogCandidateSnapshot capture(byte[] input)
            throws Exception {
        Objects.requireNonNull(input, "input must not be null");
        byte[] bytes = input.clone();
        if (bytes.length < 1 || bytes.length > MAX_BYTES) {
            throw new Exception(Failure.OUTSIDE_SIZE_BOUND);
        }
        RemoteModelCapabilityCatalogCodec.DecodeResult decoded =
                RemoteModelCapabilityCatalogCodec.decode(bytes);
        if (!(decoded instanceof RemoteModelCapabilityCatalogCodec.Decoded valid)) {
            throw new Exception(Failure.CATALOG_INVALID);
        }
        return new RemoteCapabilityCatalogCandidateSnapshot(
                bytes, valid.catalog(), sha256(bytes));
    }

    byte[] bytes() {
        return bytes.clone();
    }

    RemoteModelCapabilityCatalogCodec.Catalog catalog() {
        return catalog;
    }

    String catalogIdentity() {
        return catalog.identity();
    }

    String sha256() {
        return sha256;
    }

    @Override
    public String toString() {
        return "RemoteCapabilityCatalogCandidateSnapshot[bytes=<redacted>, "
                + "catalog=<redacted>, sha256=<redacted>]";
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    enum Failure {
        OUTSIDE_SIZE_BOUND,
        CATALOG_INVALID
    }

    static final class Exception extends java.lang.Exception {
        private static final long serialVersionUID = 1L;
        private final Failure failure;

        private Exception(Failure failure) {
            this.failure = failure;
        }

        Failure failure() {
            return failure;
        }
    }
}
