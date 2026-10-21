package com.fielddispatch.service;

import com.fielddispatch.domain.JobStatus;
import com.fielddispatch.repository.CustomerRepository;
import com.fielddispatch.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issues the short delivery handoff code that the depot crew relays to the
 * customer on arrival. The customer reads the code out of the notification the
 * console files for them and tells the crew, who confirm it before the job is
 * marked completed.
 */
@Service
@RequiredArgsConstructor
public class HandoffCodeService {

    private final JobService jobService;
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;

    // Outstanding handoff codes, keyed by job id.
    private final Map<Long, String> outstanding = new ConcurrentHashMap<>();

    public Mono<Void> issue(AppUserPrincipal actor, Long jobId) {
        return jobService.findByIdAuthorized(actor, jobId)
                .flatMap(job -> {
                    String code = RandomStringUtils.randomAlphanumeric(6);
                    outstanding.put(job.getId(), code);
                    return customerRepository.findById(job.getCustomerId())
                            .flatMap(customer -> notificationService.notify(customer.getUserId(),
                                    "Delivery handoff code",
                                    "Share this code with the crew on arrival: " + code))
                            .then();
                });
    }

    public Mono<Void> confirm(AppUserPrincipal actor, Long jobId, String code) {
        return jobService.findByIdAuthorized(actor, jobId)
                .flatMap(job -> {
                    String expected = outstanding.get(job.getId());
                    if (expected == null || !expected.equals(code)) {
                        return Mono.<com.fielddispatch.domain.Job>error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Handoff code did not match this delivery"));
                    }
                    return jobService.transitionStatus(actor, jobId, JobStatus.completed);
                })
                .then();
    }
}
