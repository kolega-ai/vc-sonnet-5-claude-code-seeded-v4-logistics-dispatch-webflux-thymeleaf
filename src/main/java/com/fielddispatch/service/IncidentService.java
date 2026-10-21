package com.fielddispatch.service;

import com.fielddispatch.domain.Incident;
import com.fielddispatch.domain.IncidentStatus;
import com.fielddispatch.domain.Job;
import com.fielddispatch.repository.IncidentRepository;
import com.fielddispatch.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final JobService jobService;
    private final AuditService auditService;
    private final AuthorizationService authorizationService;

    public Mono<Incident> report(AppUserPrincipal actor, Long jobId, String severity, String internalNotes, String customerSummary) {
        return jobService.findByIdAuthorized(actor, jobId)
                .flatMap(job -> incidentRepository.save(Incident.builder()
                                .jobId(job.getId()).reportedBy(actor.getUserId())
                                .severity(severity).status(IncidentStatus.open.name())
                                .internalNotes(internalNotes).customerSummary(customerSummary)
                                .createdAt(OffsetDateTime.now()).build())
                        .flatMap(saved -> auditService.record(actor, "INCIDENT_CREATE", "INCIDENT", String.valueOf(saved.getId()),
                                        null, saved.getSeverity(), "job-ownership-check")
                                .thenReturn(saved)));
    }

    public Flux<Incident> forJobVisibleTo(AppUserPrincipal actor, Job job) {
        return incidentRepository.findByJobIdOrderByCreatedAtDesc(job.getId());
    }

    public Flux<Incident> queueForStaff() {
        return incidentRepository.findAllByOrderByCreatedAtDesc();
    }

    public Mono<Incident> assign(AppUserPrincipal actor, Long incidentId, Long assigneeUserId) {
        return requireStaff(actor)
                .then(incidentRepository.findById(incidentId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)))
                .flatMap(incident -> {
                    IncidentStatus current = IncidentStatus.valueOf(incident.getStatus());
                    if (!current.canTransitionTo(IncidentStatus.assigned) && current != IncidentStatus.assigned) {
                        return Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Cannot assign from " + current));
                    }
                    incident.setAssignedTo(assigneeUserId);
                    incident.setStatus(IncidentStatus.assigned.name());
                    return incidentRepository.save(incident)
                            .flatMap(saved -> auditService.record(actor, "INCIDENT_ASSIGN", "INCIDENT", String.valueOf(saved.getId()),
                                            current.name(), saved.getStatus(), "staff-role-check")
                                    .thenReturn(saved));
                });
    }

    public Mono<Incident> updateStatus(AppUserPrincipal actor, Long incidentId, IncidentStatus target, String internalNotes, String customerSummary) {
        return requireStaff(actor)
                .then(incidentRepository.findById(incidentId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)))
                .flatMap(incident -> {
                    IncidentStatus current = IncidentStatus.valueOf(incident.getStatus());
                    if (!current.canTransitionTo(target)) {
                        return Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Cannot move incident from " + current + " to " + target));
                    }
                    if (internalNotes != null) incident.setInternalNotes(internalNotes);
                    if (customerSummary != null) incident.setCustomerSummary(customerSummary);
                    incident.setStatus(target.name());
                    if (target == IncidentStatus.resolved || target == IncidentStatus.closed) {
                        incident.setResolvedAt(OffsetDateTime.now());
                    }
                    return incidentRepository.save(incident)
                            .flatMap(saved -> auditService.record(actor, "INCIDENT_STATUS_CHANGE", "INCIDENT", String.valueOf(saved.getId()),
                                            current.name(), target.name(), "staff-role-check")
                                    .thenReturn(saved));
                });
    }

    private Mono<Void> requireStaff(AppUserPrincipal actor) {
        return authorizationService.isStaff(actor) ? Mono.empty() : Mono.error(AuthorizationService.forbidden());
    }
}
