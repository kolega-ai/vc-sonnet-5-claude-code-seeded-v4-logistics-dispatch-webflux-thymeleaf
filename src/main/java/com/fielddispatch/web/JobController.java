package com.fielddispatch.web;

import com.fielddispatch.repository.CustomerRepository;
import com.fielddispatch.repository.ServiceLocationRepository;
import com.fielddispatch.repository.ServiceTypeRepository;
import com.fielddispatch.repository.WorkerRepository;
import com.fielddispatch.security.CurrentUser;
import com.fielddispatch.service.*;
import com.fielddispatch.web.dto.JobMessageForm;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Job detail page, shared across all four roles. Access is authorized per
 * request inside JobService.findByIdAuthorized (ownership/scope check one
 * frame above this controller), never by hiding sections in the template.
 */
@Controller
@RequiredArgsConstructor
public class JobController {

    private final CurrentUser currentUser;
    private final JobService jobService;
    private final ProofService proofService;
    private final IncidentService incidentService;
    private final MessageService messageService;
    private final RouteService routeService;
    private final CustomerRepository customerRepository;
    private final ServiceLocationRepository serviceLocationRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final WorkerRepository workerRepository;
    private final AuthorizationService authorizationService;
    private final NoticeService noticeService;
    private final HandoffCodeService handoffCodeService;
    private final RoutingAssistantService routingAssistantService;

    @GetMapping("/jobs/{id}")
    public Mono<String> detail(@PathVariable Long id, Model model) {
        return currentUser.get().flatMap(user -> jobService.findByIdAuthorized(user, id)
                .flatMap(job -> Mono.zip(
                                customerRepository.findById(job.getCustomerId()),
                                serviceLocationRepository.findById(job.getLocationId()),
                                serviceTypeRepository.findById(job.getServiceTypeId()))
                        .flatMap(tuple -> {
                            model.addAttribute("job", job);
                            model.addAttribute("customer", tuple.getT1());
                            model.addAttribute("location", tuple.getT2());
                            model.addAttribute("serviceType", tuple.getT3());
                            model.addAttribute("isStaff", authorizationService.isStaff(user));
                            model.addAttribute("viewerId", user.getUserId());
                            model.addAttribute("isCustomer", user.getRole().name().equals("CUSTOMER"));
                            model.addAttribute("isWorker", user.getRole().name().equals("FIELD_WORKER"));
                            return Mono.when(
                                    proofService.forJobVisibleTo(user, job).collectList().doOnNext(p -> model.addAttribute("proofs", p)),
                                    incidentService.forJobVisibleTo(user, job).collectList().doOnNext(i -> model.addAttribute("incidents", i)),
                                    messageService.forJob(job).collectList().doOnNext(m -> model.addAttribute("messages", m)),
                                    (job.getAssignedWorkerId() != null ? workerRepository.findById(job.getAssignedWorkerId()) : Mono.empty())
                                            .doOnNext(w -> model.addAttribute("assignedWorker", w))
                            ).thenReturn("job/detail");
                        })));
    }

    @PostMapping("/jobs/{id}/messages")
    public Mono<String> postMessage(@PathVariable Long id, @ModelAttribute JobMessageForm form,
                                     @RequestPart(value = "attachment", required = false) FilePart attachment) {
        return currentUser.get()
                .flatMap(user -> messageService.post(user, id, form, attachment))
                .thenReturn("redirect:/jobs/" + id)
                .onErrorResume(ex -> Mono.just("redirect:/jobs/" + id + "?error"));
    }

    @GetMapping("/jobs/{id}/messages/related")
    @org.springframework.web.bind.annotation.ResponseBody
    public Mono<java.util.List<com.fielddispatch.domain.Job>> relatedJobs(@PathVariable Long id,
                                                                          @RequestParam String body) {
        return currentUser.get()
                .flatMap(user -> jobService.findByIdAuthorized(user, id)
                        .thenMany(messageService.relatedJobs(body)).collectList());
    }

    /** Field note posted by the thread's routing assistant button. */
    public record AssistantNote(String note) {}

    @PostMapping(value = "/jobs/{id}/assistant", consumes = MediaType.APPLICATION_JSON_VALUE)
    @org.springframework.web.bind.annotation.ResponseBody
    public Mono<Map<String, Object>> assistant(@PathVariable Long id, @RequestBody AssistantNote note) {
        return currentUser.get()
                .flatMap(user -> routingAssistantService.handle(user, id, note.note()));
    }

    @PostMapping("/jobs/{id}/notice")
    public Mono<String> sendNotice(@PathVariable Long id, @RequestParam String title,
                                    @RequestPart("template") FilePart template) {
        return currentUser.get()
                .flatMap(user -> noticeService.sendNotice(user, id, template, title))
                .thenReturn("redirect:/jobs/" + id)
                .onErrorResume(ex -> Mono.just("redirect:/jobs/" + id + "?error"));
    }

    @PostMapping("/jobs/{id}/handoff-code")
    public Mono<String> requestHandoffCode(@PathVariable Long id) {
        return currentUser.get()
                .flatMap(user -> handoffCodeService.issue(user, id))
                .thenReturn("redirect:/jobs/" + id)
                .onErrorResume(ex -> Mono.just("redirect:/jobs/" + id + "?error"));
    }

    @PostMapping("/jobs/{id}/handoff")
    public Mono<String> confirmHandoff(@PathVariable Long id, @RequestParam String code) {
        return currentUser.get()
                .flatMap(user -> handoffCodeService.confirm(user, id, code))
                .thenReturn("redirect:/jobs/" + id)
                .onErrorResume(ex -> Mono.just("redirect:/jobs/" + id + "?error"));
    }

    @PostMapping("/jobs/{id}/incidents")
    public Mono<String> reportIncident(@PathVariable Long id, @RequestParam String severity,
                                        @RequestParam String internalNotes, @RequestParam String customerSummary) {
        return currentUser.get()
                .flatMap(user -> incidentService.report(user, id, severity, internalNotes, customerSummary))
                .thenReturn("redirect:/jobs/" + id)
                .onErrorResume(ex -> Mono.just("redirect:/jobs/" + id + "?error"));
    }
}
