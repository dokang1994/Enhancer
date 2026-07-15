package com.enhancer.run;

import java.io.IOException;

public final class MissingRunRecordException extends IOException {
    public MissingRunRecordException(String reference) {
        super("run record does not exist: " + reference);
    }
}
