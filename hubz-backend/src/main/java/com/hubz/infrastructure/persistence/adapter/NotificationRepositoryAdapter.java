package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.NotificationRepositoryPort;
import com.hubz.domain.model.Notification;
import com.hubz.infrastructure.persistence.mapper.NotificationMapper;
import com.hubz.infrastructure.persistence.repository.NotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository jpaRepository;
    private final NotificationMapper mapper;

    @Override
    public Notification save(Notification notification) {
        var entity = mapper.toEntity(notification);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Notification> findByUserId(UUID userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Notification> findByUserIdAndReadFalse(UUID userId) {
        return jpaRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, int limit) {
        return jpaRepository.findByUserIdWithLimit(userId, PageRequest.of(0, limit)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByUserIdAndReadFalse(UUID userId) {
        return jpaRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID id) {
        jpaRepository.markAsRead(id, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void markAllAsReadForUser(UUID userId) {
        jpaRepository.markAllAsReadForUser(userId, LocalDateTime.now());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteAllByUserId(UUID userId) {
        jpaRepository.deleteAllByUserId(userId);
    }
}
