package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.NotificationPreferencesRepositoryPort;
import com.hubz.domain.model.NotificationPreferences;
import com.hubz.infrastructure.persistence.mapper.NotificationPreferencesMapper;
import com.hubz.infrastructure.persistence.repository.NotificationPreferencesJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationPreferencesRepositoryAdapter implements NotificationPreferencesRepositoryPort {

    private final NotificationPreferencesJpaRepository jpaRepository;
    private final NotificationPreferencesMapper mapper;

    @Override
    public Optional<NotificationPreferences> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId)
                .map(mapper::toDomain);
    }

    @Override
    public NotificationPreferences save(NotificationPreferences preferences) {
        var entity = mapper.toEntity(preferences);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return jpaRepository.existsByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }
}
