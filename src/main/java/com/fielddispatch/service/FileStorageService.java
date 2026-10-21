package com.fielddispatch.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;

/**
 * Stores uploaded proof documents on local disk and resolves downloads back
 * to disk.
 *
 * PRECISION BASELINE #5: downloads never concatenate a client-supplied name
 * directly into a filesystem path. Every read resolves the stored filename
 * against the canonicalized upload base directory and verifies the resulting
 * path is still contained within it, which defeats "../" traversal even if a
 * stored filename were ever attacker-influenced.
 */
@Service
public class FileStorageService {

    private final Path baseDir;

    public FileStorageService(@Value("${app.uploads.base-dir}") String baseDirProperty) {
        try {
            Path dir = Path.of(baseDirProperty);
            Files.createDirectories(dir);
            this.baseDir = dir.toRealPath();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to initialise upload directory", e);
        }
    }

    public Mono<String> store(FilePart filePart) {
        String extension = StringUtils.getFilenameExtension(filePart.filename());
        String storedName = UUID.randomUUID() + (extension != null ? "." + sanitizeExtension(extension) : "");
        Path target = baseDir.resolve(storedName);
        return filePart.transferTo(target).thenReturn(storedName);
    }

    /**
     * Stores a document under the name the sender gave it, so the attachment
     * keeps its identity in the job thread and in the proof list.
     */
    public Mono<String> storeUnderGivenName(FilePart filePart) {
        String storedName = uniqueSuffix() + "-" + filePart.filename();
        Path target = baseDir.resolve(storedName);
        return DataBufferUtils.write(filePart.content(), target, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
                .thenReturn(storedName);
    }

    /**
     * Canonicalizes the requested file against the base directory and refuses
     * to serve anything that resolves outside it.
     */
    public Path resolveForRead(String storedFilename) {
        Path candidate = baseDir.resolve(storedFilename).normalize();
        Path real;
        try {
            real = candidate.toRealPath();
        } catch (IOException e) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "File not found");
        }
        if (!real.startsWith(baseDir)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Invalid file path");
        }
        return real;
    }

    private String sanitizeExtension(String ext) {
        return ext.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    public String uniqueSuffix() {
        return Instant.now().toEpochMilli() + "-" + UUID.randomUUID();
    }
}
