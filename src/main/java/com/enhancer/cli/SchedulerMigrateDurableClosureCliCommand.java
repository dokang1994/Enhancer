package com.enhancer.cli;

import java.nio.file.Path;
import java.util.List;

record SchedulerMigrateDurableClosureCliCommand(
        Path fenceFile,
        String expectedFenceSha256,
        Path manifestRoot,
        List<String> submissionIds,
        Path queueRoot,
        String queueId,
        Path runtimeRoot,
        List<String> goalIds,
        List<Path> workSpoolPoints,
        List<Path> resultSpoolPoints,
        List<Path> ingressSpoolPoints,
        List<Path> bindingPoints) implements CliCommand {

    SchedulerMigrateDurableClosureCliCommand {
        submissionIds = List.copyOf(submissionIds);
        goalIds = List.copyOf(goalIds);
        workSpoolPoints = List.copyOf(workSpoolPoints);
        resultSpoolPoints = List.copyOf(resultSpoolPoints);
        ingressSpoolPoints = List.copyOf(ingressSpoolPoints);
        bindingPoints = List.copyOf(bindingPoints);
    }
}
