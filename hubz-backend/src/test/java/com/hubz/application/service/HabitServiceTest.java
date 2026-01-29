package com.hubz.application.service;

import com.hubz.application.dto.request.CreateHabitRequest;
import com.hubz.application.dto.request.LogHabitRequest;
import com.hubz.application.dto.request.UpdateHabitRequest;
import com.hubz.application.dto.response.HabitLogResponse;
import com.hubz.application.dto.response.HabitResponse;
import com.hubz.application.port.out.HabitLogRepositoryPort;
import com.hubz.application.port.out.HabitRepositoryPort;
import com.hubz.domain.enums.HabitFrequency;
import com.hubz.domain.exception.HabitNotFoundException;
import com.hubz.domain.model.Habit;
import com.hubz.domain.model.HabitLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HabitService Unit Tests")
class HabitServiceTest {

    @Mock
    private HabitRepositoryPort habitRepository;

    @Mock
    private HabitLogRepositoryPort habitLogRepository;

    @InjectMocks
    private HabitService habitService;

    private UUID userId;
    private UUID habitId;
    private Habit testHabit;
    private HabitLog testHabitLog;
    private CreateHabitRequest createRequest;
    private UpdateHabitRequest updateRequest;
    private LogHabitRequest logRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        habitId = UUID.randomUUID();

        testHabit = Habit.builder()
                .id(habitId)
                .name("Morning Exercise")
                .icon("dumbbell")
                .frequency(HabitFrequency.DAILY)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testHabitLog = HabitLog.builder()
                .id(UUID.randomUUID())
                .habitId(habitId)
                .date(LocalDate.now())
                .completed(true)
                .notes("Great workout!")
                .duration(30)
                .createdAt(LocalDateTime.now())
                .build();

        createRequest = CreateHabitRequest.builder()
                .name("Morning Exercise")
                .icon("dumbbell")
                .frequency(HabitFrequency.DAILY)
                .build();

        updateRequest = UpdateHabitRequest.builder()
                .name("Evening Exercise")
                .icon("running")
                .frequency(HabitFrequency.WEEKLY)
                .build();

