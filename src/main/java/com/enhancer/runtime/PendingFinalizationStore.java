package com.enhancer.runtime;

import java.io.IOException;
import java.util.Optional;

/**
 * Durable boundary for the single-worker cycle-intent checkpoint. A single worker means at most
 * one pending cycle globally, so the store holds at most one record.
 */
public interface PendingFinalizationStore {
    /** Writes or overwrites the single intent. */
    void record(PendingFinalization pending) throws IOException;

    /** Returns the single intent, if any. */
    Optional<PendingFinalization> findPending() throws IOException;

    /** Removes the intent; idempotent. */
    void clear() throws IOException;
}
