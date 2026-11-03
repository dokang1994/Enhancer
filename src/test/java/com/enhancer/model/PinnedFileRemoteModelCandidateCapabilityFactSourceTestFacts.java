package com.enhancer.model;

import java.time.Duration;
import java.time.Instant;

final class PinnedFileRemoteModelCandidateCapabilityFactSourceTestFacts {
    RemoteModelCandidateCapabilityFacts facts() {
        return RemoteModelCandidateCapabilityFacts.create(
                RemoteModelCandidateCapabilityFacts.SCHEMA_VERSION,
                "catalog-source",
                "c".repeat(64),
                "capacity-evidence-a",
                Instant.parse("2026-10-23T00:00:00Z"),
                Instant.parse("2026-10-23T00:05:00Z"),
                "candidate-a",
                "destination-a",
                "reasoning-standard",
                "repository-analysis",
                ModelRoutePricingFacts.TOKEN_SEMANTICS_IDENTITY,
                "schedule-a",
                new ModelTokenBudget(10, 5, 15),
                ModelReasoningRequirement.EXTENDED,
                1_000,
                new ModelTokenBudget(100, 50, 150),
                Duration.ofSeconds(60),
                "USD",
                987_654_321,
                ModelDataClassification.RESTRICTED);
    }
}
