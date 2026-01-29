package com.hubz.application.port.out;

import com.hubz.domain.model.HabitLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HabitLogRepositoryPort {
    HabitLog save(HabitLog habitLog);
    Optional<HabitLog> findById(UUID id);
    List<HabitLog> findByHabitId(UUID habitId);
    List<HabitLog> findByHabitIdAndDateRange(UUID habitId, LocalDate startDate, LocalDate endDate);
    Optional<HabitLog> findByHabitIdAndDate(UUID habitId, LocalDate date);
    void deleteById(UUID id);
}
