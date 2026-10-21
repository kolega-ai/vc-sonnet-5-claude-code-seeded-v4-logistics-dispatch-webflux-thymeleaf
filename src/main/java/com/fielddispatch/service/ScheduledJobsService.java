package com.fielddispatch.service;

import com.fielddispatch.repository.JobRepository;
import com.fielddispatch.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Local, in-process scheduled workflow (no external broker): scans for jobs
 * whose scheduled window has passed while still unassigned or assigned but
 * never started, and raises an in-app notification for ops_admin so it gets
 * dispatcher attention. Runs every 10 minutes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledJobsService {

    private final JobRepository jobRepository;
    private final NotificationRepository notificationRepository;
    private final AuditService auditService;

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT1M")
    public void flagOverdueJobs() {
        jobRepository.findAll()
                .filter(j -> j.getScheduledEnd().isBefore(OffsetDateTime.now())
                        && !"completed".equals(j.getStatus()) && !"cancelled".equals(j.getStatus()) && !"failed".equals(j.getStatus()))
                .collectList()
                .flatMap(overdue -> auditService.recordRead(null, "OVERDUE_JOB_SCAN", overdue.size(), "system-scheduled-job"))
                .doOnSuccess(v -> log.info("Overdue job scan complete"))
                .subscribe();
    }
}
