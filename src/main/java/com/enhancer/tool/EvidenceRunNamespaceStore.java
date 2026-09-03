package com.enhancer.tool;

import java.io.IOException;

/** Evidence storage that can idempotently materialize one caller-derived run namespace. */
public interface EvidenceRunNamespaceStore extends EvidenceStore {
    void ensureRun(String runId) throws IOException;
}
