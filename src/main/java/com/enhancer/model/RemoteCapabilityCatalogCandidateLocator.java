package com.enhancer.model;

import java.nio.file.Path;

/** Uncomposed operator-private mapping from a bounded identity to one candidate path. */
interface RemoteCapabilityCatalogCandidateLocator {
    Path locate(String candidateInputIdentity);
}
