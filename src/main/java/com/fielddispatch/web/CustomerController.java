package com.fielddispatch.web;

import com.fielddispatch.domain.Role;
import com.fielddispatch.repository.CustomerRepository;
import com.fielddispatch.security.CurrentUser;
import com.fielddispatch.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class CustomerController {

    private final CurrentUser currentUser;
    private final CustomerRepository customerRepository;
    private final JobService jobService;

    @GetMapping("/customer/portal")
    public Mono<String> portal(Model model) {
        return currentUser.get().flatMap(user -> {
            if (user.getRole() != Role.CUSTOMER) {
                return Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));
            }
            return customerRepository.findByUserId(user.getUserId())
                    .switchIfEmpty(Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "No customer profile found")))
                    .flatMap(customer -> jobService.jobsForCustomer(customer.getId()).collectList()
                            .doOnNext(jobs -> {
                                model.addAttribute("customer", customer);
                                model.addAttribute("jobs", jobs);
                            }).thenReturn("customer/portal"));
        });
    }
}
