package com.enhancer.tool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalInt;

public final class ReadFileTool implements Tool {
    public static final String NAME = "read-file";
    public static final String PATH_ARGUMENT = "path";
    private final EvidenceRecorder evidenceRecorder;

    public ReadFileTool() {
        this.evidenceRecorder = null;
    }

    public ReadFileTool(EvidenceRecorder evidenceRecorder) {
        this.evidenceRecorder = java.util.Objects.requireNonNull(
                evidenceRecorder,
                "evidenceRecorder must not be null");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ToolResult execute(ToolRequest request, ExecutionPolicy policy) throws IOException {
        String pathValue = request.arguments().get(PATH_ARGUMENT);
        if (pathValue == null) {
            throw new IllegalArgumentException("path argument is required");
        }
        if (pathValue.isBlank()) {
            throw new IllegalArgumentException("path argument must not be blank");
        }

        Path relativePath = Path.of(pathValue);
        if (relativePath.isAbsolute()) {
            throw new IllegalArgumentException("path must be relative to the project root");
        }

        Path normalizedRoot = policy.projectRoot();
        Path candidate = normalizedRoot.resolve(relativePath).normalize();
        if (!candidate.startsWith(normalizedRoot)) {
            throw new SecurityException("path resolves outside the project root");
        }

        Path realRoot = realProjectRoot(normalizedRoot);
        Path realFile;
        try {
            realFile = candidate.toRealPath();
        } catch (NoSuchFileException exception) {
            throw new IOException("file not found: " + pathValue, exception);
        }

        if (!realFile.startsWith(realRoot)) {
            throw new SecurityException("path resolves outside the real project root");
        }
        if (!Files.isRegularFile(realFile)) {
            throw new IOException("path must identify a regular file");
        }

        long declaredSize = Files.size(realFile);
        if (declaredSize > policy.maxReadBytes()) {
            throw new IOException("file size exceeds policy limit");
        }

        byte[] bytes = Files.readAllBytes(realFile);
        if (bytes.length > policy.maxReadBytes()) {
            throw new IOException("file size changed beyond policy limit while reading");
        }

        String content = decodeUtf8(bytes);
        if (evidenceRecorder == null
                && content.length() > VerificationEvidence.MAX_OUTPUT_TAIL_CHARACTERS) {
            throw new IllegalStateException(
                    "complete evidence persistence is required for truncated output");
        }
        VerificationEvidence evidence = evidenceRecorder == null
                ? VerificationEvidence.capture(
                        "Read file successfully",
                        content,
                        Optional.empty())
                : evidenceRecorder.capture(
                        request.correlationId(),
                        "Read file successfully",
                        content);
        return new ToolResult(
                NAME,
                ToolResultStatus.SUCCESS,
                OptionalInt.empty(),
                evidence);
    }

    private Path realProjectRoot(Path projectRoot) throws IOException {
        try {
            Path realRoot = projectRoot.toRealPath();
            if (!Files.isDirectory(realRoot)) {
                throw new IOException("project root must be a directory");
            }
            return realRoot;
        } catch (NoSuchFileException exception) {
            throw new IOException("project root not found", exception);
        }
    }

    private String decodeUtf8(byte[] bytes) throws IOException {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IOException("file is not valid UTF-8", exception);
        }
    }
}
