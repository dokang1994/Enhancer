package com.enhancer.run;

import java.io.IOException;

public interface RunRecordStore {
    StoredRunRecord persist(RunRecord record) throws IOException;

    ResolvedRunRecord resolve(String reference) throws IOException;
}
