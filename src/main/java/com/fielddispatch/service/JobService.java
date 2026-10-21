package com.fielddispatch.service;

import com.fielddispatch.domain.*;
import com.fielddispatch.repository.*;
import com.fielddispatch.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final ProofRepository proofRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final WorkerRepository workerRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final WebhookService webhookService;

    public Mono<Job> findByIdAuthorized(AppUserPrincipal principal, Long jobId) {
        return jobRepository.findById(jobId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)))
                .flatMap(job -> authorizationService.assertCanAccessJob(principal, job));
    }

    public Mono<Job> createJob(AppUserPrincipal dispatcher, Long customerId, Long locationId, Long serviceTypeId,
                                Long regionId, String priority, OffsetDateTime start, OffsetDateTime end, String description) {
        return authorizationService.assertDispatcherRegion(dispatcher, regionId)
                .then(Mono.defer(() -> {
                    Job job = Job.builder()
                            .customerId(customerId).locationId(locationId).serviceTypeId(serviceTypeId)
                            .regionId(regionId).status(JobStatus.unassigned.name()).priority(priority)
                            .scheduledStart(start).scheduledEnd(end).description(description)
                            .rescheduleCount(0).createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                            .build();
                    return jobRepository.save(job)
                            .flatMap(saved -> auditService.record(dispatcher, "JOB_CREATE", "JOB", String.valueOf(saved.getId()),
                                            null, saved.getStatus(), "dispatcher-region-scope")
                                    .thenReturn(saved));
                }));
    }

    public Mono<Job> assignWorker(AppUserPrincipal dispatcher, Long jobId, Long workerId) {
        return findByIdAuthorized(dispatcher, jobId)
                .flatMap(job -> requireRole(dispatcher, Role.DISPATCHER, Role.OPS_ADMIN)
                        .then(workerRepository.findById(workerId))
                        .switchIfEmpty(Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Worker not found")))
                        .flatMap(worker -> {
                            if (!worker.getRegionId().equals(job.getRegionId())) {
                                return Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Worker is outside job region"));
                            }
                            String before = job.getStatus() + "/worker=" + job.getAssignedWorkerId();
                            job.setAssignedWorkerId(worker.getId());
                            job.setStatus(JobStatus.assigned.name());
                            job.setUpdatedAt(OffsetDateTime.now());
                            return jobRepository.save(job)
                                    .flatMap(saved -> auditService.record(dispatcher, "JOB_ASSIGN", "JOB", String.valueOf(saved.getId()),
                                                    before, saved.getStatus() + "/worker=" + saved.getAssignedWorkerId(), "dispatcher-assignment")
                                            .then(notificationService.notify(worker.getUserId(), "New job assigned",
                                                    "You have been assigned job #" + saved.getId() + "."))
                                            .then(webhookService.fireJobStatusEvent(saved))
                                            .thenReturn(saved));
                        }));
    }

    public Mono<Job> reschedule(AppUserPrincipal dispatcher, Long jobId, OffsetDateTime newStart, OffsetDateTime newEnd) {
        return findByIdAuthorized(dispatcher, jobId)
                .flatMap(job -> requireRole(dispatcher, Role.DISPATCHER, Role.OPS_ADMIN).then(Mono.defer(() -> {
                    String before = job.getScheduledStart() + " - " + job.getScheduledEnd();
                    job.setScheduledStart(newStart);
                    job.setScheduledEnd(newEnd);
                    job.setRescheduleCount(job.getRescheduleCount() + 1);
                    job.setUpdatedAt(OffsetDateTime.now());
                    return jobRepository.save(job)
                            .flatMap(saved -> auditService.record(dispatcher, "JOB_RESCHEDULE", "JOB", String.valueOf(saved.getId()),
                                            before, saved.getScheduledStart() + " - " + saved.getScheduledEnd(), "dispatcher-reschedule")
                                    .thenReturn(saved));
                })));
    }

    /**
     * Applies a job status transition. FIELD_WORKER may only update jobs
     * assigned to them (enforced by findByIdAuthorized) and can never move a
     * job that is still 'unassigned'. DISPATCHER/OPS_ADMIN may cancel or
     * reset jobs. All transitions are validated against JobStatus's edge list.
     */
    public Mono<Job> transitionStatus(AppUserPrincipal actor, Long jobId, JobStatus target) {
        return findByIdAuthorized(actor, jobId)
                .flatMap(job -> {
                    JobStatus current = JobStatus.valueOf(job.getStatus());
                    if (current == JobStatus.unassigned && actor.getRole() == Role.FIELD_WORKER) {
                        return Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                                "Field workers cannot update unassigned jobs"));
                    }
                    if (!current.canTransitionTo(target)) {
                        return Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                                "Cannot move job from " + current + " to " + target));
                    }
                    if (target == JobStatus.completed) {
                        return serviceTypeRepository.findById(job.getServiceTypeId())
                                .flatMap(st -> {
                                    if (st.isRequiresProof()) {
                                        return proofRepository.findByJobIdOrderByCreatedAtDesc(job.getId()).hasElements()
                                                .flatMap(hasProof -> hasProof
                                                        ? applyTransition(actor, job, current, target)
                                                        : Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                                                                "This service type requires proof before completion")));
                                    }
                                    return applyTransition(actor, job, current, target);
                                });
                    }
                    return applyTransition(actor, job, current, target);
                });
    }

    private Mono<Job> applyTransition(AppUserPrincipal actor, Job job, JobStatus before, JobStatus target) {
        job.setStatus(target.name());
        job.setUpdatedAt(OffsetDateTime.now());
        return jobRepository.save(job)
                .flatMap(saved -> auditService.record(actor, "JOB_STATUS_CHANGE", "JOB", String.valueOf(saved.getId()),
                                before.name(), target.name(), roleBasis(actor))
                        .then(webhookService.fireJobStatusEvent(saved))
                        .thenReturn(saved));
    }

    public Flux<Job> jobsForWorker(Long workerId) {
        return jobRepository.findByAssignedWorkerId(workerId);
    }

    public Flux<Job> jobsForCustomer(Long customerId) {
        return jobRepository.findByCustomerId(customerId);
    }

    private String roleBasis(AppUserPrincipal actor) {
        return actor.getRole().name().toLowerCase() + "-ownership-check";
    }

    private Mono<Void> requireRole(AppUserPrincipal principal, Role... roles) {
        for (Role r : roles) {
            if (principal.getRole() == r) {
                return Mono.empty();
            }
        }
        return Mono.error(AuthorizationService.forbidden());
    }
}
