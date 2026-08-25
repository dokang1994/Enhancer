package com.enhancer.runtime;

import java.util.Objects;
import java.util.Optional;

public record CoordinatedDurableMigrationPreflightResult(
        CoordinatedDurableMigrationPreflightStatus status,
        Optional<CoordinatedDurableMigrationRefusalCode> refusalCode,
        Optional<CoordinatedDurableMigrationRefusalDetail> refusalDetail) {

    public CoordinatedDurableMigrationPreflightResult {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(refusalCode, "refusalCode must not be null");
        Objects.requireNonNull(refusalDetail, "refusalDetail must not be null");
        if ((status == CoordinatedDurableMigrationPreflightStatus.REFUSED)
                != (refusalCode.isPresent() && refusalDetail.isPresent())) {
            throw new IllegalArgumentException(
                    "only a refused preflight may carry a refusal pair");
        }
    }

    static CoordinatedDurableMigrationPreflightResult ready(boolean alreadyCurrent) {
        return new CoordinatedDurableMigrationPreflightResult(
                alreadyCurrent
                        ? CoordinatedDurableMigrationPreflightStatus.ALREADY_CURRENT
                        : CoordinatedDurableMigrationPreflightStatus.READY,
                Optional.empty(),
                Optional.empty());
    }

    static CoordinatedDurableMigrationPreflightResult refused(
            CoordinatedDurableMigrationRefusalCode code,
            CoordinatedDurableMigrationRefusalDetail detail) {
        return new CoordinatedDurableMigrationPreflightResult(
                CoordinatedDurableMigrationPreflightStatus.REFUSED,
                Optional.of(code),
                Optional.of(detail));
    }
}
