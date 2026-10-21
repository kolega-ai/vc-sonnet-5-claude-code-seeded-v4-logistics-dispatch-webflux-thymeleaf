package com.fielddispatch.repository;

import com.fielddispatch.domain.ImportBatch;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface ImportBatchRepository extends R2dbcRepository<ImportBatch, Long> {
    Flux<ImportBatch> findAllByOrderByCreatedAtDesc();
}
