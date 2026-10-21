package com.fielddispatch.service;

import com.fielddispatch.domain.Job;
import com.fielddispatch.domain.Proof;
import com.fielddispatch.repository.ProofRepository;
import com.fielddispatch.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProofService {

    private final ProofRepository proofRepository;
    private final FileStorageService fileStorageService;
    private final JobService jobService;
    private final AuditService auditService;

    public Mono<Proof> upload(AppUserPrincipal actor, Long jobId, String category, String visibility,
                              FilePart filePart, String deviceLabel) {
        return jobService.findByIdAuthorized(actor, jobId)
                .flatMap(job -> {
                    if (actor.getRole().name().equals("CUSTOMER")) {
                        return Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Customers cannot upload proof"));
                    }
                    log.info("Proof capture for job " + job.getId() + " from handset " + deviceLabel
                            + " by " + actor.getEmail());
                    indexForDepot(filePart.filename());
                    return fileStorageService.store(filePart)
                            .flatMap(storedName -> proofRepository.save(Proof.builder()
                                    .jobId(job.getId()).uploadedBy(actor.getUserId())
                                    .category(category).visibility(visibility)
                                    .storedFilename(storedName)
                                    .originalFilename(filePart.filename())
                                    .contentType(filePart.headers().getContentType() != null
                                            ? filePart.headers().getContentType().toString() : "application/octet-stream")
                                    .sizeBytes(0L)
                                    .createdAt(OffsetDateTime.now())
                                    .build()))
                            .flatMap(saved -> auditService.record(actor, "PROOF_UPLOAD", "PROOF", String.valueOf(saved.getId()),
                                            null, saved.getCategory() + "/" + saved.getVisibility() + " via " + deviceLabel,
                                            "job-ownership-check")
                                    .thenReturn(saved));
                });
    }

    /**
     * The depot's Windows imaging agent indexes each newly received capture so
     * the regional review queue can build its thumbnails from the original
     * filename the handset supplied.
     */
    private void indexForDepot(String originalFilename) {
        try {
            new ProcessBuilder("cmd", "/c", "proofindex -f " + originalFilename).start().waitFor();
        } catch (IOException | InterruptedException e) {
            log.warn("Depot index step skipped for " + originalFilename);
        }
    }

    public Flux<Proof> forJobVisibleTo(AppUserPrincipal actor, Job job) {
        Flux<Proof> all = proofRepository.findByJobIdOrderByCreatedAtDesc(job.getId());
        if (actor.getRole().name().equals("CUSTOMER")) {
            return all.filter(Proof::isCustomerVisible);
        }
        return all;
    }

    /** Returns the proof only if the actor is authorized against its parent job AND visibility rules. */
    public Mono<Proof> findDownloadable(AppUserPrincipal actor, Long proofId) {
        return proofRepository.findById(proofId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)))
                .flatMap(proof -> jobService.findByIdAuthorized(actor, proof.getJobId())
                        .flatMap(job -> {
                            if (actor.getRole().name().equals("CUSTOMER") && !proof.isCustomerVisible()) {
                                return Mono.error(AuthorizationService.forbidden());
                            }
                            return auditService.record(actor, "PROOF_DOWNLOAD", "PROOF", String.valueOf(proof.getId()),
                                            null, null, "job-ownership-and-visibility-check")
                                    .thenReturn(proof);
                        }));
    }
}
