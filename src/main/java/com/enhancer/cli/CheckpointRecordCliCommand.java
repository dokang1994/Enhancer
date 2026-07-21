package com.enhancer.cli;

import com.enhancer.session.DevelopmentSessionCheckpointState;
import java.nio.file.Path;
import java.util.List;

record CheckpointRecordCliCommand(
        Path projectRoot,
        String runId,
        long expectedRevision,
        DevelopmentSessionCheckpointState state,
        String step,
        String nextAction,
        List<String> artifacts,
        List<String> evidenceReferences) implements CliCommand {
}