        logRequest = LogHabitRequest.builder()
                .date(LocalDate.now())
                .completed(true)
                .notes("Great workout!")
                .duration(30)
                .build();
    }

    @Nested
    @DisplayName("Create Habit Tests")
    class CreateTests {

        @Test
        @DisplayName("Should successfully create habit")
        void shouldCreateHabit() {
            // Given
            when(habitRepository.save(any(Habit.class))).thenReturn(testHabit);

            // When
            HabitResponse response = habitService.create(createRequest, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo(testHabit.getName());
            assertThat(response.getIcon()).isEqualTo(testHabit.getIcon());
            assertThat(response.getFrequency()).isEqualTo(testHabit.getFrequency());
            assertThat(response.getUserId()).isEqualTo(userId);

            verify(habitRepository).save(any(Habit.class));
        }

        @Test
        @DisplayName("Should generate UUID for new habit")
        void shouldGenerateUUID() {
            // Given
            ArgumentCaptor<Habit> habitCaptor = ArgumentCaptor.forClass(Habit.class);
            when(habitRepository.save(habitCaptor.capture())).thenReturn(testHabit);

            // When
            habitService.create(createRequest, userId);

            // Then
            Habit savedHabit = habitCaptor.getValue();
            assertThat(savedHabit.getId()).isNotNull();
        }

        @Test
        @DisplayName("Should set timestamps when creating habit")
        void shouldSetTimestamps() {
            // Given
            ArgumentCaptor<Habit> habitCaptor = ArgumentCaptor.forClass(Habit.class);
            when(habitRepository.save(habitCaptor.capture())).thenReturn(testHabit);

            // When
            habitService.create(createRequest, userId);

            // Then
            Habit savedHabit = habitCaptor.getValue();
            assertThat(savedHabit.getCreatedAt()).isNotNull();
            assertThat(savedHabit.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should set all habit properties from request")
        void shouldSetAllPropertiesFromRequest() {
            // Given
            ArgumentCaptor<Habit> habitCaptor = ArgumentCaptor.forClass(Habit.class);
            when(habitRepository.save(habitCaptor.capture())).thenReturn(testHabit);

            // When
            habitService.create(createRequest, userId);

            // Then
            Habit savedHabit = habitCaptor.getValue();
            assertThat(savedHabit.getName()).isEqualTo(createRequest.getName());
            assertThat(savedHabit.getIcon()).isEqualTo(createRequest.getIcon());
            assertThat(savedHabit.getFrequency()).isEqualTo(createRequest.getFrequency());
            assertThat(savedHabit.getUserId()).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("Get User Habits Tests")
    class GetUserHabitsTests {

        @Test
        @DisplayName("Should successfully get user habits")
        void shouldGetUserHabits() {
            // Given
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(testHabit));

            // When
            List<HabitResponse> habits = habitService.getUserHabits(userId);

            // Then
            assertThat(habits).hasSize(1);
            assertThat(habits.get(0).getName()).isEqualTo(testHabit.getName());
            assertThat(habits.get(0).getUserId()).isEqualTo(userId);
            verify(habitRepository).findByUserId(userId);
        }

        @Test
        @DisplayName("Should return empty list when user has no habits")
        void shouldReturnEmptyListWhenNoHabits() {
            // Given
            when(habitRepository.findByUserId(userId)).thenReturn(List.of());

            // When
            List<HabitResponse> habits = habitService.getUserHabits(userId);

            // Then
            assertThat(habits).isEmpty();
        }
    }

    @Nested
    @DisplayName("Update Habit Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should successfully update habit")
        void shouldUpdateHabit() {
            // Given
            when(habitRepository.findById(habitId)).thenReturn(Optional.of(testHabit));
            when(habitRepository.save(any(Habit.class))).thenReturn(testHabit);

            // When
            HabitResponse response = habitService.update(habitId, updateRequest, userId);

            // Then
            assertThat(response).isNotNull();
            verify(habitRepository).findById(habitId);
            verify(habitRepository).save(any(Habit.class));
        }

        @Test
        @DisplayName("Should throw exception when habit not found")
        void shouldThrowExceptionWhenHabitNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(habitRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> habitService.update(nonExistentId, updateRequest, userId))
                    .isInstanceOf(HabitNotFoundException.class);
            verify(habitRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when updating habit by non-owner")
        void shouldThrowExceptionWhenNonOwnerUpdates() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            when(habitRepository.findById(habitId)).thenReturn(Optional.of(testHabit));

            // When & Then
            assertThatThrownBy(() -> habitService.update(habitId, updateRequest, otherUserId))
                    .isInstanceOf(HabitNotFoundException.class);
            verify(habitRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update updatedAt timestamp")
        void shouldUpdateTimestamp() {
            // Given
            when(habitRepository.findById(habitId)).thenReturn(Optional.of(testHabit));
            ArgumentCaptor<Habit> habitCaptor = ArgumentCaptor.forClass(Habit.class);
            when(habitRepository.save(habitCaptor.capture())).thenReturn(testHabit);

            // When
            habitService.update(habitId, updateRequest, userId);

            // Then
            Habit updatedHabit = habitCaptor.getValue();
            assertThat(updatedHabit.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should only update non-null fields")
        void shouldOnlyUpdateNonNullFields() {
            // Given
            UpdateHabitRequest partialUpdate = UpdateHabitRequest.builder()
                    .name("New Name")
                    .build();

            when(habitRepository.findById(habitId)).thenReturn(Optional.of(testHabit));
            ArgumentCaptor<Habit> habitCaptor = ArgumentCaptor.forClass(Habit.class);
            when(habitRepository.save(habitCaptor.capture())).thenReturn(testHabit);

            // When
            habitService.update(habitId, partialUpdate, userId);

            // Then
            Habit updatedHabit = habitCaptor.getValue();
            assertThat(updatedHabit.getName()).isEqualTo("New Name");
            assertThat(updatedHabit.getIcon()).isEqualTo(testHabit.getIcon());
            assertThat(updatedHabit.getFrequency()).isEqualTo(testHabit.getFrequency());
        }
    }

    @Nested
    @DisplayName("Delete Habit Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should successfully delete habit")
        void shouldDeleteHabit() {
            // Given
            when(habitRepository.findById(habitId)).thenReturn(Optional.of(testHabit));
            doNothing().when(habitRepository).deleteById(habitId);

            // When
            habitService.delete(habitId, userId);

            // Then
            verify(habitRepository).findById(habitId);
            verify(habitRepository).deleteById(habitId);
        }

        @Test
        @DisplayName("Should throw exception when habit not found")
        void shouldThrowExceptionWhenHabitNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(habitRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> habitService.delete(nonExistentId, userId))
                    .isInstanceOf(HabitNotFoundException.class);
            verify(habitRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw exception when deleting habit by non-owner")
        void shouldThrowExceptionWhenNonOwnerDeletes() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            when(habitRepository.findById(habitId)).thenReturn(Optional.of(testHabit));

            // When & Then
            assertThatThrownBy(() -> habitService.delete(habitId, otherUserId))
                    .isInstanceOf(HabitNotFoundException.class);
            verify(habitRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Log Habit Tests")
    class LogHabitTests {

        @Test
        @DisplayName("Should successfully log habit for new date")
        void shouldLogHabitForNewDate() {
            // Given
            when(habitRepository.findById(habitId)).thenReturn(Optional.of(testHabit));
            when(habitLogRepository.findByHabitIdAndDate(habitId, logRequest.getDate())).thenReturn(Optional.empty());
            when(habitLogRepository.save(any(HabitLog.class))).thenReturn(testHabitLog);

            // When
            HabitLogResponse response = habitService.logHabit(habitId, logRequest, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getHabitId()).isEqualTo(habitId);
            assertThat(response.getCompleted()).isTrue();
            verify(habitLogRepository).save(any(HabitLog.class));
        }

        @Test
        @DisplayName("Should update existing log for same date")
        void shouldUpdateExistingLog() {
            // Given
            when(habitRepository.findById(habitId)).thenReturn(Optional.of(testHabit));
            when(habitLogRepository.findByHabitIdAndDate(habitId, logRequest.getDate()))
                    .thenReturn(Optional.of(testHabitLog));
            when(habitLogRepository.save(any(HabitLog.class))).thenReturn(testHabitLog);

            // When
            HabitLogResponse response = habitService.logHabit(habitId, logRequest, userId);

            // Then
            assertThat(response).isNotNull();
            verify(habitLogRepository).save(testHabitLog);
        }

        @Test
        @DisplayName("Should throw exception when habit not found")
        void shouldThrowExceptionWhenHabitNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(habitRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> habitService.logHabit(nonExistentId, logRequest, userId))
                    .isInstanceOf(HabitNotFoundException.class);
            verify(habitLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when logging habit by non-owner")
        void shouldThrowExceptionWhenNonOwnerLogs() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            when(habitRepository.findById(habitId)).thenReturn(Optional.of(testHabit));

            // When & Then
            assertThatThrownBy(() -> habitService.logHabit(habitId, logRequest, otherUserId))
                    .isInstanceOf(HabitNotFoundException.class);
            verify(habitLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should generate UUID for new log")
        void shouldGenerateUUIDForNewLog() {
            // Given
            when(habitRepository.findById(habitId)).thenReturn(Optional.of(testHabit));
            when(habitLogRepository.findByHabitIdAndDate(habitId, logRequest.getDate())).thenReturn(Optional.empty());
            ArgumentCaptor<HabitLog> logCaptor = ArgumentCaptor.forClass(HabitLog.class);
            when(habitLogRepository.save(logCaptor.capture())).thenReturn(testHabitLog);

            // When
            habitService.logHabit(habitId, logRequest, userId);

            // Then
            HabitLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getId()).isNotNull();
        }

        @Test
        @DisplayName("Should set createdAt for new log")
        void shouldSetCreatedAtForNewLog() {
            // Given
            when(habitRepository.findById(habitId)).thenReturn(Optional.of(testHabit));
            when(habitLogRepository.findByHabitIdAndDate(habitId, logRequest.getDate())).thenReturn(Optional.empty());
            ArgumentCaptor<HabitLog> logCaptor = ArgumentCaptor.forClass(HabitLog.class);
            when(habitLogRepository.save(logCaptor.capture())).thenReturn(testHabitLog);

            // When
            habitService.logHabit(habitId, logRequest, userId);

            // Then
            HabitLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should set all log properties from request")
        void shouldSetAllLogPropertiesFromRequest() {
            // Given
            when(habitRepository.findById(habitId)).thenReturn(Optional.of(testHabit));
            when(habitLogRepository.findByHabitIdAndDate(habitId, logRequest.getDate())).thenReturn(Optional.empty());
            ArgumentCaptor<HabitLog> logCaptor = ArgumentCaptor.forClass(HabitLog.class);
            when(habitLogRepository.save(logCaptor.capture())).thenReturn(testHabitLog);

            // When
            habitService.logHabit(habitId, logRequest, userId);

            // Then
            HabitLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getHabitId()).isEqualTo(habitId);
            assertThat(savedLog.getDate()).isEqualTo(logRequest.getDate());
            assertThat(savedLog.getCompleted()).isEqualTo(logRequest.getCompleted());
            assertThat(savedLog.getNotes()).isEqualTo(logRequest.getNotes());
            assertThat(savedLog.getDuration()).isEqualTo(logRequest.getDuration());
        }
    }

    @Nested
    @DisplayName("Get Habit Logs Tests")
    class GetHabitLogsTests {

        @Test
        @DisplayName("Should successfully get habit logs")
        void shouldGetHabitLogs() {
            // Given
            when(habitRepository.findById(habitId)).thenReturn(Optional.of(testHabit));
            when(habitLogRepository.findByHabitId(habitId)).thenReturn(List.of(testHabitLog));

            // When
            List<HabitLogResponse> logs = habitService.getHabitLogs(habitId, userId);

            // Then
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getHabitId()).isEqualTo(habitId);
            assertThat(logs.get(0).getCompleted()).isTrue();
            verify(habitLogRepository).findByHabitId(habitId);
        }

        @Test
        @DisplayName("Should return empty list when no logs exist")
        void shouldReturnEmptyListWhenNoLogs() {
            // Given
            when(habitRepository.findById(habitId)).thenReturn(Optional.of(testHabit));
            when(habitLogRepository.findByHabitId(habitId)).thenReturn(List.of());

            // When
            List<HabitLogResponse> logs = habitService.getHabitLogs(habitId, userId);

            // Then
            assertThat(logs).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when habit not found")
        void shouldThrowExceptionWhenHabitNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(habitRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> habitService.getHabitLogs(nonExistentId, userId))
                    .isInstanceOf(HabitNotFoundException.class);
            verify(habitLogRepository, never()).findByHabitId(any());
        }

        @Test
        @DisplayName("Should throw exception when getting logs by non-owner")
        void shouldThrowExceptionWhenNonOwnerGetsLogs() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            when(habitRepository.findById(habitId)).thenReturn(Optional.of(testHabit));

            // When & Then
            assertThatThrownBy(() -> habitService.getHabitLogs(habitId, otherUserId))
                    .isInstanceOf(HabitNotFoundException.class);
            verify(habitLogRepository, never()).findByHabitId(any());
        }
    }

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should correctly map habit to response")
        void shouldCorrectlyMapHabitToResponse() {
            // Given
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(testHabit));

            // When
            List<HabitResponse> habits = habitService.getUserHabits(userId);

            // Then
            HabitResponse response = habits.get(0);
            assertThat(response.getId()).isEqualTo(testHabit.getId());
            assertThat(response.getName()).isEqualTo(testHabit.getName());
            assertThat(response.getIcon()).isEqualTo(testHabit.getIcon());
            assertThat(response.getFrequency()).isEqualTo(testHabit.getFrequency());
            assertThat(response.getUserId()).isEqualTo(testHabit.getUserId());
            assertThat(response.getCreatedAt()).isEqualTo(testHabit.getCreatedAt());
            assertThat(response.getUpdatedAt()).isEqualTo(testHabit.getUpdatedAt());
        }

        @Test
        @DisplayName("Should correctly map habit log to response")
        void shouldCorrectlyMapHabitLogToResponse() {
            // Given
            when(habitRepository.findById(habitId)).thenReturn(Optional.of(testHabit));
            when(habitLogRepository.findByHabitId(habitId)).thenReturn(List.of(testHabitLog));

            // When
            List<HabitLogResponse> logs = habitService.getHabitLogs(habitId, userId);

            // Then
            HabitLogResponse response = logs.get(0);
            assertThat(response.getId()).isEqualTo(testHabitLog.getId());
            assertThat(response.getHabitId()).isEqualTo(testHabitLog.getHabitId());
            assertThat(response.getDate()).isEqualTo(testHabitLog.getDate());
            assertThat(response.getCompleted()).isEqualTo(testHabitLog.getCompleted());
            assertThat(response.getNotes()).isEqualTo(testHabitLog.getNotes());
            assertThat(response.getDuration()).isEqualTo(testHabitLog.getDuration());
            assertThat(response.getCreatedAt()).isEqualTo(testHabitLog.getCreatedAt());
        }
    }
}
