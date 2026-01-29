package com.hubz.application.service;

import com.hubz.application.dto.request.CreateHabitRequest;
import com.hubz.application.dto.request.LogHabitRequest;
import com.hubz.application.dto.request.UpdateHabitRequest;
import com.hubz.application.dto.response.HabitLogResponse;
import com.hubz.application.dto.response.HabitResponse;
import com.hubz.application.port.out.HabitLogRepositoryPort;
import com.hubz.application.port.out.HabitRepositoryPort;
import com.hubz.domain.exception.HabitNotFoundException;
import com.hubz.domain.model.Habit;
import com.hubz.domain.model.HabitLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HabitService {

    private final HabitRepositoryPort habitRepository;
    private final HabitLogRepositoryPort habitLogRepository;

    @Transactional
    public HabitResponse create(CreateHabitRequest request, UUID userId) {
        Habit habit = Habit.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .icon(request.getIcon())
                .frequency(request.getFrequency())
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(habitRepository.save(habit));
    }

    public List<HabitResponse> getUserHabits(UUID userId) {
        return habitRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HabitResponse update(UUID id, UpdateHabitRequest request, UUID currentUserId) {
        Habit habit = habitRepository.findById(id)
                .orElseThrow(() -> new HabitNotFoundException(id));

        // Check ownership
        if (!habit.getUserId().equals(currentUserId)) {
            throw new HabitNotFoundException(id);
        }

        if (request.getName() != null) habit.setName(request.getName());
        if (request.getIcon() != null) habit.setIcon(request.getIcon());
        if (request.getFrequency() != null) habit.setFrequency(request.getFrequency());
        habit.setUpdatedAt(LocalDateTime.now());

        return toResponse(habitRepository.save(habit));
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId) {
        Habit habit = habitRepository.findById(id)
                .orElseThrow(() -> new HabitNotFoundException(id));

        // Check ownership
        if (!habit.getUserId().equals(currentUserId)) {
            throw new HabitNotFoundException(id);
        }

        habitRepository.deleteById(id);
    }

    @Transactional
    public HabitLogResponse logHabit(UUID habitId, LogHabitRequest request, UUID currentUserId) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new HabitNotFoundException(habitId));

        // Check ownership
        if (!habit.getUserId().equals(currentUserId)) {
            throw new HabitNotFoundException(habitId);
        }

        // Check if log already exists for this date
        var existingLog = habitLogRepository.findByHabitIdAndDate(habitId, request.getDate());

        HabitLog log;
        if (existingLog.isPresent()) {
            // Update existing log
            log = existingLog.get();
            log.setCompleted(request.getCompleted());
            log.setNotes(request.getNotes());
            log.setDuration(request.getDuration());
        } else {
            // Create new log
            log = HabitLog.builder()
                    .id(UUID.randomUUID())
                    .habitId(habitId)
                    .date(request.getDate())
                    .completed(request.getCompleted())
                    .notes(request.getNotes())
                    .duration(request.getDuration())
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        return toLogResponse(habitLogRepository.save(log));
    }

    public List<HabitLogResponse> getHabitLogs(UUID habitId, UUID currentUserId) {
        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new HabitNotFoundException(habitId));

        // Check ownership
        if (!habit.getUserId().equals(currentUserId)) {
            throw new HabitNotFoundException(habitId);
        }

        return habitLogRepository.findByHabitId(habitId).stream()
                .map(this::toLogResponse)
                .toList();
    }

    private HabitResponse toResponse(Habit habit) {
        return HabitResponse.builder()
                .id(habit.getId())
                .name(habit.getName())
                .icon(habit.getIcon())
                .frequency(habit.getFrequency())
                .userId(habit.getUserId())
                .createdAt(habit.getCreatedAt())
                .updatedAt(habit.getUpdatedAt())
                .build();
    }

    private HabitLogResponse toLogResponse(HabitLog log) {
        return HabitLogResponse.builder()
                .id(log.getId())
                .habitId(log.getHabitId())
                .date(log.getDate())
                .completed(log.getCompleted())
                .notes(log.getNotes())
                .duration(log.getDuration())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
