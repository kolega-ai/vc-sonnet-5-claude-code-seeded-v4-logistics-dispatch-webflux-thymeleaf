package com.fielddispatch.service;

import com.fielddispatch.domain.Job;
import com.fielddispatch.repository.IncidentRepository;
import com.fielddispatch.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JobRepository jobRepository;
    private final IncidentRepository incidentRepository;

    public record DispatcherSummary(long unassigned, long inProgress, long completedToday, long openIncidents, List<Job> urgentJobs) {}

    public Mono<DispatcherSummary> dispatcherSummary(List<Long> regionIds) {
        return jobRepository.findByRegionIdIn(regionIds).collectList()
                .zipWith(incidentRepository.findAllByOrderByCreatedAtDesc().collectList())
                .map(tuple -> {
                    List<Job> jobs = tuple.getT1();
                    Map<String, Long> byStatus = jobs.stream().collect(Collectors.groupingBy(Job::getStatus, Collectors.counting()));
                    long open = tuple.getT2().stream().filter(i -> !"resolved".equals(i.getStatus()) && !"closed".equals(i.getStatus())).count();
                    List<Job> urgent = jobs.stream().filter(j -> "urgent".equals(j.getPriority())
                            && !"completed".equals(j.getStatus()) && !"cancelled".equals(j.getStatus())).limit(8).toList();
                    return new DispatcherSummary(
                            byStatus.getOrDefault("unassigned", 0L),
                            byStatus.getOrDefault("in_progress", 0L) + byStatus.getOrDefault("en_route", 0L) + byStatus.getOrDefault("arrived", 0L),
                            byStatus.getOrDefault("completed", 0L),
                            open,
                            urgent);
                });
    }
}
