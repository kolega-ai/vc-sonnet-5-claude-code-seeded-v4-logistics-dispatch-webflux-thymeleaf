package com.fielddispatch.web;

import com.fielddispatch.security.CurrentUser;
import com.fielddispatch.service.FileStorageService;
import com.fielddispatch.service.ProofService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

/**
 * Proof-document downloads. Authorization (job ownership + customer-visible
 * vs internal-only) happens in ProofService.findDownloadable, one call frame
 * above this controller. The file itself is resolved through
 * FileStorageService's canonicalized base directory (PRECISION BASELINE #5).
 */
@Controller
@RequiredArgsConstructor
public class ProofController {

    private final CurrentUser currentUser;
    private final ProofService proofService;
    private final FileStorageService fileStorageService;

    @GetMapping("/proofs/{id}/download")
    @ResponseBody
    public Mono<ResponseEntity<FileSystemResource>> download(@PathVariable Long id) {
        return currentUser.get()
                .flatMap(user -> proofService.findDownloadable(user, id))
                .map(proof -> {
                    var path = fileStorageService.resolveForRead(proof.getStoredFilename());
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sanitize(proof.getOriginalFilename()) + "\"")
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(new FileSystemResource(path));
                });
    }

    private String sanitize(String name) {
        return name == null ? "proof.dat" : name.replaceAll("[\\r\\n\"]", "_");
    }
}
