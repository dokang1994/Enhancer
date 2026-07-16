package com.enhancer.tool;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared live-worker capacity. A lease remains occupied until its actual worker thread exits.
 */
final class ToolIsolationCapacity {
    private static final int MAX_CONFIGURED_WORKERS = 1024;

    private final int maximumWorkers;
    private final AtomicInteger occupiedWorkers = new AtomicInteger();

    ToolIsolationCapacity(int maximumWorkers) {
        if (maximumWorkers < 1
                || maximumWorkers > MAX_CONFIGURED_WORKERS) {
            throw new IllegalArgumentException(
                    "maximumWorkers must be between 1 and "
                            + MAX_CONFIGURED_WORKERS);
        }
        this.maximumWorkers = maximumWorkers;
    }

    Optional<Lease> tryAcquire() {
        while (true) {
            int occupied = occupiedWorkers.get();
            if (occupied >= maximumWorkers) {
                return Optional.empty();
            }
            if (occupiedWorkers.compareAndSet(
                    occupied,
                    occupied + 1)) {
                return Optional.of(new Lease(this));
            }
        }
    }

    int occupiedWorkers() {
        return occupiedWorkers.get();
    }

    private void release() {
        int remaining = occupiedWorkers.decrementAndGet();
        if (remaining < 0) {
            throw new IllegalStateException(
                    "Tool isolation capacity was released too many times");
        }
    }

    static final class Lease implements AutoCloseable {
        private final ToolIsolationCapacity capacity;
        private final AtomicBoolean released = new AtomicBoolean();

        private Lease(ToolIsolationCapacity capacity) {
            this.capacity = capacity;
        }

        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                capacity.release();
            }
        }
    }
}
