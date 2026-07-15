package com.enhancer.brain;

import com.enhancer.run.ResolvedRunRecord;
import com.enhancer.workspace.ApprovedTaskRevision;
import com.enhancer.workspace.WorkspaceSnapshot;
import com.enhancer.workspace.WorkspaceSourceKind;
import com.enhancer.workspace.WorkspaceSourceObservation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * First Project Brain graph producer. It projects only what one Workspace snapshot and one
 * stored run record actually prove: the approved task, the observed repository artifacts with
 * their freshness, the stored execution, and the recorded-as relationship between them. Edges
 * that no evidence justifies are never emitted.
 */
public final class RunEvidenceGraphProducer {

    public ProjectBrainGraph produce(
            WorkspaceSnapshot snapshot,
            ResolvedRunRecord run,
            Instant projectedAt) {
        return produce(snapshot, run, projectedAt, List.of());
    }

    public ProjectBrainGraph produce(
            WorkspaceSnapshot snapshot,
            ResolvedRunRecord run,
            Instant projectedAt,
            List<GraphNode> additionalNodes) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(run, "run must not be null");
        Objects.requireNonNull(projectedAt, "projectedAt must not be null");
        Objects.requireNonNull(additionalNodes, "additionalNodes must not be null");
        requireSameApprovedTask(snapshot.approvedTaskRevision(), run);

        ApprovedTaskRevision revision = snapshot.approvedTaskRevision();
        List<GraphNode> nodes = new ArrayList<>();
        nodes.add(new GraphNode(
                revision.taskId(),
                GraphNodeKind.TASK,
                new GraphProvenance(
                        revision.sourceDocument(),
                        Optional.of(revision.sourceSha256()),
                        GraphElementFreshness.CURRENT)));

        for (WorkspaceSourceObservation observation : snapshot.observations()) {
            if (observation.kind() != WorkspaceSourceKind.REPOSITORY_DOCUMENT
                    && observation.kind() != WorkspaceSourceKind.REPOSITORY_FILE) {
                continue;
            }
            nodes.add(new GraphNode(
                    observation.sourceId(),
                    GraphNodeKind.ARTIFACT,
                    new GraphProvenance(
                            observation.sourceId(),
                            observation.contentSha256(),
                            freshness(observation))));
        }

        String reference = run.metadata().reference();
        GraphProvenance executionProvenance = new GraphProvenance(
                reference,
                Optional.of(run.metadata().sha256()),
                GraphElementFreshness.CURRENT);
        nodes.add(new GraphNode(reference, GraphNodeKind.EXECUTION, executionProvenance));
        nodes.addAll(additionalNodes);

        return ProjectBrainGraph.project(
                snapshot.snapshotId(),
                projectedAt,
                nodes,
                List.of(new GraphEdge(
                        revision.taskId(),
                        GraphEdgeKind.RECORDED_AS,
                        reference,
                        executionProvenance)));
    }

    private static GraphElementFreshness freshness(WorkspaceSourceObservation observation) {
        return switch (observation.state()) {
            case AVAILABLE -> GraphElementFreshness.CURRENT;
            case STALE -> GraphElementFreshness.STALE;
            case UNAVAILABLE -> GraphElementFreshness.SOURCE_MISSING;
        };
    }

    private static void requireSameApprovedTask(
            ApprovedTaskRevision revision,
            ResolvedRunRecord run) {
        String runTaskId = run.record().approvedTask().taskId();
        if (!revision.taskId().equals(runTaskId)) {
            throw new IllegalArgumentException(
                    "run task " + runTaskId
                            + " does not match snapshot task " + revision.taskId());
        }
        String runSourceDocument = run.record().approvedTask().sourceDocument();
        if (!revision.sourceDocument().equals(runSourceDocument)) {
            throw new IllegalArgumentException(
                    "run source document " + runSourceDocument
                            + " does not match snapshot source document "
                            + revision.sourceDocument());
        }
    }
}
