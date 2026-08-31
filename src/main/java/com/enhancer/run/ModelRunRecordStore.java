package com.enhancer.run;

import java.io.IOException;

/** A type-level persistence boundary that cannot resolve legacy RunRecord payloads. */
public interface ModelRunRecordStore {

    StoredRunRecord persistModel(ModelRunRecord record) throws IOException;

    default StoredRunRecord persistModel(
            String recordId,
            ModelRunRecord record) throws IOException {
        throw new IOException(
                "this Model RunRecord store does not support caller-supplied identities");
    }

    ResolvedModelRunRecord resolveModel(String reference) throws IOException;
}
