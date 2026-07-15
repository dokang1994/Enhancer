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
 * Projects the active task's explicit {@code ## Justified By} references into JUSTIFIED_BY
 * edges. Justification is a claim by the task's author, so the only accepted evidence is the
 * task source document naming an accepted decision exactly; unresolved, duplicate, or malformed
 * references are rejected rather than skipped.
 */
public final class TaskJustificationProjector {
    private static final String SECTION_HEADING = "## Justified By";
    private static final String BULLET_PREFIX = "- ";

    public List<GraphEdge> project(
            WorkspaceSnapshot snapshot,
            ProjectContext repositoryMemory,
            List<GraphNode> acceptedDecisions) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(repositoryMemory, "repositoryMemory must not be null");
        Objects.requireNonNull(acceptedDecisions, "acceptedDecisions must not be null");

        String sourceDocument = snapshot.approvedTaskRevision().sourceDocument();
        ProjectDocument taskDocument = repositoryMemory.documents().stream()
                .filter(document -> sourceDocument.equals(document.path()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "repositoryMemory does not contain the task source document: "
                                + sourceDocument));

        List<String> references = references(taskDocument.content());
        if (references.isEmpty()) {
            return List.of();
        }

        Set<String> decisionIds = new LinkedHashSet<>();
        for (GraphNode decision : acceptedDecisions) {
            Objects.requireNonNull(decision, "acceptedDecisions must not contain null");
            decisionIds.add(decision.nodeId());
        }

        String documentDigest = sha256(taskDocument.content());
        GraphProvenance provenance = new GraphProvenance(
                sourceDocument,
                Optional.of(documentDigest),
                freshness(snapshot, sourceDocument, documentDigest));

        List<GraphEdge> edges = new ArrayList<>(references.size());
        for (String reference : references) {
            if (!decisionIds.contains(reference)) {
                throw new IllegalArgumentException(
                        "Justified By references an unaccepted or unknown decision: "
                                + reference);
            }
            edges.add(new GraphEdge(
                    snapshot.approvedTaskRevision().taskId(),
                    GraphEdgeKind.JUSTIFIED_BY,
                    reference,
                    provenance));
        }
        return List.copyOf(edges);
    }

    private static List<String> references(String content) {
        List<String> lines = content
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .lines()
                .toList();
        int start = -1;
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).trim().equals(SECTION_HEADING)) {
                start = index + 1;
                break;
            }
        }
        if (start < 0) {
            return List.of();
        }

        Set<String> references = new LinkedHashSet<>();
        for (int index = start; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.startsWith("## ")) {
                break;
            }
            if (line.isBlank()) {
                continue;
            }
            if (!line.startsWith(BULLET_PREFIX) || line.substring(2).isBlank()) {
                throw new IllegalArgumentException(
                        "Justified By must contain bullet decision references");
            }
            if (!references.add(line.substring(2).trim())) {
                throw new IllegalArgumentException(
                        "Justified By contains a duplicate reference: " + line.substring(2));
            }
        }
        if (references.isEmpty()) {
            throw new IllegalArgumentException(
                    "Justified By must not be empty when present");
        }
        return List.copyOf(references);
    }

    private static GraphElementFreshness freshness(
            WorkspaceSnapshot snapshot,
            String sourceDocument,
            String documentDigest) {
        for (WorkspaceSourceObservation observation : snapshot.observations()) {
            if (observation.kind() == WorkspaceSourceKind.REPOSITORY_DOCUMENT
                    && sourceDocument.equals(observation.sourceId())) {
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
