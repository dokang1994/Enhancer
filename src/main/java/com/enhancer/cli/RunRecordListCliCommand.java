package com.enhancer.cli;

import java.nio.file.Path;
import java.util.Objects;

record RunRecordListCliCommand(
        Path runRecordRoot,
        int limit) implements CliCommand {
    static final int MAX_REFERENCES = 48;

    RunRecordListCliCommand {
        Objects.requireNonNull(
                runRecordRoot, "runRecordRoot must not be null");
        runRecordRoot = runRecordRoot.toAbsolutePath().normalize();
        if (limit < 1 || limit > MAX_REFERENCES) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and " + MAX_REFERENCES);
        }
    }
}
