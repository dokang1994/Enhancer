package com.enhancer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SchedulerApplyCancelCliArgumentsTest {
    private static final String GOAL_ID =
            "00000000-0000-0000-0000-00000000ca01";
    private static final String CONTROL_ID =
            "00000000-0000-0000-0000-00000000ca02";

    @TempDir
    Path temporaryRoot;

    @Test
    void parsesOnlyRequestStorageAndOptionalEventInputs() {
        SchedulerApplyCancelCliCommand command = (SchedulerApplyCancelCliCommand)
                CliArguments.parse(arguments());

        assertEquals(temporaryRoot.resolve("runtime").toAbsolutePath().normalize(),
                command.runtimeRoot());
        assertEquals(GOAL_ID, command.goalId());
        assertEquals(CONTROL_ID, command.controlMessageId());
        assertEquals(temporaryRoot.resolve("proof.bin").toAbsolutePath().normalize(),
                command.proofFile());
        assertEquals(temporaryRoot.resolve("audit").toAbsolutePath().normalize(),
                command.authorizationAuditRoot());
        assertFalse(command.runtimeEventPublication().isPresent());
    }

    @Test
    void parsesTheExistingAllOrNoneEventGroup() {
        String[] arguments = java.util.Arrays.copyOf(arguments(), 17);
        arguments[11] = "--runtime-event-root";
        arguments[12] = temporaryRoot.resolve("events").toString();
        arguments[13] = "--runtime-event-publication-root";
        arguments[14] = temporaryRoot.resolve("points").toString();
        arguments[15] = "--max-pending-runtime-event-publications";
        arguments[16] = "8";

        SchedulerApplyCancelCliCommand command = (SchedulerApplyCancelCliCommand)
                CliArguments.parse(arguments);

        assertEquals(8, command.runtimeEventPublication().orElseThrow()
                .maxPendingPublications());
    }

    @Test
    void rejectsTrustAuthorityAndMalformedRequestInputs() {
        for (String forbidden : new String[] {
                "policy-file", "policy-sha256", "metadata-file", "actor-id",
                "authorization-id", "issuer-id", "key-id", "clock", "approved"
        }) {
            String[] arguments = java.util.Arrays.copyOf(arguments(), 13);
            arguments[11] = "--" + forbidden;
            arguments[12] = "attacker-controlled";
            assertThrows(CliUsageException.class, () -> CliArguments.parse(arguments));
        }
        String[] badGoal = arguments();
        badGoal[4] = "not-a-uuid";
        assertThrows(CliUsageException.class, () -> CliArguments.parse(badGoal));
        String[] partialEvents = java.util.Arrays.copyOf(arguments(), 13);
        partialEvents[11] = "--runtime-event-root";
        partialEvents[12] = temporaryRoot.resolve("events").toString();
        assertThrows(CliUsageException.class, () -> CliArguments.parse(partialEvents));
    }

    private String[] arguments() {
        return new String[] {
                "scheduler-apply-cancel",
                "--runtime-root", temporaryRoot.resolve("runtime").toString(),
                "--goal-id", GOAL_ID,
                "--control-message-id", CONTROL_ID,
                "--proof-file", temporaryRoot.resolve("proof.bin").toString(),
                "--authorization-audit-root", temporaryRoot.resolve("audit").toString()
        };
    }
}
