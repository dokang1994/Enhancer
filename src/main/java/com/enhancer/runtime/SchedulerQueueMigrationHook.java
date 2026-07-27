package com.enhancer.runtime;

import java.io.IOException;
import java.nio.file.Path;

@FunctionalInterface
interface SchedulerQueueMigrationHook {
    void beforeSourceValidation(Path source) throws IOException;
}
