package com.enhancer.tool;

import java.io.IOException;

public interface EvidenceStore {
    String createRun() throws IOException;

    StoredEvidence persist(String runId, String content) throws IOException;

    ResolvedEvidence resolve(String reference) throws IOException;

    EvidenceStoragePolicy storagePolicy();
}
