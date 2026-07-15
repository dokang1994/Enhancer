package com.enhancer.cli;

import java.nio.file.Path;

record RunCliCommand(
        Path projectRoot,
        String taskId,
        String targetPath,
        String expectedSha256,
        Path evidenceRoot,
        Path runRecordRoot) implements CliCommand {
}
