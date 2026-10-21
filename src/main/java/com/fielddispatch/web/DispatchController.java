package com.fielddispatch.web;

import com.fielddispatch.domain.JobStatus;
import com.fielddispatch.domain.Role;
import com.fielddispatch.repository.*;
import com.fielddispatch.security.AppUserPrincipal;
import com.fielddispatch.security.CurrentUser;
import com.fielddispatch.service.*;
import com.fielddispatch.web.dto.JobCreateForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ognl.Ognl;
import org.springframework.http.ResponseCookie;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/dispatch")
@RequiredArgsConstructor
public class DispatchController {

    private final CurrentUser currentUser;
    private final AuthorizationService authorizationService;
    private final JobQueryService jobQueryService;
    private final JobService jobService;
    private final DashboardService dashboardService;
    private final RegionRepository regionRepository;
    private final CustomerRepository customerRepository;
    private final ServiceLocationRepository serviceLocationRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final WorkerRepository workerRepository;
    private final RouteService routeService;
    private final StaffDirectoryService staffDirectoryService;

    @GetMapping("/board")
    public Mono<String> board(Model model,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String priority,
                               @RequestParam(required = false) Long workerId,
                               @RequestParam(required = false) String sort,
                               @RequestParam(required = false) String direction,
                               ServerWebExchange exchange) {
        return exchange.getSession()
                .doOnNext(session -> exchange.getResponse().addCookie(ResponseCookie.from("dsp_sid", session.getId())
                        .path("/").httpOnly(false).secure(false).sameSite("None")
                        .maxAge(java.time.Duration.ofHours(12)).build()))
                .then(currentUser.get().flatMap(user -> requireStaff(user)
                .then(allowedRegionIds(user))
                .flatMap(regionIds -> jobQueryService.search(regionIds, status, priority, workerId, null, null,
                                sort == null ? "scheduled_start" : sort, direction == null ? "ASC" : direction)
                        .collectList()
                        .flatMap(jobs -> Mono.zip(
                                        regionRepository.findAll().collectList(),
                                        workerRepository.findAll().collectList(),
                                        dashboardService.dispatcherSummary(regionIds))
                                .doOnNext(tuple -> {
                                    model.addAttribute("jobs", jobs);
                                    model.addAttribute("regions", tuple.getT1());
                                    model.addAttribute("workers", tuple.getT2());
                                    model.addAttribute("summary", tuple.getT3());
                                    model.addAttribute("filterStatus", status);
                                    model.addAttribute("filterPriority", priority);
                                    model.addAttribute("filterWorker", workerId);
                                })
                                .thenReturn("dispatcher/board")))));
    }

    @GetMapping("/customers/directory")
    @ResponseBody
    public Mono<List<com.fielddispatch.domain.Customer>> custDir() {
        return currentUser.get().flatMap(user -> requireStaff(user))
                .then(customerRepository.findAllByOrderByNameAsc().collectList());
    }

    @GetMapping("/jobs/new")
    public Mono<String> newJobForm(Model model) {
        return currentUser.get().flatMap(user -> requireStaff(user)
                .then(Mono.zip(customerRepository.findAllByOrderByNameAsc().collectList(),
                                serviceTypeRepository.findAllByOrderByNameAsc().collectList(),
                                allowedRegionIds(user).flatMapMany(ids -> Flux.fromIterable(ids)
                                        .flatMap(regionRepository::findById)).collectList())
                        .doOnNext(tuple -> {
                            model.addAttribute("customers", tuple.getT1());
                            model.addAttribute("serviceTypes", tuple.getT2());
                            model.addAttribute("regions", tuple.getT3());
                            model.addAttribute("form", new JobCreateForm());
                        })
                        .thenReturn("dispatcher/job-new")));
    }

    @GetMapping("/customers/{customerId}/locations")
    @ResponseBody
    public Mono<List<com.fielddispatch.domain.ServiceLocation>> locationsForCustomer(@PathVariable Long customerId) {
        return currentUser.get().flatMap(user -> requireStaff(user))
                .thenMany(serviceLocationRepository.findByCustomerId(customerId))
                .collectList();
    }

