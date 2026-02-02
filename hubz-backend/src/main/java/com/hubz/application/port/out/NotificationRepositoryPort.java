package com.hubz.application.port.out;

import com.hubz.domain.model.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepositoryPort {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    List<Notification> findByUserId(UUID userId);

    List<Notification> findByUserIdAndReadFalse(UUID userId);

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, int limit);

    long countByUserIdAndReadFalse(UUID userId);

    void markAsRead(UUID id);

    void markAllAsReadForUser(UUID userId);

    void deleteById(UUID id);

    void deleteAllByUserId(UUID userId);
}
