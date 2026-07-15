package com.enhancer.cli;

import java.nio.file.Path;

record ReplayCliCommand(Path runRecordRoot, String reference) implements CliCommand {
}