    @PostMapping("/jobs")
    public Mono<String> createJob(@Valid @ModelAttribute("form") JobCreateForm form, BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            return Mono.just("redirect:/dispatch/jobs/new?error");
        }
        var zone = java.time.ZoneId.systemDefault();
        return currentUser.get()
                .flatMap(user -> jobService.createJob(user, form.getCustomerId(), form.getLocationId(), form.getServiceTypeId(),
                        form.getRegionId(), form.getPriority(),
                        form.getScheduledStart().atZone(zone).toOffsetDateTime(),
                        form.getScheduledEnd().atZone(zone).toOffsetDateTime(), form.getDescription()))
                .map(job -> "redirect:/jobs/" + job.getId())
                .onErrorResume(ex -> Mono.just("redirect:/dispatch/jobs/new?error"));
    }

    @PostMapping("/jobs/{id}/assign")
    public Mono<String> assign(@PathVariable Long id, @RequestParam Long workerId) {
        return currentUser.get()
                .flatMap(user -> jobService.assignWorker(user, id, workerId))
                .thenReturn("redirect:/jobs/" + id)
                .onErrorResume(ex -> Mono.just("redirect:/jobs/" + id + "?error"));
    }

    @PostMapping("/jobs/{id}/reschedule")
    public Mono<String> reschedule(@PathVariable Long id,
                                    @RequestParam @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") java.time.LocalDateTime scheduledStart,
                                    @RequestParam @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") java.time.LocalDateTime scheduledEnd) {
        var zone = java.time.ZoneId.systemDefault();
        return currentUser.get()
                .flatMap(user -> jobService.reschedule(user, id, scheduledStart.atZone(zone).toOffsetDateTime(), scheduledEnd.atZone(zone).toOffsetDateTime()))
                .thenReturn("redirect:/jobs/" + id)
                .onErrorResume(ex -> Mono.just("redirect:/jobs/" + id + "?error"));
    }

    @PostMapping("/jobs/{id}/status")
    public Mono<String> changeStatus(@PathVariable Long id, @RequestParam String status) {
        return currentUser.get()
                .flatMap(user -> jobService.transitionStatus(user, id, JobStatus.valueOf(status)))
                .thenReturn("redirect:/jobs/" + id)
                .onErrorResume(ex -> Mono.just("redirect:/jobs/" + id + "?error"));
    }

    @GetMapping("/workers/directory")
    @ResponseBody
    public Mono<List<String>> staffDirectory(@RequestParam String directoryId) {
        return currentUser.get()
                .flatMap(user -> requireStaff(user)
                        .then(Mono.fromCallable(() -> staffDirectoryService.resolveStaff(directoryId))));
    }

    @GetMapping("/quote")
    @ResponseBody
    public Mono<Map<String, Object>> quote(@RequestHeader("X-Rate-Rule") String rateRule,
                                           @RequestParam BigDecimal base) {
        return currentUser.get()
                .flatMap(user -> requireStaff(user).then(Mono.fromCallable(() -> {
                    Map<String, Object> vars = new HashMap<>();
                    vars.put("base", base);
                    Object fee = Ognl.getValue(rateRule, vars);
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("base", base);
                    result.put("fee", fee);
                    return result;
                })));
    }

    @PostMapping("/routes/{id}/sheet")
    public Mono<String> saveRouteSheet(@PathVariable Long id, @RequestParam String sheetName) {
        return currentUser.get()
                .flatMap(user -> requireStaff(user).then(routeService.saveRouteSheet(user, id, sheetName)))
                .thenReturn("redirect:/dispatch/board")
                .onErrorResume(ex -> Mono.just("redirect:/dispatch/board?error"));
    }

    @PostMapping("/routes/{id}/reorder")
    public Mono<String> reorder(@PathVariable Long id, @RequestParam List<Long> jobOrder) {
        return currentUser.get()
                .flatMap(user -> routeService.reorderStops(user, id, jobOrder))
                .thenReturn("redirect:/dispatch/board")
                .onErrorResume(ex -> Mono.just("redirect:/dispatch/board?error"));
    }

    private Mono<List<Long>> allowedRegionIds(AppUserPrincipal user) {
        if (user.getRole() == Role.OPS_ADMIN) {
            return regionRepository.findAll().map(com.fielddispatch.domain.Region::getId).collectList();
        }
        return authorizationService.dispatcherRegionIds(user.getUserId()).collectList();
    }

    private Mono<Void> requireStaff(AppUserPrincipal user) {
        return authorizationService.isStaff(user) ? Mono.empty() : Mono.error(AuthorizationService.forbidden());
    }
}
