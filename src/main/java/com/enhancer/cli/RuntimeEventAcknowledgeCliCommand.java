package com.enhancer.cli;

import java.nio.file.Path;

record RuntimeEventAcknowledgeCliCommand(
        Path runtimeEventRoot,
        Path publicationRoot,
        String publicationFile) implements CliCommand {
}
