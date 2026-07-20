package com.enhancer.bus;

import java.io.IOException;

/**
 * Thrown when a spooled transport message cannot be decoded into an exact {@link
 * TransportMessage}: an unrecognized frame, a truncated or oversized body, trailing bytes, a
 * digest mismatch, invalid UTF-8, an unsupported payload or enum value, or content that violates
 * an envelope invariant.
 *
 * <p>It is deliberately distinct from a plain {@link IOException}. A corrupt message stays
 * corrupt, so a caller should dead-letter it; a plain {@code IOException} is a filesystem
 * condition that may be transient and worth retrying.
 */
public final class CorruptedSpooledMessageException extends IOException {
    private static final long serialVersionUID = 1L;

    public CorruptedSpooledMessageException(String message) {
        super(message);
    }

    public CorruptedSpooledMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
