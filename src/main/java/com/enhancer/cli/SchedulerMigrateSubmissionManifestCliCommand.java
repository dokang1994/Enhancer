package com.enhancer.cli;

import java.nio.file.Path;

record SchedulerMigrateSubmissionManifestCliCommand(
        Path submissionRoot,
        String submissionId) implements CliCommand {
}
