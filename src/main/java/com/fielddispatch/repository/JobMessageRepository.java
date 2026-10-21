package com.fielddispatch.repository;

import com.fielddispatch.domain.JobMessage;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface JobMessageRepository extends R2dbcRepository<JobMessage, Long> {
    Flux<JobMessage> findByJobIdOrderByCreatedAtAsc(Long jobId);
}
