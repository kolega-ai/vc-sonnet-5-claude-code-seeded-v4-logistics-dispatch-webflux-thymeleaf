package com.fielddispatch.service;

import com.fielddispatch.domain.Job;
import com.fielddispatch.domain.Role;
import com.fielddispatch.repository.CustomerRepository;
import com.fielddispatch.repository.WorkerRepository;
import com.fielddispatch.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * PRECISION BASELINE #1: this ownership/scope check lives in a shared service
 * one call frame above the controller methods that invoke it (JobController,
 * ProofController, IncidentController, etc.) rather than being duplicated
 * inline inside each controller. Every check below resolves scope from the
 * AUTHENTICATED PRINCIPAL (loaded from our own DB via the session), never
 * from a request-supplied id.
 */
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final CustomerRepository customerRepository;
    private final WorkerRepository workerRepository;
    private final DatabaseClient databaseClient;

    /** Region ids a dispatcher is permitted to operate in. */
    public Flux<Long> dispatcherRegionIds(Long dispatcherUserId) {
        return databaseClient.sql("SELECT region_id FROM dispatcher_regions WHERE dispatcher_id = :id")
                .bind("id", dispatcherUserId)
                .map(row -> row.get("region_id", Long.class))
                .all();
    }

    /**
     * Verifies the acting principal may view/act on the given job, throwing
     * a 403 otherwise. Applies role-appropriate scoping:
     * - OPS_ADMIN: unrestricted
     * - DISPATCHER: job's region must be in the dispatcher's permitted regions
     * - FIELD_WORKER: job must be assigned to this worker's worker record
     * - CUSTOMER: job must belong to this customer's account
     */
    public Mono<Job> assertCanAccessJob(AppUserPrincipal principal, Job job) {
        if (job == null) {
            return Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        }
        return switch (principal.getRole()) {
            case OPS_ADMIN -> Mono.just(job);
            case DISPATCHER -> dispatcherRegionIds(principal.getUserId())
                    .collectList()
                    .flatMap(regions -> regions.contains(job.getRegionId())
                            ? Mono.just(job)
                            : Mono.error(forbidden()));
            case FIELD_WORKER -> workerRepository.findByUserId(principal.getUserId())
                    .flatMap(worker -> worker.getId().equals(job.getAssignedWorkerId())
                            ? Mono.just(job)
                            : Mono.<Job>error(forbidden()))
                    .switchIfEmpty(Mono.error(forbidden()));
            case CUSTOMER -> customerRepository.findByUserId(principal.getUserId())
                    .flatMap(customer -> customer.getId().equals(job.getCustomerId())
                            ? Mono.just(job)
                            : Mono.<Job>error(forbidden()))
                    .switchIfEmpty(Mono.error(forbidden()));
        };
    }

    public Mono<Void> assertDispatcherRegion(AppUserPrincipal principal, Long regionId) {
        if (principal.getRole() == Role.OPS_ADMIN) {
            return Mono.empty();
        }
        if (principal.getRole() != Role.DISPATCHER) {
            return Mono.error(forbidden());
        }
        return dispatcherRegionIds(principal.getUserId())
                .collectList()
                .flatMap(regions -> regions.contains(regionId) ? Mono.empty() : Mono.error(forbidden()));
    }

    public boolean isStaff(AppUserPrincipal principal) {
        return principal.getRole() == Role.OPS_ADMIN || principal.getRole() == Role.DISPATCHER;
    }

    public static ResponseStatusException forbidden() {
        return new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not permitted");
    }
}
