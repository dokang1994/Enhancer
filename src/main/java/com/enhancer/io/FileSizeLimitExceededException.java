package com.enhancer.io;

import java.io.IOException;

public final class FileSizeLimitExceededException extends IOException {
    private static final long serialVersionUID = 1L;

    public FileSizeLimitExceededException(long maximumBytes) {
        super("file content exceeds the " + maximumBytes + " byte limit");
    }
}
