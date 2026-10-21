package com.fielddispatch.repository;

import com.fielddispatch.domain.Proof;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface ProofRepository extends R2dbcRepository<Proof, Long> {
    Flux<Proof> findByJobIdOrderByCreatedAtDesc(Long jobId);
}
