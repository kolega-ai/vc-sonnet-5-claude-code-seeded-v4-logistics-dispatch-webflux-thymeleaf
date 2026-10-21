package com.fielddispatch.repository;

import com.fielddispatch.domain.Worker;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorkerRepository extends R2dbcRepository<Worker, Long> {
    Mono<Worker> findByUserId(Long userId);
    Flux<Worker> findByRegionId(Long regionId);
}
