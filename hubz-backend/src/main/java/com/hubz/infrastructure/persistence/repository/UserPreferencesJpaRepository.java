package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.UserPreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPreferencesJpaRepository extends JpaRepository<UserPreferencesEntity, UUID> {

    Optional<UserPreferencesEntity> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    List<UserPreferencesEntity> findByDigestEnabledTrue();

    List<UserPreferencesEntity> findByReminderEnabledTrue();
}
