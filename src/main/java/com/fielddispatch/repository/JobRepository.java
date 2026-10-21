package com.fielddispatch.repository;

import com.fielddispatch.domain.Job;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.List;

public interface JobRepository extends R2dbcRepository<Job, Long> {
    Flux<Job> findByCustomerId(Long customerId);
    Flux<Job> findByAssignedWorkerId(Long workerId);
    Flux<Job> findByRegionIdIn(List<Long> regionIds);
}
