package com.enhancer.run;

import java.io.IOException;

public final class MissingRunRecordException extends IOException {
    private static final long serialVersionUID = 1L;

    public MissingRunRecordException(String reference) {
        super("run record does not exist: " + reference);
    }
}
