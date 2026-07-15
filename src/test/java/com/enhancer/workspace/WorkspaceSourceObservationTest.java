package com.enhancer.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WorkspaceSourceObservationTest {
    private static final Instant OBSERVED_AT = Instant.parse("2026-07-15T03:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-15T02:59:00Z");

    @Test
    void representsAvailableStaleAndUnavailableSourcesExplicitly() {
        WorkspaceSourceObservation available = observation(
                WorkspaceSourceState.AVAILABLE,
                Optional.empty(),
                Optional.of("a".repeat(64)),
                Optional.empty());
        WorkspaceSourceObservation stale = observation(
                WorkspaceSourceState.STALE,
                Optional.of(UPDATED_AT),
                Optional.of("b".repeat(64)),
                Optional.of("adapter snapshot is older than the requested revision"));
        WorkspaceSourceObservation unavailable = observation(
                WorkspaceSourceState.UNAVAILABLE,
                Optional.of(UPDATED_AT),
                Optional.empty(),
                Optional.of("source adapter is unavailable"));

        assertEquals(WorkspaceSourceState.AVAILABLE, available.state());
        assertEquals(WorkspaceSourceState.STALE, stale.state());
        assertEquals(WorkspaceSourceState.UNAVAILABLE, unavailable.state());
    }

    @Test
    void rejectsAvailabilityAndDigestContradictions() {
        assertThrows(IllegalArgumentException.class, () -> observation(
                WorkspaceSourceState.AVAILABLE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> observation(
                WorkspaceSourceState.AVAILABLE,
                Optional.empty(),
                Optional.of("a".repeat(64)),
                Optional.of("unexpected reason")));
        assertThrows(IllegalArgumentException.class, () -> observation(
                WorkspaceSourceState.STALE,
                Optional.empty(),
                Optional.of("a".repeat(64)),
                Optional.of("stale")));
        assertThrows(IllegalArgumentException.class, () -> observation(
                WorkspaceSourceState.STALE,
                Optional.of(UPDATED_AT),
                Optional.of("a".repeat(64)),
                Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> observation(
                WorkspaceSourceState.UNAVAILABLE,
                Optional.empty(),
                Optional.of("a".repeat(64)),
                Optional.of("unavailable")));
        assertThrows(IllegalArgumentException.class, () -> observation(
                WorkspaceSourceState.UNAVAILABLE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));
    }

    @Test
    void rejectsFutureUpdatesMalformedDigestsAndUnboundedMetadata() {
        assertThrows(IllegalArgumentException.class, () -> observation(
                WorkspaceSourceState.STALE,
                Optional.of(OBSERVED_AT.plusSeconds(1)),
                Optional.of("a".repeat(64)),
                Optional.of("stale")));
        assertThrows(IllegalArgumentException.class, () -> observation(
                WorkspaceSourceState.AVAILABLE,
                Optional.empty(),
                Optional.of("NOT-A-DIGEST"),
                Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceSourceObservation(
                WorkspaceSourceKind.REPOSITORY_FILE,
                "x".repeat(WorkspaceSourceObservation.MAX_SOURCE_ID_CHARACTERS + 1),
                "filesystem-adapter",
                OBSERVED_AT,
                Optional.empty(),
                WorkspaceSourceState.AVAILABLE,
                Optional.of("a".repeat(64)),
                Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceSourceObservation(
                WorkspaceSourceKind.REPOSITORY_FILE,
                "src/main/App.java",
                "x".repeat(WorkspaceSourceObservation.MAX_PROVENANCE_CHARACTERS + 1),
                OBSERVED_AT,
                Optional.empty(),
                WorkspaceSourceState.AVAILABLE,
                Optional.of("a".repeat(64)),
                Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> observation(
                WorkspaceSourceState.UNAVAILABLE,
                Optional.empty(),
                Optional.empty(),
                Optional.of("x".repeat(
                        WorkspaceSourceObservation.MAX_REASON_CHARACTERS + 1))));
    }

    private WorkspaceSourceObservation observation(
            WorkspaceSourceState state,
            Optional<Instant> sourceUpdatedAt,
            Optional<String> contentSha256,
            Optional<String> reason) {
        return new WorkspaceSourceObservation(
                WorkspaceSourceKind.REPOSITORY_FILE,
                "src/main/App.java",
                "filesystem-adapter",
                OBSERVED_AT,
                sourceUpdatedAt,
                state,
                contentSha256,
                reason);
    }
}
