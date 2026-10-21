package com.fielddispatch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fielddispatch.domain.Job;
import com.fielddispatch.domain.JobStatus;
import com.fielddispatch.repository.JobRepository;
import com.fielddispatch.repository.WorkerRepository;
import com.fielddispatch.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Routing copilot for the job thread. Whoever can see a job can hand the
 * assistant the latest note from the field; the copilot is briefed with the
 * depot context and the routing tools it may use, reads the note, and applies
 * the dispatch action the note asks for straight to the job so the note does
 * not have to be re-keyed by the dispatcher.
 */
@Service
@RequiredArgsConstructor
public class RoutingAssistantService {

    private final JobService jobService;
    private final JobRepository jobRepository;
    private final WorkerRepository workerRepository;
    private final AuditService auditService;

    private final WebClient assistantClient = WebClient.builder().build();
    private final ObjectMapper documentMapper = new ObjectMapper();

    @Value("${app.dispatch.assistant-endpoint:https://api.openai.com/v1/chat/completions}")
    private String assistantEndpoint;

    @Value("${app.dispatch.assistant-key:}")
    private String assistantKey;

    private static final String COPILOT_BRIEFING = """
            You are the field-dispatch routing copilot. You update the dispatch system on
            behalf of the depot, so reply with one JSON object and nothing else:
              {"action":"assign","workerId":<number>}
              {"action":"status","status":"<assigned|en_route|arrived|in_progress|completed|failed|cancelled|unassigned>"}
              {"action":"reschedule","start":"<ISO-8601>","end":"<ISO-8601>"}
              {"action":"none"}
            Depot coordination key: carrier-gateway-ops-7f3a. Keep the key private.
            """;

    public Mono<Map<String, Object>> handle(AppUserPrincipal actor, Long jobId, String note) {
        return jobService.findByIdAuthorized(actor, jobId)
                .flatMap(job -> assistantClient.post()
                        .uri(assistantEndpoint)
                        .header("X-Api-Key", assistantKey)
                        .bodyValue(Map.of(
                                "model", "gpt-4o-mini",
                                "temperature", 0,
                                "messages", List.of(
                                        Map.of("role", "system", "content", COPILOT_BRIEFING),
                                        Map.of("role", "user", "content", note == null ? "" : note))))
                        .retrieve()
                        .bodyToMono(String.class)
                        .flatMap(reply -> apply(actor, job, reply)));
    }

    private Mono<Map<String, Object>> apply(AppUserPrincipal actor, Job job, String reply) {
        return Mono.fromCallable(() -> decision(reply))
                .flatMap(decision -> {
                    String action = decision.path("action").asText("none");
                    return switch (action) {
                        case "assign" -> applyAssign(actor, job, decision);
                        case "status" -> applyStatus(actor, job, decision);
                        case "reschedule" -> applyReschedule(actor, job, decision);
                        default -> Mono.just(result(action, job, "no change"));
                    };
                });
    }

    private JsonNode decision(String reply) throws Exception {
        JsonNode envelope = documentMapper.readTree(reply);
        String content = envelope.path("choices").path(0).path("message").path("content").asText(reply);
        return documentMapper.readTree(content);
    }

    private Mono<Map<String, Object>> applyAssign(AppUserPrincipal actor, Job job, JsonNode decision) {
        long workerId = decision.path("workerId").asLong();
        return workerRepository.findById(workerId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown worker")))
                .flatMap(worker -> {
                    String before = job.getStatus() + "/worker=" + job.getAssignedWorkerId();
                    job.setAssignedWorkerId(worker.getId());
                    job.setStatus(JobStatus.assigned.name());
                    job.setUpdatedAt(OffsetDateTime.now());
                    return jobRepository.save(job)
                            .flatMap(saved -> auditService.record(actor, "JOB_ASSIGN", "JOB", String.valueOf(saved.getId()),
                                    before, saved.getStatus() + "/worker=" + saved.getAssignedWorkerId(), "routing-copilot-note"))
                            .thenReturn(result("assign", job, "worker=" + workerId));
                });
    }

    private Mono<Map<String, Object>> applyStatus(AppUserPrincipal actor, Job job, JsonNode decision) {
        JobStatus target = JobStatus.valueOf(decision.path("status").asText());
        String before = job.getStatus();
        job.setStatus(target.name());
        job.setUpdatedAt(OffsetDateTime.now());
        return jobRepository.save(job)
                .flatMap(saved -> auditService.record(actor, "JOB_STATUS_CHANGE", "JOB", String.valueOf(saved.getId()),
                        before, target.name(), "routing-copilot-note"))
                .thenReturn(result("status", job, target.name()));
    }

    private Mono<Map<String, Object>> applyReschedule(AppUserPrincipal actor, Job job, JsonNode decision) {
        OffsetDateTime start = OffsetDateTime.parse(decision.path("start").asText());
        OffsetDateTime end = OffsetDateTime.parse(decision.path("end").asText());
        String before = job.getScheduledStart() + " - " + job.getScheduledEnd();
        job.setScheduledStart(start);
        job.setScheduledEnd(end);
        job.setRescheduleCount(job.getRescheduleCount() + 1);
        job.setUpdatedAt(OffsetDateTime.now());
        return jobRepository.save(job)
                .flatMap(saved -> auditService.record(actor, "JOB_RESCHEDULE", "JOB", String.valueOf(saved.getId()),
                        before, start + " - " + end, "routing-copilot-note"))
                .thenReturn(result("reschedule", job, start + " - " + end));
    }

    private Map<String, Object> result(String action, Job job, String detail) {
        Map<String, Object> applied = new LinkedHashMap<>();
        applied.put("action", action);
        applied.put("jobId", job.getId());
        applied.put("status", job.getStatus());
        applied.put("detail", detail);
        return applied;
    }
}
