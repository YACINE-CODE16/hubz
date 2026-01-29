package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.HabitLogRepositoryPort;
import com.hubz.domain.model.HabitLog;
import com.hubz.infrastructure.persistence.mapper.HabitLogMapper;
import com.hubz.infrastructure.persistence.repository.HabitLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HabitLogRepositoryAdapter implements HabitLogRepositoryPort {

    private final HabitLogJpaRepository jpaRepository;
    private final HabitLogMapper mapper;

    @Override
    public HabitLog save(HabitLog habitLog) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(habitLog)));
    }

    @Override
    public Optional<HabitLog> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<HabitLog> findByHabitId(UUID habitId) {
        return jpaRepository.findByHabitId(habitId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<HabitLog> findByHabitIdAndDateRange(UUID habitId, LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findByHabitIdAndDateBetween(habitId, startDate, endDate)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<HabitLog> findByHabitIdAndDate(UUID habitId, LocalDate date) {
        return jpaRepository.findByHabitIdAndDate(habitId, date).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
