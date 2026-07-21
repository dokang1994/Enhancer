package com.enhancer.runtime;

import java.io.IOException;

/** Durable boundary for one current external-effect ledger snapshot. */
public interface ExternalEffectLedgerStore {
    void create(ExternalEffectLedgerState initialState) throws IOException;

    void update(ExternalEffectLedgerState nextState) throws IOException;

    ExternalEffectLedgerState resolve(String goalId) throws IOException;
}
