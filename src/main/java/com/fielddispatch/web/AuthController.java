package com.fielddispatch.web;

import com.fielddispatch.repository.RegionRepository;
import com.fielddispatch.service.UserService;
import com.fielddispatch.web.dto.RegisterForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RegionRepository regionRepository;

    @GetMapping("/login")
    public Mono<String> loginPage() {
        return Mono.just("auth/login");
    }

    @GetMapping("/register")
    public Mono<String> registerPage(Model model) {
        model.addAttribute("form", new RegisterForm());
        return regionRepository.findAll().collectList()
                .doOnNext(regions -> model.addAttribute("regions", regions))
                .thenReturn("auth/register");
    }

    @PostMapping("/register")
    public Mono<String> register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            return regionRepository.findAll().collectList()
                    .doOnNext(regions -> model.addAttribute("regions", regions))
                    .thenReturn("auth/register");
        }
        return userService.registerCustomer(form.getEmail(), form.getPassword(), form.getFullName(), form.getPhone(), form.getRegionId())
                .thenReturn("redirect:/login?registered")
                .onErrorResume(ex -> {
                    model.addAttribute("errorMessage", "That email is already registered.");
                    return regionRepository.findAll().collectList()
                            .doOnNext(regions -> model.addAttribute("regions", regions))
                            .thenReturn("auth/register");
                });
    }

    @GetMapping("/forgot-password")
    public Mono<String> forgotPasswordPage() {
        return Mono.just("auth/forgot-password");
    }

    @PostMapping("/forgot-password")
    public Mono<String> forgotPassword(@RequestParam String email, Model model) {
        return userService.requestPasswordReset(email)
                .map(known -> known ? "redirect:/forgot-password?sent" : "redirect:/forgot-password?unknown");
    }

    @GetMapping("/reset-password")
    public Mono<String> resetPasswordPage(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return Mono.just("auth/reset-password");
    }

    @PostMapping("/reset-password")
    public Mono<String> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        return userService.resetPassword(token, newPassword)
                .thenReturn("redirect:/login?reset")
                .onErrorResume(ex -> Mono.just("redirect:/reset-password?token=" + token + "&error"));
    }
}
