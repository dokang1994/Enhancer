package com.enhancer.cli;

import com.enhancer.runtime.FileSystemRuntimeEventPublisher;
import java.nio.file.Path;
import java.util.Objects;

/** Explicit optional filesystem roots and capacity for supported runtime-event publication. */
record RuntimeEventPublicationCliConfiguration(
        Path runtimeEventRoot,
        Path publicationRoot,
        int maxPendingPublications) {

    RuntimeEventPublicationCliConfiguration {
        Objects.requireNonNull(runtimeEventRoot, "runtimeEventRoot must not be null");
        Objects.requireNonNull(publicationRoot, "publicationRoot must not be null");
        if (maxPendingPublications
                        < FileSystemRuntimeEventPublisher.MIN_PENDING_PUBLICATIONS
                || maxPendingPublications
                        > FileSystemRuntimeEventPublisher.MAX_PENDING_PUBLICATIONS) {
            throw new IllegalArgumentException(
                    "maxPendingPublications is outside the supported range");
        }
    }
}
