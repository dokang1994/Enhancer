package com.enhancer.cli;

import java.nio.file.Path;

/** Exact four-locator input for manifest-authorized typed ModelWork receive. */
record DeterministicFakeModelReceiveCliCommand(
        Path transportSpoolRoot,
        String messageFile,
        Path submissionRoot,
        Path queueRoot) implements CliCommand {
}
