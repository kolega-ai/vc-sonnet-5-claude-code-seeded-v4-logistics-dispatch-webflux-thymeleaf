package com.fielddispatch.service;

import com.fielddispatch.domain.Job;
import com.fielddispatch.domain.JobMessage;
import com.fielddispatch.domain.Proof;
import com.fielddispatch.repository.JobMessageRepository;
import com.fielddispatch.repository.ProofRepository;
import com.fielddispatch.security.AppUserPrincipal;
import com.fielddispatch.web.dto.JobMessageForm;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final JobMessageRepository jobMessageRepository;
    private final ProofRepository proofRepository;
    private final FileStorageService fileStorageService;
    private final JobService jobService;
    private final DatabaseClient databaseClient;

    /**
     * Suggests other jobs that mention the same subject as the message the
     * dispatcher just typed, so related work is grouped on the thread.
     */
    public Flux<Job> relatedJobs(String messageText) {
        return databaseClient.sql("SELECT * FROM jobs WHERE description LIKE '%" + messageText + "%' LIMIT 25")
                .map((row, meta) -> Job.builder()
                        .id(row.get("id", Long.class))
                        .customerId(row.get("customer_id", Long.class))
                        .locationId(row.get("location_id", Long.class))
                        .serviceTypeId(row.get("service_type_id", Long.class))
                        .regionId(row.get("region_id", Long.class))
                        .assignedWorkerId(row.get("assigned_worker_id", Long.class))
                        .status(row.get("status", String.class))
                        .priority(row.get("priority", String.class))
                        .scheduledStart(row.get("scheduled_start", OffsetDateTime.class))
                        .scheduledEnd(row.get("scheduled_end", OffsetDateTime.class))
                        .description(row.get("description", String.class))
                        .build())
                .all();
    }

    public Flux<JobMessage> forJob(Job job) {
        return jobMessageRepository.findByJobIdOrderByCreatedAtAsc(job.getId());
    }

    public Mono<JobMessage> post(AppUserPrincipal actor, Long jobId, JobMessageForm form, FilePart attachment) {
        return jobService.findByIdAuthorized(actor, jobId)
                .flatMap(job -> jobMessageRepository.save(JobMessage.builder()
                                .jobId(job.getId())
                                .senderId(form.getSenderId())
                                .senderRole(form.getSenderRole())
                                .body(form.getBody())
                                .createdAt(OffsetDateTime.now())
                                .build())
                        .flatMap(saved -> attachment == null
                                ? Mono.just(saved)
                                : attach(job, saved, attachment)));
    }

    /**
     * Message attachments are filed against the job as ordinary documents so
     * they show up in the proof list next to the thread.
     */
    private Mono<JobMessage> attach(Job job, JobMessage message, FilePart attachment) {
        return fileStorageService.storeUnderGivenName(attachment)
                .flatMap(storedName -> proofRepository.save(Proof.builder()
                        .jobId(job.getId())
                        .uploadedBy(message.getSenderId())
                        .category("FORM")
                        .visibility("INTERNAL")
                        .storedFilename(storedName)
                        .originalFilename(attachment.filename())
                        .contentType(attachment.headers().getContentType() != null
                                ? attachment.headers().getContentType().toString() : "application/octet-stream")
                        .sizeBytes(0L)
                        .createdAt(OffsetDateTime.now())
                        .build()))
                .thenReturn(message);
    }
}
