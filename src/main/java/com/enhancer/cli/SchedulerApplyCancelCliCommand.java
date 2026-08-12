package com.enhancer.cli;

import java.nio.file.Path;
import java.util.Optional;

record SchedulerApplyCancelCliCommand(
        Path runtimeRoot,
        String goalId,
        String controlMessageId,
        Path proofFile,
        Path authorizationAuditRoot,
        Optional<RuntimeEventPublicationCliConfiguration> runtimeEventPublication)
        implements CliCommand {
}
