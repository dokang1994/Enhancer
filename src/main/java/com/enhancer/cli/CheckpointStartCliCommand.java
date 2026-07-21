package com.enhancer.cli;

import java.nio.file.Path;
import java.util.List;

record CheckpointStartCliCommand(
        Path projectRoot,
        String step,
        String nextAction,
        List<String> artifacts) implements CliCommand {
}
