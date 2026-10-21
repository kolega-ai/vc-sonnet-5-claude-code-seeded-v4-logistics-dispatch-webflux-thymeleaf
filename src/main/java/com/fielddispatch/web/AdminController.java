package com.fielddispatch.web;

import com.fielddispatch.domain.Region;
import com.fielddispatch.domain.Role;
import com.fielddispatch.domain.ServiceType;
import com.fielddispatch.repository.*;
import com.fielddispatch.security.CurrentUser;
import com.fielddispatch.service.AuditService;
import com.fielddispatch.service.DashboardService;
import com.fielddispatch.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * All routes under /admin are additionally gated to ROLE_OPS_ADMIN at the
 * security-filter-chain level (see SecurityConfig); this controller performs
 * business-level operations only after that coarse gate has already passed.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CurrentUser currentUser;
    private final RegionRepository regionRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final WorkerRepository workerRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserService userService;
    private final DashboardService dashboardService;
    private final AuditService auditService;
    private final JobRepository jobRepository;

    @GetMapping("/overview")
    public Mono<String> overview(Model model) {
        return currentUser.get().flatMap(admin ->
                Mono.zip(regionRepository.findAll().collectList(), jobRepository.findAll().collectList())
                        .doOnNext(tuple -> {
                            model.addAttribute("regionCount", tuple.getT1().size());
                            model.addAttribute("jobCount", tuple.getT2().size());
                        })
                        .thenReturn("admin/overview"));
    }

    @GetMapping("/regions")
    public Mono<String> regions(Model model) {
        return regionRepository.findAll().collectList()
                .doOnNext(regions -> model.addAttribute("regions", regions))
                .thenReturn("admin/regions");
    }

    @PostMapping("/regions")
    public Mono<String> createRegion(@RequestParam String name, @RequestParam String code) {
        return currentUser.get().flatMap(admin -> regionRepository.save(Region.builder()
                        .name(name).code(code).createdAt(OffsetDateTime.now()).build())
                .flatMap(saved -> auditService.record(admin, "REGION_CREATE", "REGION", String.valueOf(saved.getId()),
                        null, name, "ops_admin-role"))
                .thenReturn("redirect:/admin/regions"));
    }

    @GetMapping("/service-types")
    public Mono<String> serviceTypes(Model model) {
        return serviceTypeRepository.findAllByOrderByNameAsc().collectList()
                .doOnNext(list -> model.addAttribute("serviceTypes", list))
                .thenReturn("admin/service-types");
    }

    @PostMapping("/service-types")
    public Mono<String> createServiceType(@RequestParam String name, @RequestParam String description,
                                           @RequestParam(defaultValue = "false") boolean requiresProof, @RequestParam BigDecimal baseRate) {
        return currentUser.get().flatMap(admin -> serviceTypeRepository.save(ServiceType.builder()
                        .name(name).description(description).requiresProof(requiresProof).baseRate(baseRate).build())
                .flatMap(saved -> auditService.record(admin, "SERVICE_TYPE_CREATE", "SERVICE_TYPE", String.valueOf(saved.getId()),
                        null, name, "ops_admin-role"))
                .thenReturn("redirect:/admin/service-types"));
    }

    @GetMapping("/workers")
    public Mono<String> workers(Model model) {
        return workerRepository.findAll().collectList()
                .flatMap(workers -> userRepository.findAll().collectList()
                        .doOnNext(users -> {
                            model.addAttribute("workers", workers);
                            model.addAttribute("users", users);
                        }))
                .thenReturn("admin/workers");
    }

    @GetMapping("/users")
    public Mono<String> users(Model model) {
        return userRepository.findAll().collectList()
                .doOnNext(list -> model.addAttribute("users", list))
                .thenReturn("admin/users");
    }

    @PostMapping("/users/{id}/role")
    public Mono<String> changeRole(@PathVariable Long id, @RequestParam Role role) {
        return currentUser.get()
                .flatMap(admin -> userService.adminChangeRole(admin, id, role))
                .thenReturn("redirect:/admin/users");
    }

    @PostMapping("/users/{id}/enabled")
    public Mono<String> setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        return currentUser.get()
                .flatMap(admin -> userService.adminSetEnabled(admin, id, enabled))
                .thenReturn("redirect:/admin/users");
    }

    @PostMapping("/dispatchers/{id}/regions")
    public Mono<String> assignDispatcherRegions(@PathVariable Long id, @RequestParam List<Long> regionIds) {
        return currentUser.get()
                .flatMap(admin -> userService.replaceDispatcherRegions(id, regionIds))
                .thenReturn("redirect:/admin/users");
    }

    @GetMapping("/audit-log")
    public Mono<String> auditLog(Model model, @RequestParam(defaultValue = "0") int page) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, 50)).collectList()
                .doOnNext(list -> {
                    model.addAttribute("entries", list);
                    model.addAttribute("page", page);
                })
                .thenReturn("admin/audit-log");
    }
}
