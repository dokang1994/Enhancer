package com.enhancer.brain;

public enum GraphElementFreshness {
    CURRENT,
    STALE,
    SOURCE_MISSING;

    public boolean rebuildRequired() {
        return this != CURRENT;
    }
}
