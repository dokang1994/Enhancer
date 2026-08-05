package com.enhancer.cli;

import java.nio.file.Path;

record RuntimeEventReadCliCommand(
        Path runtimeEventRoot,
        Path publicationRoot,
        String publicationFile) implements CliCommand {
}
