package com.hubz.application.port.out;

import com.hubz.domain.model.Habit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HabitRepositoryPort {
    Habit save(Habit habit);
    Optional<Habit> findById(UUID id);
    List<Habit> findByUserId(UUID userId);
    void deleteById(UUID id);
}
