package com.enhancer.brain;

import com.enhancer.context.ProjectContext;
import com.enhancer.context.ProjectDocument;
import com.enhancer.workspace.WorkspaceSnapshot;
import com.enhancer.workspace.WorkspaceSourceKind;
import com.enhancer.workspace.WorkspaceSourceObservation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Projects accepted decisions from the decision log in already-loaded repository memory into
 * unlinked decision nodes. Acceptance evidence is the document's own {@code Status: Accepted
 * Decision} line; nothing else is interpreted and no relationship edge is emitted.
 */
public final class AcceptedDecisionProjector {
    private static final String DECISION_LOG_PATH = "DECISION_LOG.md";
    private static final String HEADING_PREFIX = "### ";
    private static final String ACCEPTED_STATUS = "Status: Accepted Decision";

    public List<GraphNode> project(WorkspaceSnapshot snapshot, ProjectContext repositoryMemory) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(repositoryMemory, "repositoryMemory must not be null");

        ProjectDocument decisionLog = repositoryMemory.documents().stream()
                .filter(document -> DECISION_LOG_PATH.equals(document.path()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "repositoryMemory does not contain " + DECISION_LOG_PATH));

        String documentDigest = sha256(decisionLog.content());
        GraphProvenance provenance = new GraphProvenance(
                DECISION_LOG_PATH,
                Optional.of(documentDigest),
                freshness(snapshot, documentDigest));

        List<GraphNode> nodes = new ArrayList<>();
        Set<String> headings = new LinkedHashSet<>();
        String currentHeading = null;
        boolean currentAccepted = false;
        for (String line : decisionLog.content()
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .split("\n", -1)) {
            if (line.startsWith(HEADING_PREFIX)) {
                appendIfAccepted(nodes, currentHeading, currentAccepted, provenance);
                currentHeading = line.substring(HEADING_PREFIX.length()).trim();
                currentAccepted = false;
                if (currentHeading.isBlank() || !headings.add(currentHeading)) {
                    throw new IllegalArgumentException(
                            "decision log contains a blank or duplicate heading: "
                                    + currentHeading);
                }
            } else if (currentHeading != null && line.trim().equals(ACCEPTED_STATUS)) {
                currentAccepted = true;
            }
        }
        appendIfAccepted(nodes, currentHeading, currentAccepted, provenance);
        return List.copyOf(nodes);
    }

    private static void appendIfAccepted(
            List<GraphNode> nodes,
            String heading,
            boolean accepted,
            GraphProvenance provenance) {
        if (heading != null && accepted) {
            nodes.add(new GraphNode(heading, GraphNodeKind.DECISION, provenance));
        }
    }

    private static GraphElementFreshness freshness(
            WorkspaceSnapshot snapshot,
            String documentDigest) {
        for (WorkspaceSourceObservation observation : snapshot.observations()) {
            if (observation.kind() == WorkspaceSourceKind.REPOSITORY_DOCUMENT
                    && DECISION_LOG_PATH.equals(observation.sourceId())) {
                return observation.contentSha256()
                        .filter(documentDigest::equals)
                        .isPresent()
                                ? GraphElementFreshness.CURRENT
                                : GraphElementFreshness.STALE;
            }
        }
        return GraphElementFreshness.STALE;
    }

    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
