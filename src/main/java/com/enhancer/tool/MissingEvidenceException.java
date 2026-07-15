package com.enhancer.tool;

import java.io.IOException;

public final class MissingEvidenceException extends IOException {
    private static final long serialVersionUID = 1L;

    public MissingEvidenceException(String reference) {
        super("evidence not found: " + reference);
    }
}
