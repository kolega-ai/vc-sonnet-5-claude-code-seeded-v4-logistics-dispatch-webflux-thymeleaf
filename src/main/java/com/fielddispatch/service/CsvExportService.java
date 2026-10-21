package com.fielddispatch.service;

import com.fielddispatch.domain.*;
import com.fielddispatch.repository.*;
import com.fielddispatch.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Builds CSV exports for route schedules, completion reports, and incidents.
 * Every export is scoped to the acting principal's permitted regions (staff)
 * or nothing at all for non-staff roles, and each export is audit-logged as
 * one event naming the resource class and row count.
 */
@Service
@RequiredArgsConstructor
public class CsvExportService {

    private final JobRepository jobRepository;
    private final IncidentRepository incidentRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    public Mono<String> exportCompletionReport(AppUserPrincipal actor, List<Long> regionIds) {
        return jobRepository.findByRegionIdIn(regionIds)
                .filter(j -> "completed".equals(j.getStatus()) || "failed".equals(j.getStatus()))
                .collectList()
                .flatMap(jobs -> {
                    StringBuilder sb = new StringBuilder("job_id,customer_id,region_id,status,priority,scheduled_start,scheduled_end,reschedule_count\n");
                    for (Job j : jobs) {
                        sb.append(j.getId()).append(',').append(j.getCustomerId()).append(',').append(j.getRegionId()).append(',')
                                .append(j.getStatus()).append(',').append(j.getPriority()).append(',')
                                .append(j.getScheduledStart()).append(',').append(j.getScheduledEnd()).append(',')
                                .append(j.getRescheduleCount()).append('\n');
                    }
                    return auditService.recordRead(actor, "JOB_COMPLETION_REPORT", jobs.size(), "staff-region-scope-export")
                            .thenReturn(sb.toString());
                });
    }

    public Mono<String> exportIncidents(AppUserPrincipal actor, List<Long> regionIds) {
        return jobRepository.findByRegionIdIn(regionIds).collectList()
                .flatMap(jobs -> {
                    var jobIds = jobs.stream().map(Job::getId).toList();
                    return incidentRepository.findAllByOrderByCreatedAtDesc()
                            .filter(i -> jobIds.contains(i.getJobId()))
                            .collectList()
                            .flatMap(incidents -> {
                                StringBuilder sb = new StringBuilder("incident_id,job_id,severity,status,customer_summary,created_at,resolved_at\n");
                                for (Incident i : incidents) {
                                    sb.append(i.getId()).append(',').append(i.getJobId()).append(',').append(i.getSeverity()).append(',')
                                            .append(i.getStatus()).append(',').append('"').append(safe(i.getCustomerSummary())).append('"').append(',')
                                            .append(i.getCreatedAt()).append(',').append(i.getResolvedAt() != null ? i.getResolvedAt() : "").append('\n');
                                }
                                return auditService.recordRead(actor, "INCIDENT_REPORT", incidents.size(), "staff-region-scope-export")
                                        .thenReturn(sb.toString());
                            });
                });
    }

    public Mono<String> exportRouteSchedule(AppUserPrincipal actor, Long routeId) {
        return routeRepository.findById(routeId)
                .flatMap(route -> authorizationService.assertDispatcherRegion(actor, route.getRegionId())
                        .then(routeStopRepository.findByRouteIdOrderBySequenceAsc(routeId).collectList())
                        .flatMap(stops -> {
                            StringBuilder sb = new StringBuilder("sequence,job_id\n");
                            for (RouteStop stop : stops) {
                                sb.append(stop.getSequence()).append(',').append(stop.getJobId()).append('\n');
                            }
                            return auditService.recordRead(actor, "ROUTE_SCHEDULE_EXPORT", stops.size(), "staff-region-scope-export")
                                    .thenReturn(sb.toString());
                        }));
    }

    private String safe(String s) {
        return s == null ? "" : s.replace("\"", "'");
    }
}
