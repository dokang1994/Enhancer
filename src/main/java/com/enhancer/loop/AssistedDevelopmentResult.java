package com.enhancer.loop;

import com.enhancer.planner.TaskProposal;
import java.util.Objects;
import java.util.Optional;

public record AssistedDevelopmentResult(
        AssistedDevelopmentOutcome outcome,
        Optional<TaskProposal> proposal) {

    public AssistedDevelopmentResult {
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(proposal, "proposal must not be null");

        if (outcome == AssistedDevelopmentOutcome.PROPOSAL_AVAILABLE && proposal.isEmpty()) {
            throw new IllegalArgumentException("PROPOSAL_AVAILABLE requires a proposal");
        }
        if (outcome == AssistedDevelopmentOutcome.ACTIVE_TASK_PRESERVED && proposal.isPresent()) {
            throw new IllegalArgumentException("ACTIVE_TASK_PRESERVED cannot contain a proposal");
        }
    }

    public static AssistedDevelopmentResult proposalAvailable(TaskProposal proposal) {
        return new AssistedDevelopmentResult(
                AssistedDevelopmentOutcome.PROPOSAL_AVAILABLE,
                Optional.of(Objects.requireNonNull(proposal, "proposal must not be null")));
    }

    public static AssistedDevelopmentResult activeTaskPreserved() {
        return new AssistedDevelopmentResult(
                AssistedDevelopmentOutcome.ACTIVE_TASK_PRESERVED,
                Optional.empty());
    }
}
