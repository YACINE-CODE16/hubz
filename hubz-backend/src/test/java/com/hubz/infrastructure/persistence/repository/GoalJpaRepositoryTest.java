package com.hubz.infrastructure.persistence.repository;

import com.hubz.domain.enums.GoalType;
import com.hubz.infrastructure.persistence.entity.GoalEntity;
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
@DisplayName("GoalJpaRepository Tests")
class GoalJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GoalJpaRepository goalRepository;

    private UUID organizationId;
    private UUID userId;
    private GoalEntity testGoal;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testGoal = GoalEntity.builder()
                .id(UUID.randomUUID())
                .title("Test Goal")
                .description("A test goal description")
                .type(GoalType.SHORT)
                .deadline(LocalDate.now().plusMonths(1))
                .organizationId(organizationId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("findByOrganizationId")
    class FindByOrganizationIdTests {

        @Test
        @DisplayName("Should find all goals for an organization")
        void shouldFindGoalsByOrganizationId() {
            // Given
            entityManager.persistAndFlush(testGoal);

            GoalEntity anotherGoal = GoalEntity.builder()
                    .id(UUID.randomUUID())
                    .title("Another Goal")
                    .type(GoalType.MEDIUM)
                    .organizationId(organizationId)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(anotherGoal);

            // When
            List<GoalEntity> goals = goalRepository.findByOrganizationId(organizationId);

            // Then
            assertThat(goals).hasSize(2);
            assertThat(goals).extracting(GoalEntity::getOrganizationId)
                    .containsOnly(organizationId);
        }

        @Test
        @DisplayName("Should return empty list when no goals in organization")
        void shouldReturnEmptyListWhenNoGoals() {
            // Given
            UUID emptyOrgId = UUID.randomUUID();

            // When
            List<GoalEntity> goals = goalRepository.findByOrganizationId(emptyOrgId);

            // Then
            assertThat(goals).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserIdAndOrganizationIdIsNull")
    class FindPersonalGoalsTests {

        @Test
        @DisplayName("Should find personal goals (organization is null)")
        void shouldFindPersonalGoals() {
            // Given - Create personal goal
            GoalEntity personalGoal = GoalEntity.builder()
                    .id(UUID.randomUUID())
                    .title("Personal Goal")
                    .type(GoalType.SHORT)
                    .organizationId(null)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(personalGoal);

            // Create organization goal
            entityManager.persistAndFlush(testGoal);

            // When
            List<GoalEntity> personalGoals = goalRepository.findByUserIdAndOrganizationIdIsNull(userId);

            // Then
            assertThat(personalGoals).hasSize(1);
            assertThat(personalGoals.get(0).getTitle()).isEqualTo("Personal Goal");
            assertThat(personalGoals.get(0).getOrganizationId()).isNull();
        }

        @Test
        @DisplayName("Should return empty list when user has no personal goals")
        void shouldReturnEmptyListWhenNoPersonalGoals() {
            // Given
            entityManager.persistAndFlush(testGoal); // Organization goal only

            // When
            List<GoalEntity> personalGoals = goalRepository.findByUserIdAndOrganizationIdIsNull(userId);

            // Then
            assertThat(personalGoals).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchByTitle")
    class SearchByTitleTests {

        @Test
        @DisplayName("Should find goals matching title query")
        void shouldFindGoalsByTitle() {
            // Given
            entityManager.persistAndFlush(testGoal);

            // When
            List<GoalEntity> results = goalRepository.searchByTitle("Test", List.of(organizationId), userId);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("Test Goal");
        }

        @Test
        @DisplayName("Should search case-insensitively")
        void shouldSearchCaseInsensitive() {
            // Given
            entityManager.persistAndFlush(testGoal);

            // When
            List<GoalEntity> results = goalRepository.searchByTitle("test", List.of(organizationId), userId);

            // Then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("Should find personal goals in search")
        void shouldFindPersonalGoalsInSearch() {
            // Given
            GoalEntity personalGoal = GoalEntity.builder()
                    .id(UUID.randomUUID())
                    .title("Personal Test Goal")
                    .type(GoalType.SHORT)
                    .organizationId(null)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(personalGoal);

            // When
            List<GoalEntity> results = goalRepository.searchByTitle("Test", List.of(), userId);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("Personal Test Goal");
        }

        @Test
        @DisplayName("Should return empty list when no matches")
        void shouldReturnEmptyListWhenNoMatches() {
            // Given
            entityManager.persistAndFlush(testGoal);

            // When
            List<GoalEntity> results = goalRepository.searchByTitle("Nonexistent", List.of(organizationId), userId);

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Goal Type Tests")
    class GoalTypeTests {

        @Test
        @DisplayName("Should save goals with all goal types")
        void shouldSaveGoalsWithAllTypes() {
            // Given & When
            for (GoalType type : GoalType.values()) {
                GoalEntity goal = GoalEntity.builder()
                        .id(UUID.randomUUID())
                        .title("Goal with type " + type)
                        .type(type)
                        .organizationId(organizationId)
                        .userId(userId)
                        .createdAt(LocalDateTime.now())
                        .build();
                entityManager.persistAndFlush(goal);
            }

            // Then
            List<GoalEntity> goals = goalRepository.findByOrganizationId(organizationId);
            assertThat(goals).hasSize(GoalType.values().length);
            assertThat(goals).extracting(GoalEntity::getType)
                    .containsExactlyInAnyOrder(GoalType.values());
        }
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("Should save and find goal by ID")
        void shouldSaveAndFindById() {
            // Given
            GoalEntity saved = entityManager.persistAndFlush(testGoal);

            // When
            Optional<GoalEntity> found = goalRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Test Goal");
            assertThat(found.get().getType()).isEqualTo(GoalType.SHORT);
        }

        @Test
        @DisplayName("Should update goal")
        void shouldUpdateGoal() {
            // Given
            GoalEntity saved = entityManager.persistAndFlush(testGoal);

            // When
            saved.setTitle("Updated Goal");
            saved.setType(GoalType.LONG);
            saved.setUpdatedAt(LocalDateTime.now());
            goalRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            // Then
            Optional<GoalEntity> updated = goalRepository.findById(saved.getId());
            assertThat(updated).isPresent();
            assertThat(updated.get().getTitle()).isEqualTo("Updated Goal");
            assertThat(updated.get().getType()).isEqualTo(GoalType.LONG);
        }

        @Test
        @DisplayName("Should delete goal")
        void shouldDeleteGoal() {
            // Given
            GoalEntity saved = entityManager.persistAndFlush(testGoal);
            UUID goalId = saved.getId();

            // When
            goalRepository.deleteById(goalId);
            entityManager.flush();

            // Then
            Optional<GoalEntity> deleted = goalRepository.findById(goalId);
            assertThat(deleted).isEmpty();
        }
    }
}
