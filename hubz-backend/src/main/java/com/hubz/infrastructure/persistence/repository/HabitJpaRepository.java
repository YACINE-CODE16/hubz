package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.HabitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HabitJpaRepository extends JpaRepository<HabitEntity, UUID> {
    List<HabitEntity> findByUserId(UUID userId);
}
