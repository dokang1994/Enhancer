package com.enhancer.run;

import java.util.Objects;

/** One independently validated model record and its immutable storage metadata. */
public record ResolvedModelRunRecord(
        StoredRunRecord metadata,
        ModelRunRecord record) {

    public ResolvedModelRunRecord {
        Objects.requireNonNull(metadata, "metadata must not be null");
        Objects.requireNonNull(record, "record must not be null");
    }
}
