package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.UserPreferencesRepositoryPort;
import com.hubz.domain.model.UserPreferences;
import com.hubz.infrastructure.persistence.mapper.UserPreferencesMapper;
import com.hubz.infrastructure.persistence.repository.UserPreferencesJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserPreferencesRepositoryAdapter implements UserPreferencesRepositoryPort {

    private final UserPreferencesJpaRepository jpaRepository;
    private final UserPreferencesMapper mapper;

    @Override
    public Optional<UserPreferences> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId)
                .map(mapper::toDomain);
    }

    @Override
    public UserPreferences save(UserPreferences preferences) {
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

    @Override
    public List<UserPreferences> findByDigestEnabledTrue() {
        return jpaRepository.findByDigestEnabledTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<UserPreferences> findByReminderEnabledTrue() {
        return jpaRepository.findByReminderEnabledTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
