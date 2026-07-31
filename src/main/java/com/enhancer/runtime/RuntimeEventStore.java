package com.enhancer.runtime;

import java.io.IOException;

/** Append-only persistence port for per-Goal runtime-event streams. */
public interface RuntimeEventStore {
    RuntimeEventAppendResult append(RuntimeEvent event) throws IOException;

    RuntimeEventStream resolve(String goalId) throws IOException;
}
