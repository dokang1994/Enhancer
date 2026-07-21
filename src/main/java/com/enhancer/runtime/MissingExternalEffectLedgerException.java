package com.enhancer.runtime;

import java.io.IOException;

public final class MissingExternalEffectLedgerException extends IOException {
    private static final long serialVersionUID = 1L;

    public MissingExternalEffectLedgerException(String goalId) {
        super("External effect ledger is missing for Goal: " + goalId);
    }
}
