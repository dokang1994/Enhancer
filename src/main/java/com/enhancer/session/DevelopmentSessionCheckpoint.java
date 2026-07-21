package com.enhancer.session;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Machine-written recovery metadata for one repository development session. It references the
 * active task and evidence; it is not task, verification, completion, or delivery authority.
 */
public record DevelopmentSessionCheckpoint(
        String runId,
        String taskId,
        String taskContractSha256,
        long revision,
        DevelopmentSessionCheckpointState state,
        String currentStep,
        Optional<String> lastSuccessfulStep,
        String nextAction,
        List<String> evidenceReferences,
        List<DevelopmentSessionArtifact> artifacts) {
    public static final int MAX_EVIDENCE_REFERENCES = 64;
    public static final int MAX_ARTIFACTS = 256;
    private static final int MAX_ID_CHARACTERS = 256;
    private static final int MAX_STEP_CHARACTERS = 1024;
    private static final int MAX_REFERENCE_CHARACTERS = 2048;
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public DevelopmentSessionCheckpoint {
        runId = canonicalUuid(runId);
        taskId = bounded(taskId, "taskId", MAX_ID_CHARACTERS);
        if (taskContractSha256 == null
                || !SHA_256.matcher(taskContractSha256).matches()) {
            throw new IllegalArgumentException(
                    "taskContractSha256 must be 64 lowercase hexadecimal characters");
        }
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be positive");
        }
        Objects.requireNonNull(state, "state must not be null");
        currentStep = bounded(currentStep, "currentStep", MAX_STEP_CHARACTERS);
        Objects.requireNonNull(
                lastSuccessfulStep, "lastSuccessfulStep must not be null");
        lastSuccessfulStep = lastSuccessfulStep.map(step ->
                bounded(step, "lastSuccessfulStep", MAX_STEP_CHARACTERS));
        nextAction = bounded(nextAction, "nextAction", MAX_STEP_CHARACTERS);
        evidenceReferences = immutableUnique(
                evidenceReferences,
                "evidenceReferences",
                MAX_EVIDENCE_REFERENCES,
                MAX_REFERENCE_CHARACTERS);
        Objects.requireNonNull(artifacts, "artifacts must not be null");
        if (artifacts.size() > MAX_ARTIFACTS) {
            throw new IllegalArgumentException(
                    "artifacts exceeds " + MAX_ARTIFACTS + " entries");
        }
        artifacts = List.copyOf(artifacts);
        Set<String> artifactPaths = new HashSet<>();
        for (DevelopmentSessionArtifact artifact : artifacts) {
            Objects.requireNonNull(artifact, "artifact must not be null");
            if (!artifactPaths.add(artifact.path())) {
                throw new IllegalArgumentException(
                        "artifact paths must be unique: " + artifact.path());
            }
        }
    }

    private static String canonicalUuid(String value) {
        Objects.requireNonNull(value, "runId must not be null");
        try {
            UUID parsed = UUID.fromString(value);
            if (!parsed.toString().equals(value)) {
                throw new IllegalArgumentException("runId must be a canonical UUID");
            }
            return value;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("runId must be a canonical UUID", exception);
        }
    }

    private static String bounded(String value, String name, int maximum) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank() || value.length() > maximum) {
            throw new IllegalArgumentException(
                    name + " must be non-blank and at most " + maximum + " characters");
        }
        return value;
    }

    private static List<String> immutableUnique(
            List<String> values,
            String name,
            int maximumEntries,
            int maximumCharacters) {
        Objects.requireNonNull(values, name + " must not be null");
        if (values.size() > maximumEntries) {
            throw new IllegalArgumentException(
                    name + " exceeds " + maximumEntries + " entries");
        }
        List<String> copied = values.stream()
                .map(value -> bounded(value, "evidenceReference", maximumCharacters))
                .toList();
        if (new HashSet<>(copied).size() != copied.size()) {
            throw new IllegalArgumentException(name + " must be unique");
        }
        return copied;
    }
}
