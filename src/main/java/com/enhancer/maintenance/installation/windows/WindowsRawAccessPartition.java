package com.enhancer.maintenance.installation.windows;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Complete, disjoint allowed/denied raw-right partition. */
public record WindowsRawAccessPartition(
        Set<WindowsRawAccessRight> allowed,
        Set<WindowsRawAccessRight> denied) {
    public WindowsRawAccessPartition {
        allowed = copy(allowed, "allowed");
        denied = copy(denied, "denied");
        if (!Collections.disjoint(allowed, denied)) {
            throw new IllegalArgumentException("raw partitions must be disjoint");
        }
        EnumSet<WindowsRawAccessRight> complete = EnumSet.noneOf(
                WindowsRawAccessRight.class);
        complete.addAll(allowed);
        complete.addAll(denied);
        if (!complete.equals(EnumSet.allOf(WindowsRawAccessRight.class))) {
            throw new IllegalArgumentException("raw partition must be complete");
        }
    }

    private static Set<WindowsRawAccessRight> copy(
            Set<WindowsRawAccessRight> source, String name) {
        Set<WindowsRawAccessRight> checked = Objects.requireNonNull(
                source, name + " must not be null");
        return checked.isEmpty() ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(checked));
    }
}
