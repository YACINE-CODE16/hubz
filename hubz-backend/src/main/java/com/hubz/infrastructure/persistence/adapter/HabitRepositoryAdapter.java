package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.HabitRepositoryPort;
import com.hubz.domain.model.Habit;
import com.hubz.infrastructure.persistence.mapper.HabitMapper;
import com.hubz.infrastructure.persistence.repository.HabitJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HabitRepositoryAdapter implements HabitRepositoryPort {

    private final HabitJpaRepository jpaRepository;
    private final HabitMapper mapper;

    @Override
    public Habit save(Habit habit) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(habit)));
    }

    @Override
    public Optional<Habit> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Habit> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
