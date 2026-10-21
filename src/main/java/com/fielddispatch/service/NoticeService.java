package com.fielddispatch.service;

import com.fielddispatch.repository.CustomerRepository;
import com.fielddispatch.security.AppUserPrincipal;
import com.github.jknack.handlebars.Handlebars;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends a customer-facing notice about a job. Dispatchers keep their own
 * notice wording in the depot's shared folder and attach the current copy as
 * a small Handlebars document so the console renders it with the job's
 * details before filing it as a notification.
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final JobService jobService;
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public Mono<String> sendNotice(AppUserPrincipal actor, Long jobId, FilePart doc, String title) {
        return jobService.findByIdAuthorized(actor, jobId)
                .flatMap(job -> customerRepository.findById(job.getCustomerId())
                        .switchIfEmpty(Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)))
                        .flatMap(customer -> DataBufferUtils.join(doc.content()).flatMap(buffer -> {
                            byte[] bytes = new byte[buffer.readableByteCount()];
                            buffer.read(bytes);
                            DataBufferUtils.release(buffer);
                            String tpl = new String(bytes, StandardCharsets.UTF_8);
                            try {
                                Map<String, Object> ctx = new HashMap<>();
                                ctx.put("jobId", job.getId());
                                ctx.put("status", job.getStatus());
                                ctx.put("customerName", customer.getName());
                                String out = new Handlebars().compileInline(tpl).apply(ctx);
                                return notificationService.notify(customer.getUserId(), title, out)
                                        .flatMap(saved -> auditService.record(actor, "NOTICE_SEND", "JOB",
                                                        String.valueOf(job.getId()), null, title, "dispatcher-job-scope")
                                                .thenReturn(out));
                            } catch (IOException e) {
                                return Mono.error(new ResponseStatusException(
                                        org.springframework.http.HttpStatus.BAD_REQUEST, "Notice document could not be rendered"));
                            }
                        })));
    }
}
