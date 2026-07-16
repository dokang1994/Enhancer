package com.enhancer.run;

import java.io.IOException;
import java.util.List;

public interface RunRecordStore {
    int MAX_REFERENCE_WINDOW = 4096;

    StoredRunRecord persist(RunRecord record) throws IOException;

    ResolvedRunRecord resolve(String reference) throws IOException;

    List<String> references() throws IOException;

    List<String> recentReferences(int limit) throws IOException;
}
