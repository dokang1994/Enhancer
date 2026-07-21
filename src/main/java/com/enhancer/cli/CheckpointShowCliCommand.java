package com.enhancer.cli;

import java.nio.file.Path;

record CheckpointShowCliCommand(Path projectRoot) implements CliCommand {
}
