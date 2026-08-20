package com.enhancer.model;

import java.util.Objects;
import java.util.regex.Pattern;

/** A currency-explicit integer cost ceiling that grants no spend authority. */
public record ModelCostBudget(String currencyCode, long maxMicrounits) {

    public static final long MAX_MICROUNITS = 1_000_000_000_000_000L;

    private static final Pattern CURRENCY_CODE = Pattern.compile("[A-Z]{3}");

    public ModelCostBudget {
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
        if (!CURRENCY_CODE.matcher(currencyCode).matches()) {
            throw new IllegalArgumentException(
                    "currencyCode must be an upper-case three-letter code");
        }
        if (maxMicrounits < 0) {
            throw new IllegalArgumentException("maxMicrounits must not be negative");
        }
        if (maxMicrounits > MAX_MICROUNITS) {
            throw new IllegalArgumentException(
                    "maxMicrounits exceeds the supported bound " + MAX_MICROUNITS);
        }
    }
}
