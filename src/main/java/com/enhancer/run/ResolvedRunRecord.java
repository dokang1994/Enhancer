package com.enhancer.run;

import java.util.Objects;

public record ResolvedRunRecord(
        StoredRunRecord metadata,
        RunRecord record) {

    public ResolvedRunRecord {
        Objects.requireNonNull(metadata, "metadata must not be null");
        Objects.requireNonNull(record, "record must not be null");
    }
}
