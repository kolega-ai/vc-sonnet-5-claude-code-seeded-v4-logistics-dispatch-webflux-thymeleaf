package com.fielddispatch.security;

import com.fielddispatch.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileStorageServiceTest {

    @Test
    void rejectsPathTraversalOutsideBaseDir(@TempDir Path tempDir) throws IOException {
        FileStorageService storage = new FileStorageService(tempDir.toString());
        Path outside = tempDir.getParent().resolve("secret-outside-" + System.nanoTime() + ".txt");
        Files.writeString(outside, "should never be reachable");

        // A traversal-style relative name must never resolve outside the base dir.
        assertThrows(ResponseStatusException.class, () -> storage.resolveForRead("../" + outside.getFileName()));

        Files.deleteIfExists(outside);
    }

    @Test
    void resolvesLegitimateFileWithinBaseDir(@TempDir Path tempDir) throws IOException {
        FileStorageService storage = new FileStorageService(tempDir.toString());
        Path file = tempDir.resolve("proof.txt");
        Files.writeString(file, "proof content");

        Path resolved = storage.resolveForRead("proof.txt");
        assertEquals(file.toRealPath(), resolved);
    }
}
