package com.enhancer.model;

import java.util.Objects;

/** Closed ephemeral RFC-0029 route-selection result. */
public sealed interface ModelRouteSelectionDecision
        permits ModelRouteSelectionDecision.Selected, ModelRouteSelectionDecision.Refused {

    /** Successful selection retaining one opaque exact route. */
    record Selected(SelectedModelRoute selectedRoute)
            implements ModelRouteSelectionDecision {
        public Selected {
            Objects.requireNonNull(selectedRoute, "selectedRoute must not be null");
        }
    }

    /** Fail-closed result retaining only a reason and safe option ordinal. */
    record Refused(ModelRouteSelectionRefusalReason reason, int optionOrdinal)
            implements ModelRouteSelectionDecision {
        public Refused {
            Objects.requireNonNull(reason, "reason must not be null");
            if (reason == ModelRouteSelectionRefusalReason.NO_ROUTE_OPTIONS) {
                if (optionOrdinal != -1) {
                    throw new IllegalArgumentException(
                            "NO_ROUTE_OPTIONS requires collection ordinal -1");
                }
            } else if (optionOrdinal < 0
                    || optionOrdinal >= ModelRouteOptionSet.MAXIMUM_OPTIONS) {
                throw new IllegalArgumentException(
                        "optionOrdinal must identify one bounded route option");
            }
        }
    }
}
