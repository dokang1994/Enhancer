package com.enhancer.model;

/** Uncomposed publication port; its inputs grant no provider or execution authority. */
interface RemoteCapabilityCatalogPublisher {
    RemoteCapabilityCatalogPublicationResult publish(
            RemoteCapabilityCatalogPublicationIntent intent);
}
