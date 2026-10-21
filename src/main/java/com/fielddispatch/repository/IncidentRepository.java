package com.fielddispatch.repository;

import com.fielddispatch.domain.Incident;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface IncidentRepository extends R2dbcRepository<Incident, Long> {
    Flux<Incident> findByJobIdOrderByCreatedAtDesc(Long jobId);
    Flux<Incident> findAllByOrderByCreatedAtDesc();
}
