package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.HabitLogEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("HabitLogJpaRepository Tests")
class HabitLogJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HabitLogJpaRepository habitLogRepository;

    private UUID habitId;
    private HabitLogEntity testLog;

    @BeforeEach
    void setUp() {
        habitId = UUID.randomUUID();

        testLog = HabitLogEntity.builder()
                .id(UUID.randomUUID())
                .habitId(habitId)
                .date(LocalDate.now())
                .completed(true)
                .notes("Completed the habit")
                .duration(30)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("findByHabitId")
    class FindByHabitIdTests {

        @Test
        @DisplayName("Should find all logs for a habit")
        void shouldFindLogsByHabitId() {
            // Given
            entityManager.persistAndFlush(testLog);

            HabitLogEntity anotherLog = HabitLogEntity.builder()
                    .id(UUID.randomUUID())
                    .habitId(habitId)
                    .date(LocalDate.now().minusDays(1))
                    .completed(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(anotherLog);

            // When
            List<HabitLogEntity> logs = habitLogRepository.findByHabitId(habitId);

            // Then
            assertThat(logs).hasSize(2);
            assertThat(logs).extracting(HabitLogEntity::getHabitId)
                    .containsOnly(habitId);
        }

        @Test
        @DisplayName("Should return empty list when habit has no logs")
        void shouldReturnEmptyListWhenNoLogs() {
            // Given
            UUID habitWithNoLogs = UUID.randomUUID();

            // When
            List<HabitLogEntity> logs = habitLogRepository.findByHabitId(habitWithNoLogs);

            // Then
            assertThat(logs).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByHabitIdAndDateBetween")
    class FindByHabitIdAndDateBetweenTests {

        @Test
        @DisplayName("Should find logs within date range")
        void shouldFindLogsInDateRange() {
            // Given
            LocalDate today = LocalDate.now();

            HabitLogEntity log1 = HabitLogEntity.builder()
                    .id(UUID.randomUUID())
                    .habitId(habitId)
                    .date(today.minusDays(2))
                    .completed(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(log1);

            HabitLogEntity log2 = HabitLogEntity.builder()
                    .id(UUID.randomUUID())
                    .habitId(habitId)
                    .date(today)
                    .completed(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(log2);

            HabitLogEntity logOutOfRange = HabitLogEntity.builder()
                    .id(UUID.randomUUID())
                    .habitId(habitId)
                    .date(today.minusDays(10))
                    .completed(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(logOutOfRange);

            // When
            List<HabitLogEntity> logs = habitLogRepository.findByHabitIdAndDateBetween(
                    habitId, today.minusDays(5), today);

            // Then
            assertThat(logs).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when no logs in range")
        void shouldReturnEmptyListWhenNoLogsInRange() {
            // Given
            entityManager.persistAndFlush(testLog);
            LocalDate today = LocalDate.now();

            // When
            List<HabitLogEntity> logs = habitLogRepository.findByHabitIdAndDateBetween(
                    habitId, today.plusDays(10), today.plusDays(20));

            // Then
            assertThat(logs).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByHabitIdAndDate")
    class FindByHabitIdAndDateTests {

        @Test
        @DisplayName("Should find log for specific date")
        void shouldFindLogForSpecificDate() {
            // Given
            entityManager.persistAndFlush(testLog);

            // When
            Optional<HabitLogEntity> found = habitLogRepository.findByHabitIdAndDate(habitId, LocalDate.now());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getCompleted()).isTrue();
        }

        @Test
        @DisplayName("Should return empty when no log for date")
        void shouldReturnEmptyWhenNoLogForDate() {
            // Given
            entityManager.persistAndFlush(testLog);

            // When
            Optional<HabitLogEntity> found = habitLogRepository.findByHabitIdAndDate(
                    habitId, LocalDate.now().plusDays(5));

            // Then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByHabitIdInAndDateBetween")
    class FindByHabitIdInAndDateBetweenTests {

        @Test
        @DisplayName("Should find logs for multiple habits within date range")
        void shouldFindLogsForMultipleHabits() {
            // Given
            LocalDate today = LocalDate.now();
            UUID anotherHabitId = UUID.randomUUID();

            entityManager.persistAndFlush(testLog);

            HabitLogEntity anotherHabitLog = HabitLogEntity.builder()
                    .id(UUID.randomUUID())
                    .habitId(anotherHabitId)
                    .date(today)
                    .completed(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(anotherHabitLog);

            // When
            List<HabitLogEntity> logs = habitLogRepository.findByHabitIdInAndDateBetween(
                    List.of(habitId, anotherHabitId), today.minusDays(1), today);

            // Then
            assertThat(logs).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findByHabitIdIn")
    class FindByHabitIdInTests {

        @Test
        @DisplayName("Should find all logs for multiple habits")
        void shouldFindLogsForMultipleHabits() {
            // Given
            UUID anotherHabitId = UUID.randomUUID();

            entityManager.persistAndFlush(testLog);

            HabitLogEntity anotherHabitLog = HabitLogEntity.builder()
                    .id(UUID.randomUUID())
                    .habitId(anotherHabitId)
                    .date(LocalDate.now().minusDays(1))
                    .completed(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(anotherHabitLog);

            // When
            List<HabitLogEntity> logs = habitLogRepository.findByHabitIdIn(
                    List.of(habitId, anotherHabitId));

            // Then
            assertThat(logs).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when habits have no logs")
        void shouldReturnEmptyListWhenNoLogs() {
            // Given
            List<UUID> habitsWithNoLogs = List.of(UUID.randomUUID(), UUID.randomUUID());

            // When
            List<HabitLogEntity> logs = habitLogRepository.findByHabitIdIn(habitsWithNoLogs);

            // Then
            assertThat(logs).isEmpty();
        }
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("Should save and find log by ID")
        void shouldSaveAndFindById() {
            // Given
            HabitLogEntity saved = entityManager.persistAndFlush(testLog);

            // When
            Optional<HabitLogEntity> found = habitLogRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getHabitId()).isEqualTo(habitId);
            assertThat(found.get().getDate()).isEqualTo(LocalDate.now());
            assertThat(found.get().getCompleted()).isTrue();
            assertThat(found.get().getNotes()).isEqualTo("Completed the habit");
            assertThat(found.get().getDuration()).isEqualTo(30);
        }

        @Test
        @DisplayName("Should update log")
        void shouldUpdateLog() {
            // Given
            HabitLogEntity saved = entityManager.persistAndFlush(testLog);

            // When
            saved.setCompleted(false);
            saved.setNotes("Skipped today");
            habitLogRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            // Then
            Optional<HabitLogEntity> updated = habitLogRepository.findById(saved.getId());
            assertThat(updated).isPresent();
            assertThat(updated.get().getCompleted()).isFalse();
            assertThat(updated.get().getNotes()).isEqualTo("Skipped today");
        }

        @Test
        @DisplayName("Should delete log")
        void shouldDeleteLog() {
            // Given
            HabitLogEntity saved = entityManager.persistAndFlush(testLog);
            UUID logId = saved.getId();

            // When
            habitLogRepository.deleteById(logId);
            entityManager.flush();

            // Then
            Optional<HabitLogEntity> deleted = habitLogRepository.findById(logId);
            assertThat(deleted).isEmpty();
        }
    }
}
