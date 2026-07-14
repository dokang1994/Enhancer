package com.enhancer.tool;

import java.io.IOException;

public final class MissingEvidenceException extends IOException {
    public MissingEvidenceException(String reference) {
        super("evidence not found: " + reference);
    }
}
