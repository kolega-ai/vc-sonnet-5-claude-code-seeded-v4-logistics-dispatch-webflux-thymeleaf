package com.fielddispatch.web;

import com.fielddispatch.security.CurrentUser;
import com.fielddispatch.service.NotificationService;
import com.fielddispatch.service.UserService;
import com.fielddispatch.web.dto.PasswordChangeForm;
import com.fielddispatch.web.dto.ProfileForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final CurrentUser currentUser;
    private final UserService userService;
    private final NotificationService notificationService;

    @GetMapping("/profile")
    public Mono<String> profilePage(Model model) {
        return currentUser.get().doOnNext(u -> {
            ProfileForm form = new ProfileForm();
            form.setFullName(u.getFullName());
            model.addAttribute("form", form);
            model.addAttribute("user", u);
        }).thenReturn("profile/edit");
    }

    @PostMapping("/profile")
    public Mono<String> updateProfile(@Valid @ModelAttribute("form") ProfileForm form, BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            return Mono.just("profile/edit");
        }
        return currentUser.get()
                .flatMap(u -> userService.updateProfile(u, form.getFullName(), form.getPhone()))
                .thenReturn("redirect:/profile?updated");
    }

    @PostMapping("/profile/password")
    public Mono<String> changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form, BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("passwordError", "Please provide a valid current and new password.");
            return currentUser.get().doOnNext(u -> model.addAttribute("form", u)).thenReturn("profile/edit");
        }
        return currentUser.get()
                .flatMap(u -> userService.changePassword(u, form.getCurrentPassword(), form.getNewPassword()))
                .thenReturn("redirect:/profile?passwordChanged")
                .onErrorResume(ex -> Mono.just("redirect:/profile?passwordError"));
    }

    @GetMapping("/notifications")
    public Mono<String> notifications(Model model) {
        return currentUser.get().flatMap(u -> notificationService.forUser(u.getUserId()).collectList()
                .doOnNext(list -> model.addAttribute("notifications", list))
                .thenReturn("profile/notifications"));
    }

    /** Body posted by the notification card when it is opened from the bell menu. */
    public record NotificationAck(String destination) {}

    @PostMapping(value = "/notifications/{id}/read", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Mono<ResponseEntity<Void>> openNotification(@PathVariable Long id, @RequestBody NotificationAck ack) {
        return currentUser.get()
                .flatMap(u -> notificationService.markRead(u.getUserId(), id))
                .thenReturn(ResponseEntity.status(HttpStatus.SEE_OTHER)
                        .location(java.net.URI.create(ack.destination()))
                        .<Void>build());
    }

    @PostMapping("/notifications/{id}/read")
    public Mono<String> markRead(@PathVariable Long id) {
        return currentUser.get()
                .flatMap(u -> notificationService.markRead(u.getUserId(), id))
                .thenReturn("redirect:/notifications");
    }
}
