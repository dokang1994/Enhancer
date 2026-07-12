package com.enhancer.planner;

import java.util.List;
import java.util.Objects;

public record TaskProposal(
        String title,
        String reason,
        List<String> scope,
        List<String> acceptanceCriteria,
        List<String> outOfScope,
        List<String> risks,
        ProposalState state) {

    public TaskProposal {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        scope = List.copyOf(Objects.requireNonNull(scope, "scope must not be null"));
        acceptanceCriteria = List.copyOf(
                Objects.requireNonNull(acceptanceCriteria, "acceptanceCriteria must not be null"));
        outOfScope = List.copyOf(Objects.requireNonNull(outOfScope, "outOfScope must not be null"));
        risks = List.copyOf(Objects.requireNonNull(risks, "risks must not be null"));
        Objects.requireNonNull(state, "state must not be null");
    }
}
