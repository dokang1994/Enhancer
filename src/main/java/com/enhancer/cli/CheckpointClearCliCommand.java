package com.enhancer.cli;

import java.nio.file.Path;

record CheckpointClearCliCommand(
        Path projectRoot,
        String runId,
        long expectedRevision) implements CliCommand {
}
