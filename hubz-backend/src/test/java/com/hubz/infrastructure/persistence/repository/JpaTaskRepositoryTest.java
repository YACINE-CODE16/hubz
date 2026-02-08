package com.hubz.infrastructure.persistence.repository;

import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.infrastructure.persistence.entity.TaskEntity;
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
@DisplayName("JpaTaskRepository Tests")
class JpaTaskRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaTaskRepository taskRepository;

    private UUID organizationId;
    private UUID creatorId;
    private UUID assigneeId;
    private TaskEntity testTask;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();

        testTask = TaskEntity.builder()
                .title("Test Task")
                .description("A test task description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .organizationId(organizationId)
                .creatorId(creatorId)
                .assigneeId(assigneeId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("findByOrganizationId")
    class FindByOrganizationIdTests {

        @Test
        @DisplayName("Should find all tasks for an organization")
        void shouldFindTasksByOrganizationId() {
            // Given
            entityManager.persistAndFlush(testTask);

            TaskEntity anotherTask = TaskEntity.builder()
                                        .title("Another Task")
                    .status(TaskStatus.IN_PROGRESS)
                    .organizationId(organizationId)
                    .creatorId(creatorId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(anotherTask);

            // When
            List<TaskEntity> tasks = taskRepository.findByOrganizationId(organizationId);

            // Then
            assertThat(tasks).hasSize(2);
            assertThat(tasks).extracting(TaskEntity::getOrganizationId)
                    .containsOnly(organizationId);
        }

        @Test
        @DisplayName("Should return empty list when no tasks in organization")
        void shouldReturnEmptyListWhenNoTasks() {
            // Given
            UUID emptyOrgId = UUID.randomUUID();

            // When
            List<TaskEntity> tasks = taskRepository.findByOrganizationId(emptyOrgId);

            // Then
            assertThat(tasks).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByAssigneeId")
    class FindByAssigneeIdTests {

        @Test
        @DisplayName("Should find all tasks assigned to a user")
        void shouldFindTasksByAssigneeId() {
            // Given
            entityManager.persistAndFlush(testTask);

            TaskEntity anotherTask = TaskEntity.builder()
                                        .title("Another Assigned Task")
                    .status(TaskStatus.TODO)
                    .organizationId(UUID.randomUUID())
                    .creatorId(creatorId)
                    .assigneeId(assigneeId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(anotherTask);

            // When
            List<TaskEntity> tasks = taskRepository.findByAssigneeId(assigneeId);

            // Then
            assertThat(tasks).hasSize(2);
            assertThat(tasks).extracting(TaskEntity::getAssigneeId)
                    .containsOnly(assigneeId);
        }

        @Test
        @DisplayName("Should return empty list when user has no assigned tasks")
        void shouldReturnEmptyListWhenNoAssignedTasks() {
            // Given
            entityManager.persistAndFlush(testTask);

            // When
            List<TaskEntity> tasks = taskRepository.findByAssigneeId(UUID.randomUUID());

            // Then
            assertThat(tasks).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByGoalId")
    class FindByGoalIdTests {

        @Test
        @DisplayName("Should find all tasks linked to a goal")
        void shouldFindTasksByGoalId() {
            // Given
            UUID goalId = UUID.randomUUID();
            testTask.setGoalId(goalId);
            entityManager.persistAndFlush(testTask);

            TaskEntity anotherTask = TaskEntity.builder()
                                        .title("Another Goal Task")
                    .status(TaskStatus.TODO)
                    .organizationId(organizationId)
                    .creatorId(creatorId)
                    .goalId(goalId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(anotherTask);

            // When
            List<TaskEntity> tasks = taskRepository.findByGoalId(goalId);

            // Then
            assertThat(tasks).hasSize(2);
            assertThat(tasks).extracting(TaskEntity::getGoalId)
                    .containsOnly(goalId);
        }

        @Test
        @DisplayName("Should return empty list when goal has no tasks")
        void shouldReturnEmptyListWhenGoalHasNoTasks() {
            // Given
            entityManager.persistAndFlush(testTask);

            // When
            List<TaskEntity> tasks = taskRepository.findByGoalId(UUID.randomUUID());

            // Then
            assertThat(tasks).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchByTitleOrDescription")
    class SearchByTitleOrDescriptionTests {

        @Test
        @DisplayName("Should find tasks matching title query")
        void shouldFindTasksByTitle() {
            // Given
            entityManager.persistAndFlush(testTask);

            // When
            List<TaskEntity> results = taskRepository.searchByTitleOrDescription("Test", List.of(organizationId));

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTitle()).isEqualTo("Test Task");
        }

        @Test
        @DisplayName("Should find tasks matching description query")
        void shouldFindTasksByDescription() {
            // Given
            entityManager.persistAndFlush(testTask);

            // When
            List<TaskEntity> results = taskRepository.searchByTitleOrDescription("test task description", List.of(organizationId));

            // Then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("Should search case-insensitively")
        void shouldSearchCaseInsensitive() {
            // Given
            entityManager.persistAndFlush(testTask);

            // When
            List<TaskEntity> results = taskRepository.searchByTitleOrDescription("TEST", List.of(organizationId));

            // Then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("Should only search within specified organizations")
        void shouldLimitSearchToSpecifiedOrganizations() {
            // Given
            entityManager.persistAndFlush(testTask);

            UUID anotherOrgId = UUID.randomUUID();
            TaskEntity anotherTask = TaskEntity.builder()
                                        .title("Test in Another Org")
                    .status(TaskStatus.TODO)
                    .organizationId(anotherOrgId)
                    .creatorId(creatorId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            entityManager.persistAndFlush(anotherTask);

            // When - Only search in first organization
            List<TaskEntity> results = taskRepository.searchByTitleOrDescription("Test", List.of(organizationId));

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getOrganizationId()).isEqualTo(organizationId);
        }
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("Should save and find task by ID")
        void shouldSaveAndFindById() {
            // Given
            TaskEntity saved = entityManager.persistAndFlush(testTask);

            // When
            Optional<TaskEntity> found = taskRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Test Task");
            assertThat(found.get().getStatus()).isEqualTo(TaskStatus.TODO);
            assertThat(found.get().getPriority()).isEqualTo(TaskPriority.MEDIUM);
        }

        @Test
        @DisplayName("Should update task status")
        void shouldUpdateTaskStatus() {
            // Given
            TaskEntity saved = entityManager.persistAndFlush(testTask);

            // When
            saved.setStatus(TaskStatus.IN_PROGRESS);
            saved.setUpdatedAt(LocalDateTime.now());
            taskRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            // Then
            Optional<TaskEntity> updated = taskRepository.findById(saved.getId());
            assertThat(updated).isPresent();
            assertThat(updated.get().getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Should delete task")
        void shouldDeleteTask() {
            // Given
            TaskEntity saved = entityManager.persistAndFlush(testTask);
            UUID taskId = saved.getId();

            // When
            taskRepository.deleteById(taskId);
            entityManager.flush();

            // Then
            Optional<TaskEntity> deleted = taskRepository.findById(taskId);
            assertThat(deleted).isEmpty();
        }
    }

    @Nested
    @DisplayName("Status and Priority Tests")
    class StatusPriorityTests {

        @Test
        @DisplayName("Should save tasks with all status values")
        void shouldSaveTasksWithAllStatuses() {
            // Given & When
            for (TaskStatus status : TaskStatus.values()) {
                TaskEntity task = TaskEntity.builder()
                                                .title("Task with status " + status)
                        .status(status)
                        .organizationId(organizationId)
                        .creatorId(creatorId)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                entityManager.persistAndFlush(task);
            }

            // Then
            List<TaskEntity> tasks = taskRepository.findByOrganizationId(organizationId);
            assertThat(tasks).hasSize(TaskStatus.values().length);
        }

        @Test
        @DisplayName("Should save tasks with all priority values")
        void shouldSaveTasksWithAllPriorities() {
            // Given & When
            for (TaskPriority priority : TaskPriority.values()) {
                TaskEntity task = TaskEntity.builder()
                                                .title("Task with priority " + priority)
                        .status(TaskStatus.TODO)
                        .priority(priority)
                        .organizationId(organizationId)
                        .creatorId(creatorId)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                entityManager.persistAndFlush(task);
            }

            // Then
            List<TaskEntity> tasks = taskRepository.findByOrganizationId(organizationId);
            assertThat(tasks).hasSize(TaskPriority.values().length);
        }
    }
}
