package com.enhancer.cli;

final class CliUsageException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    CliUsageException(String message) {
        super(message);
    }

    CliUsageException(String message, Throwable cause) {
        super(message, cause);
    }
}
