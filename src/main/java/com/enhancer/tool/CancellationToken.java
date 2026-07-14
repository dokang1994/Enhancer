package com.enhancer.tool;

@FunctionalInterface
public interface CancellationToken {
    boolean isCancellationRequested();

    static CancellationToken none() {
        return () -> false;
    }
}
