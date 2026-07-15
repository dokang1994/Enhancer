package com.enhancer.context;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ProjectContextReader {
    public static final long MAX_DOCUMENT_BYTES = 1024 * 1024;

    public ProjectContext read(Path projectRoot) throws IOException {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Path realProjectRoot = projectRoot.toRealPath();
        if (!Files.isDirectory(realProjectRoot, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("project root must be a directory");
        }

        List<ProjectDocument> documents = new ArrayList<>();
        RequiredProjectDocument[] requiredDocuments = RequiredProjectDocument.values();

        for (int index = 0; index < requiredDocuments.length; index++) {
            RequiredProjectDocument requiredDocument = requiredDocuments[index];
            Path documentPath = projectRoot.resolve(requiredDocument.path());
            Path candidate = realProjectRoot.resolve(requiredDocument.path()).normalize();
            if (!candidate.startsWith(realProjectRoot)) {
                throw new IOException("required document resolves outside the project root: "
                        + requiredDocument.path());
            }

            Path realDocument;
            try {
                realDocument = candidate.toRealPath();
            } catch (NoSuchFileException exception) {
                throw new MissingProjectDocumentException(documentPath);
            }
            if (!realDocument.startsWith(realProjectRoot)) {
                throw new IOException("required document resolves outside the project root: "
                        + requiredDocument.path());
            }
            if (!Files.isRegularFile(realDocument, LinkOption.NOFOLLOW_LINKS)) {
                throw new MissingProjectDocumentException(documentPath);
            }

            long declaredSize = Files.size(realDocument);
            if (declaredSize > MAX_DOCUMENT_BYTES) {
                throw new IOException("required document size exceeds the supported limit: "
                        + requiredDocument.path());
            }
            byte[] bytes = Files.readAllBytes(realDocument);
            if (bytes.length > MAX_DOCUMENT_BYTES) {
                throw new IOException("required document size changed beyond the supported limit: "
                        + requiredDocument.path());
            }

            String content = decodeUtf8(requiredDocument.path(), bytes);
            documents.add(new ProjectDocument(requiredDocument.path(), index + 1, content));
        }

        return new ProjectContext(documents);
    }

    private String decodeUtf8(String documentPath, byte[] bytes) throws IOException {
        try {
            CharBuffer decoded = StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return decoded.toString();
        } catch (CharacterCodingException exception) {
            throw new IOException("required document is not valid UTF-8: " + documentPath, exception);
        }
    }
}
