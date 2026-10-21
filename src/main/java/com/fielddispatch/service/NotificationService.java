package com.fielddispatch.service;

import com.fielddispatch.domain.Notification;
import com.fielddispatch.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Mono<Notification> notify(Long userId, String title, String body) {
        return notificationRepository.save(Notification.builder()
                .userId(userId).title(title).body(body).isRead(false)
                .createdAt(OffsetDateTime.now()).build());
    }

    public Flux<Notification> forUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Mono<Long> unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public Mono<Notification> markRead(Long userId, Long notificationId) {
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .flatMap(n -> {
                    n.setRead(true);
                    return notificationRepository.save(n);
                });
    }
}
