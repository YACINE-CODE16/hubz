package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.HabitLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HabitLogJpaRepository extends JpaRepository<HabitLogEntity, UUID> {
    List<HabitLogEntity> findByHabitId(UUID habitId);
    List<HabitLogEntity> findByHabitIdAndDateBetween(UUID habitId, LocalDate startDate, LocalDate endDate);
    Optional<HabitLogEntity> findByHabitIdAndDate(UUID habitId, LocalDate date);

    // Analytics methods
    List<HabitLogEntity> findByHabitIdInAndDateBetween(List<UUID> habitIds, LocalDate startDate, LocalDate endDate);
    List<HabitLogEntity> findByHabitIdIn(List<UUID> habitIds);
}
