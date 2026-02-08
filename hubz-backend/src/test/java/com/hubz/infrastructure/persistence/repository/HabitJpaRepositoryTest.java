package com.hubz.infrastructure.persistence.repository;

import com.hubz.domain.enums.HabitFrequency;
import com.hubz.infrastructure.persistence.entity.HabitEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("HabitJpaRepository Tests")
class HabitJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HabitJpaRepository habitRepository;

    private UUID userId;
    private HabitEntity testHabit;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        testHabit = HabitEntity.builder()
                .id(UUID.randomUUID())
                .name("Morning Exercise")
                .icon("dumbbell")
                .frequency(HabitFrequency.DAILY)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("findByUserId")
    class FindByUserIdTests {

        @Test
        @DisplayName("Should find all habits for a user")
        void shouldFindHabitsByUserId() {
            // Given
            entityManager.persistAndFlush(testHabit);

            HabitEntity anotherHabit = HabitEntity.builder()
                    .id(UUID.randomUUID())
                    .name("Read Books")
                    .icon("book")
                    .frequency(HabitFrequency.DAILY)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(anotherHabit);

            // When
            List<HabitEntity> habits = habitRepository.findByUserId(userId);

            // Then
            assertThat(habits).hasSize(2);
            assertThat(habits).extracting(HabitEntity::getUserId)
                    .containsOnly(userId);
        }

        @Test
        @DisplayName("Should return empty list when user has no habits")
        void shouldReturnEmptyListWhenNoHabits() {
            // Given
            UUID userWithNoHabits = UUID.randomUUID();

            // When
            List<HabitEntity> habits = habitRepository.findByUserId(userWithNoHabits);

            // Then
            assertThat(habits).isEmpty();
        }

        @Test
        @DisplayName("Should only return habits for the specified user")
        void shouldOnlyReturnHabitsForSpecifiedUser() {
            // Given
            entityManager.persistAndFlush(testHabit);

            UUID anotherUserId = UUID.randomUUID();
            HabitEntity anotherUserHabit = HabitEntity.builder()
                    .id(UUID.randomUUID())
                    .name("Another User Habit")
                    .icon("star")
                    .frequency(HabitFrequency.WEEKLY)
                    .userId(anotherUserId)
                    .createdAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(anotherUserHabit);

            // When
            List<HabitEntity> habits = habitRepository.findByUserId(userId);

            // Then
            assertThat(habits).hasSize(1);
            assertThat(habits.get(0).getName()).isEqualTo("Morning Exercise");
        }
    }

    @Nested
    @DisplayName("Frequency Tests")
    class FrequencyTests {

        @Test
        @DisplayName("Should save habits with all frequency types")
        void shouldSaveHabitsWithAllFrequencies() {
            // Given & When
            for (HabitFrequency frequency : HabitFrequency.values()) {
                HabitEntity habit = HabitEntity.builder()
                        .id(UUID.randomUUID())
                        .name("Habit with frequency " + frequency)
                        .icon("check")
                        .frequency(frequency)
                        .userId(userId)
                        .createdAt(LocalDateTime.now())
                        .build();
                entityManager.persistAndFlush(habit);
            }

            // Then
            List<HabitEntity> habits = habitRepository.findByUserId(userId);
            assertThat(habits).hasSize(HabitFrequency.values().length);
            assertThat(habits).extracting(HabitEntity::getFrequency)
                    .containsExactlyInAnyOrder(HabitFrequency.values());
        }
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("Should save and find habit by ID")
        void shouldSaveAndFindById() {
            // Given
            HabitEntity saved = entityManager.persistAndFlush(testHabit);

            // When
            Optional<HabitEntity> found = habitRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Morning Exercise");
            assertThat(found.get().getIcon()).isEqualTo("dumbbell");
            assertThat(found.get().getFrequency()).isEqualTo(HabitFrequency.DAILY);
        }

        @Test
        @DisplayName("Should update habit")
        void shouldUpdateHabit() {
            // Given
            HabitEntity saved = entityManager.persistAndFlush(testHabit);

            // When
            saved.setName("Updated Habit");
            saved.setIcon("star");
            saved.setFrequency(HabitFrequency.WEEKLY);
            saved.setUpdatedAt(LocalDateTime.now());
            habitRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            // Then
            Optional<HabitEntity> updated = habitRepository.findById(saved.getId());
            assertThat(updated).isPresent();
            assertThat(updated.get().getName()).isEqualTo("Updated Habit");
            assertThat(updated.get().getIcon()).isEqualTo("star");
            assertThat(updated.get().getFrequency()).isEqualTo(HabitFrequency.WEEKLY);
        }

        @Test
        @DisplayName("Should delete habit")
        void shouldDeleteHabit() {
            // Given
            HabitEntity saved = entityManager.persistAndFlush(testHabit);
            UUID habitId = saved.getId();

            // When
            habitRepository.deleteById(habitId);
            entityManager.flush();

            // Then
            Optional<HabitEntity> deleted = habitRepository.findById(habitId);
            assertThat(deleted).isEmpty();
        }

        @Test
        @DisplayName("Should count habits for a user")
        void shouldCountHabitsForUser() {
            // Given
            entityManager.persistAndFlush(testHabit);

            HabitEntity anotherHabit = HabitEntity.builder()
                    .id(UUID.randomUUID())
                    .name("Another Habit")
                    .icon("check")
                    .frequency(HabitFrequency.DAILY)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(anotherHabit);

            // When
            List<HabitEntity> habits = habitRepository.findByUserId(userId);

            // Then
            assertThat(habits).hasSize(2);
        }
    }
}
