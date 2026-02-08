package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.NotificationPreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferencesJpaRepository extends JpaRepository<NotificationPreferencesEntity, UUID> {

    Optional<NotificationPreferencesEntity> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
