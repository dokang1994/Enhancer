package com.enhancer.model;

import java.util.List;
import java.util.Objects;

/** Immutable bounded caller-ordered RFC-0029 route options. */
public final class ModelRouteOptionSet {

    public static final int MAXIMUM_OPTIONS = 256;

    private final List<ModelRouteOption> options;

    public ModelRouteOptionSet(List<ModelRouteOption> options) {
        Objects.requireNonNull(options, "options must not be null");
        if (options.size() > MAXIMUM_OPTIONS) {
            throw new IllegalArgumentException(
                    "options exceeds the supported bound " + MAXIMUM_OPTIONS);
        }
        this.options = List.copyOf(options);
    }

    public List<ModelRouteOption> options() {
        return options;
    }

    @Override
    public String toString() {
        return "ModelRouteOptionSet[optionCount=" + options.size() + "]";
    }
}
