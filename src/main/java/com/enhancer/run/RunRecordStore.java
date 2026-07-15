package com.enhancer.run;

import java.io.IOException;
import java.util.List;

public interface RunRecordStore {
    StoredRunRecord persist(RunRecord record) throws IOException;

    ResolvedRunRecord resolve(String reference) throws IOException;

    List<String> references() throws IOException;
}
